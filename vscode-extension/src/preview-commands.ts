/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as vscode from 'vscode';
import {promises as fs} from 'fs';
import * as path from 'path';
import {detectProjectLayout, ProjectLayout, MixedProjectLayout} from './project-layout';
import {PreviewClient, PreviewEvent} from './preview-client';

function asLayout(value: ProjectLayout | MixedProjectLayout | undefined): ProjectLayout | undefined {
    return value && value.buildTool !== 'mixed' ? value : undefined;
}

export class PreviewManager {
    private client?: PreviewClient;
    private watcher?: vscode.FileSystemWatcher;
    private reloadTimer?: NodeJS.Timeout;
    private frameTimer?: NodeJS.Timeout;
    private panel?: vscode.WebviewPanel;
    private controlFile?: string;
    public constructor(private readonly output = vscode.window.createOutputChannel('TotalCross Preview')) {}

    public async start(): Promise<void> {
        const folders = vscode.workspace.workspaceFolders || [];
        const folder = folders.length <= 1 ? folders[0] : await vscode.window.showWorkspaceFolderPick({ placeHolder: 'Select the TotalCross project to preview' });
        if (!folder) throw new Error('TotalCross project not found in this VS Code instance.');
        const layout = asLayout(await detectProjectLayout(folder.uri.fsPath, process.platform));
        if (!layout) throw new Error('Unsupported or mixed TotalCross project.');
        this.disposePanel();
        this.panel = vscode.window.createWebviewPanel('totalcrossPreview', 'TotalCross Preview', vscode.ViewColumn.Beside,
            {enableScripts: true, retainContextWhenHidden: true});
        this.panel.webview.html = previewHtml(this.panel.webview);
        this.panel.webview.onDidReceiveMessage((message) => this.sendControl(layout.root, layout.buildTool, message));
        this.panel.onDidDispose(() => { this.panel = undefined; this.stopFramePolling(); });
        const previewRoot = layout.buildTool === 'gradle' ? layout.packageOutputRoot : path.join(layout.packageOutputRoot, 'totalcross');
        this.controlFile = path.join(previewRoot, 'preview-control.txt');
        this.client = new PreviewClient(layout);
        this.client.onEvent((event) => this.show(event));
        await this.client.start();
        this.startFramePolling(previewRoot);
        this.watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, '**/*.{java,xml,properties,gradle,gradle.kts,pom.xml}'));
        this.watcher.onDidChange(() => this.scheduleReload());
        this.watcher.onDidCreate(() => this.scheduleReload());
        this.watcher.onDidDelete(() => this.scheduleReload());
    }

    public async run(): Promise<void> { await this.start(); }
    public stop(): void { if (this.client) this.client.stop(); if (this.watcher) this.watcher.dispose(); this.stopFramePolling(); this.disposePanel(); }
    public showDiagnostics(): void { this.output.show(true); }

    private scheduleReload(): void {
        if (this.reloadTimer) clearTimeout(this.reloadTimer);
        this.reloadTimer = setTimeout(() => this.reloadAfterBuild().catch((error) => this.show({kind: 'error', message: error.message})), 250);
    }

    private async reloadAfterBuild(): Promise<void> {
        if (!this.client) return;
        await this.client.reload();
        const mainClass = await this.client.mainClass();
        await this.sendControl(this.client.root(), this.client.buildTool(), {command: 'reload', values: [mainClass]});
        this.show({kind: 'reload-requested', message: mainClass});
    }

    private show(event: PreviewEvent): void {
        this.output.appendLine(`[${event.kind}] ${event.message || ''}`);
        if (event.kind === 'error') vscode.window.showErrorMessage(`TotalCross preview: ${event.message || 'unknown error'}`);
    }

    private startFramePolling(outputRoot: string): void {
        const frame = path.join(outputRoot, 'preview-frame.png');
        const interval = Math.max(100, vscode.workspace.getConfiguration('totalcross.livePreview').get<number>('framePollInterval', 500));
        this.frameTimer = setInterval(async () => {
            if (!this.panel) return;
            try {
                const data = (await fs.readFile(frame)).toString('base64');
                this.panel.webview.postMessage({type: 'frame', data: `data:image/png;base64,${data}`});
            } catch (_) { /* The coordinator has not produced its first frame yet. */ }
        }, interval);
    }

    private stopFramePolling(): void { if (this.frameTimer) clearInterval(this.frameTimer); this.frameTimer = undefined; }

    private async sendControl(root: string, buildTool: string, message: any): Promise<void> {
        if (!message || typeof message.command !== 'string') return;
        const outputRoot = buildTool === 'gradle' ? path.join(root, 'build', 'totalcross') : path.join(root, 'target', 'totalcross');
        const file = path.join(outputRoot, 'preview-control.txt');
        const values = Array.isArray(message.values) ? message.values.map((value: unknown) => String(value)) : [];
        await fs.mkdir(outputRoot, {recursive: true});
        await fs.appendFile(file, `${message.command},${values.join(',')}\n`);
    }

    private disposePanel(): void { if (this.panel) this.panel.dispose(); this.panel = undefined; }
}

function previewHtml(webview: vscode.Webview): string {
    const nonce = Math.random().toString(36).slice(2);
    return `<!doctype html><html><body style="margin:0;overflow:auto;background:#1e1e1e">
<img id="frame" alt="TotalCross preview" tabindex="0" style="display:block;max-width:100%;image-rendering:auto">
<script nonce="${nonce}">
const vscode = acquireVsCodeApi(); const frame = document.getElementById('frame');
window.addEventListener('message', event => { if (event.data.type === 'frame') frame.src = event.data.data; });
frame.addEventListener('pointerdown', event => vscode.postMessage({command:'pointer', values:[Math.round(event.offsetX),Math.round(event.offsetY),event.button,true]}));
frame.addEventListener('pointerup', event => vscode.postMessage({command:'pointer', values:[Math.round(event.offsetX),Math.round(event.offsetY),event.button,false]}));
frame.addEventListener('keydown', event => vscode.postMessage({command:'key', values:[event.keyCode,true,event.getModifierState('Shift') ? 1 : 0]}));
frame.addEventListener('keyup', event => vscode.postMessage({command:'key', values:[event.keyCode,false,event.getModifierState('Shift') ? 1 : 0]}));
</script></body></html>`;
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
