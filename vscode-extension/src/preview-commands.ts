/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as vscode from 'vscode';
import {detectProjectLayout, ProjectLayout, MixedProjectLayout} from './project-layout';
import {PreviewClient, PreviewEvent} from './preview-client';

function asLayout(value: ProjectLayout | MixedProjectLayout | undefined): ProjectLayout | undefined {
    return value && value.buildTool !== 'mixed' ? value : undefined;
}

export class PreviewManager {
    private client?: PreviewClient;
    private watcher?: vscode.FileSystemWatcher;
    private reloadTimer?: NodeJS.Timeout;
    public constructor(private readonly output = vscode.window.createOutputChannel('TotalCross Preview')) {}

    public async start(): Promise<void> {
        const folders = vscode.workspace.workspaceFolders || [];
        const folder = folders.length <= 1 ? folders[0] : await vscode.window.showWorkspaceFolderPick({ placeHolder: 'Select the TotalCross project to preview' });
        if (!folder) throw new Error('TotalCross project not found in this VS Code instance.');
        const layout = asLayout(await detectProjectLayout(folder.uri.fsPath, process.platform));
        if (!layout) throw new Error('Unsupported or mixed TotalCross project.');
        this.client = new PreviewClient(layout);
        this.client.onEvent((event) => this.show(event));
        await this.client.start();
        this.watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, '**/*.{java,xml,properties,gradle,gradle.kts,pom.xml}'));
        this.watcher.onDidChange(() => this.scheduleReload());
        this.watcher.onDidCreate(() => this.scheduleReload());
        this.watcher.onDidDelete(() => this.scheduleReload());
    }

    public async run(): Promise<void> { await this.start(); }
    public stop(): void { if (this.client) this.client.stop(); if (this.watcher) this.watcher.dispose(); }
    public showDiagnostics(): void { this.output.show(true); }

    private scheduleReload(): void {
        if (this.reloadTimer) clearTimeout(this.reloadTimer);
        this.reloadTimer = setTimeout(() => this.client!.reload().catch((error) => this.show({kind: 'error', message: error.message})), 250);
    }

    private show(event: PreviewEvent): void {
        this.output.appendLine(`[${event.kind}] ${event.message || ''}`);
        if (event.kind === 'error') vscode.window.showErrorMessage(`TotalCross preview: ${event.message || 'unknown error'}`);
    }
}

export function registerPreviewCommands(context: vscode.ExtensionContext): PreviewManager {
    const manager = new PreviewManager();
    context.subscriptions.push(vscode.commands.registerCommand('extension.preview', () => manager.start()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.run', () => manager.run()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewStop', () => manager.stop()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewDiagnostics', () => manager.showDiagnostics()));
    context.subscriptions.push({dispose: () => manager.stop()});
    return manager;
}
