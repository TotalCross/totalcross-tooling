/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
import {promises as fs} from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import {PreviewManager} from '../../preview-commands';
import {PreviewClient, PreviewCommand, mavenExecutable, previewCommand} from '../../preview-client';
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
