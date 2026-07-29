/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
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
        assert.deepStrictEqual(commands, [{executable: './gradlew', args: ['classes', '--console=plain']}]);
        assert.deepStrictEqual(events, ['build-succeeded']);
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
