/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
import {ChildProcess} from 'child_process';
import {EventEmitter} from 'events';
import {promises as fs} from 'fs';
import {PassThrough} from 'stream';
import * as path from 'path';
import * as vscode from 'vscode';
import {PreviewManager} from '../../preview-commands';
import {PreviewClient, PreviewCommand, mavenExecutable, previewCommand, previewEnvironment} from '../../preview-client';
import {ProjectLayout} from '../../project-layout';

suite('Preview client', () => {
    const layout: ProjectLayout = {buildTool: 'gradle', root: '/tmp/app', packageCommand: '', packageOutputRoot: '', linuxArmInstallDirectory: ''};

    test('uses wrapper without a shell', () => {
        const command = previewCommand(layout, 'darwin');
        assert.strictEqual(command.executable, './gradlew');
        assert.deepStrictEqual(command.args, ['totalcrossPreview', '--console=plain']);
    });

    test('compiles Maven projects before opening preview', () => {
        const command = previewCommand({...layout, buildTool: 'maven'}, 'darwin');
        assert.strictEqual(command.executable, 'mvn');
        assert.deepStrictEqual(command.args, ['compile', 'totalcross:preview']);
    });

    test('prefers Maven Wrapper when it is present', () => {
        assert.strictEqual(mavenExecutable('/project', 'darwin', () => true), './mvnw');
        assert.strictEqual(mavenExecutable('/project', 'win32', () => true), '.\\mvnw.cmd');
        assert.strictEqual(mavenExecutable('/project', 'darwin', () => false), 'mvn');
    });

    test('forces the whole preview process tree into headless mode', () => {
        const original = {JAVA_TOOL_OPTIONS: '-Xmx512m', CUSTOM_OPTION: 'preserved'};
        const environment = previewEnvironment(original, 'darwin');

        assert.deepStrictEqual(environment, {
            JAVA_TOOL_OPTIONS: '-Xmx512m -Djava.awt.headless=true -Dapple.awt.UIElement=true',
            CUSTOM_OPTION: 'preserved'
        });
        assert.deepStrictEqual(original, {JAVA_TOOL_OPTIONS: '-Xmx512m', CUSTOM_OPTION: 'preserved'});
        assert.strictEqual(
            previewEnvironment({}, 'linux').JAVA_TOOL_OPTIONS,
            '-Djava.awt.headless=true'
        );
    });

    test('passes the headless environment to the preview launcher', async () => {
        const launch = fakeChildProcess();
        let options: import('child_process').SpawnOptions | undefined;
        const client = new PreviewClient(layout, 'darwin', (_executable, _args, spawnOptions) => {
            options = spawnOptions;
            return launch.child;
        });

        await client.start();

        assert.strictEqual(options?.env?.JAVA_TOOL_OPTIONS?.endsWith(
            '-Djava.awt.headless=true -Dapple.awt.UIElement=true'
        ), true);
        launch.events.emit('close', 0);
    });

    test('reports build success only after the reload build completes', async () => {
        const client = new PreviewClient(layout, 'darwin');
        const events: string[] = [];
        const commands: PreviewCommand[] = [];
        client.onEvent((event) => events.push(event.kind));
        (client as unknown as {run(command: PreviewCommand): Promise<void>}).run = async (command) => { commands.push(command); };
        await client.reload();
        assert.deepStrictEqual(commands, [{executable: './gradlew', args: ['totalcrossProjectModel', '--console=plain']}]);
        assert.deepStrictEqual(events, ['build-succeeded']);
    });

    test('waits for an in-flight preview launch before running the stop task', async () => {
        const launch = fakeChildProcess();
        const stop = fakeChildProcess();
        const commands: PreviewCommand[] = [];
        const client = new PreviewClient(layout, 'darwin', (executable, args) => {
            commands.push({executable, args: [...args]});
            return commands.length === 1 ? launch.child : stop.child;
        });

        await client.start();
        const stopping = client.stop();
        await Promise.resolve();
        assert.strictEqual(commands.length, 1);

        launch.events.emit('close', 0);
        await until(() => commands.length === 2);
        assert.deepStrictEqual(commands[1], {executable: './gradlew', args: ['totalcrossPreviewStop', '--console=plain']});
        stop.events.emit('close', 0);
        await stopping;
    });

    test('manager stop waits for the owned preview client', async () => {
        const output = {appendLine: () => undefined, show: () => undefined} as unknown as vscode.OutputChannel;
        const manager = new PreviewManager(output) as unknown as {
            client?: {stop(): Promise<void>};
            stop(): Promise<void>;
        };
        let release: (() => void) | undefined;
        manager.client = {stop: () => new Promise<void>((resolve) => { release = resolve; })};
        let firstFinished = false;
        let secondFinished = false;
        const firstStop = manager.stop().then(() => { firstFinished = true; });
        const secondStop = manager.stop().then(() => { secondFinished = true; });

        await Promise.resolve();
        assert.strictEqual(firstFinished, false);
        assert.strictEqual(secondFinished, false);
        release!();
        await Promise.all([firstStop, secondStop]);
        assert.strictEqual(firstFinished, true);
        assert.strictEqual(secondFinished, true);
    });

    test('reads a generated Gradle project model', async () => {
        const root = await fs.mkdtemp('/tmp/totalcross-preview-model-');
        try {
            const modelRoot = path.join(root, 'build', 'totalcross');
            await fs.mkdir(modelRoot, {recursive: true});
            await fs.writeFile(path.join(modelRoot, 'project-model.json'), '{"mainClass":"example.App"}');
            const client = new PreviewClient({...layout, root, packageOutputRoot: modelRoot}, 'darwin');
            assert.strictEqual(await client.mainClass(), 'example.App');
        } finally {
            await fs.rm(root, {recursive: true, force: true});
        }
    });

    test('reports the model generation task when the model is missing', async () => {
        const root = await fs.mkdtemp('/tmp/totalcross-preview-missing-model-');
        try {
            const modelRoot = path.join(root, 'build', 'totalcross');
            const client = new PreviewClient({...layout, root, packageOutputRoot: modelRoot}, 'darwin');
            await assert.rejects(client.mainClass(), /totalcrossProjectModel.*did not complete successfully or was not executed/);
        } finally {
            await fs.rm(root, {recursive: true, force: true});
        }
    });

    test('reload waits for Gradle model generation before reading mainClass', async () => {
        const output = {appendLine: () => undefined, show: () => undefined} as unknown as vscode.OutputChannel;
        const manager = new PreviewManager(output) as unknown as {
            client?: {reload(): Promise<void>; mainClass(): Promise<string>; root(): string; buildTool(): 'gradle'};
            reloadAfterBuild(): Promise<void>;
            sendControl(root: string, buildTool: string, message: unknown): Promise<void>;
        };
        const order: string[] = [];
        manager.client = {
            reload: async () => { order.push('totalcrossProjectModel'); },
            mainClass: async () => { order.push('mainClass'); return 'example.App'; },
            root: () => '/tmp/app', buildTool: () => 'gradle'
        };
        manager.sendControl = async () => { order.push('control'); };
        await manager.reloadAfterBuild();
        assert.deepStrictEqual(order, ['totalcrossProjectModel', 'mainClass', 'control']);
    });

    test('does not request a candidate reload after a failed build', async () => {
        const output = {appendLine: () => undefined, show: () => undefined} as unknown as vscode.OutputChannel;
        const manager = new PreviewManager(output) as unknown as {
            client?: {reload(): Promise<void>; mainClass(): Promise<string>; root(): string; buildTool(): 'gradle'};
            reloadAfterBuild(): Promise<void>;
            sendControl(root: string, buildTool: string, message: unknown): Promise<void>;
        };
        let requested = false;
        manager.client = {
            reload: async () => { throw new Error('compile failed'); },
            mainClass: async () => 'example.Main', root: () => '/tmp/app', buildTool: () => 'gradle'
        };
        manager.sendControl = async () => { requested = true; };
        await assert.rejects(manager.reloadAfterBuild(), /compile failed/);
        assert.strictEqual(requested, false);
    });
});

function fakeChildProcess(): {child: ChildProcess; events: EventEmitter} {
    const events = new EventEmitter();
    const child = events as unknown as ChildProcess & {stdout: PassThrough; stderr: PassThrough; killed: boolean};
    child.stdout = new PassThrough();
    child.stderr = new PassThrough();
    child.killed = false;
    child.kill = () => { child.killed = true; return true; };
    return {child, events};
}

async function until(condition: () => boolean): Promise<void> {
    for (let attempt = 0; attempt < 20 && !condition(); attempt++) await new Promise((resolve) => setImmediate(resolve));
    assert.ok(condition(), 'condition was not met');
}
