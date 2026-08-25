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
    private lifecycle: Promise<void> = Promise.resolve();
    public constructor(private readonly output = vscode.window.createOutputChannel('TotalCross Preview')) {}

    public start(): Promise<void> { return this.enqueue(() => this.startPreview()); }

    private async startPreview(): Promise<void> {
        const folders = vscode.workspace.workspaceFolders || [];
        const folder = folders.length <= 1 ? folders[0] : await vscode.window.showWorkspaceFolderPick({ placeHolder: 'Select the TotalCross project to preview' });
        if (!folder) throw new Error('TotalCross project not found in this VS Code instance.');
        const layout = asLayout(await detectProjectLayout(folder.uri.fsPath, process.platform));
        if (!layout) throw new Error('Unsupported or mixed TotalCross project.');
        await this.stopPreview();
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
        await this.applyDeviceProfile(layout.root, layout.buildTool);
        this.startFramePolling(previewRoot);
        this.watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, '**/*.{java,xml,properties,gradle,gradle.kts,pom.xml}'));
        this.watcher.onDidChange(() => this.scheduleReload());
        this.watcher.onDidCreate(() => this.scheduleReload());
        this.watcher.onDidDelete(() => this.scheduleReload());
    }

    public async run(): Promise<void> { await this.start(); }
    public stop(): Promise<void> { return this.enqueue(() => this.stopPreview()); }

    private async stopPreview(): Promise<void> {
        const client = this.client;
        this.client = undefined;
        if (this.reloadTimer) clearTimeout(this.reloadTimer);
        this.reloadTimer = undefined;
        if (this.watcher) this.watcher.dispose();
        this.watcher = undefined;
        this.stopFramePolling();
        this.disposePanel();
        if (client) await client.stop();
    }
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

    private async applyDeviceProfile(root: string, buildTool: string): Promise<void> {
        const configuration = vscode.workspace.getConfiguration('totalcross.livePreview');
        const orientation = configuration.get<string>('orientation', 'portrait');
        const density = configuration.get<number>('density', 1);
        let width = configuration.get<number>('width', 360);
        let height = configuration.get<number>('height', 592);
        if (orientation === 'landscape' && width < height) [width, height] = [height, width];
        if (orientation === 'portrait' && width > height) [width, height] = [height, width];
        await this.sendControl(root, buildTool, {command: 'resize', values: [width, height, density]});
        await this.panel?.webview.postMessage({type: 'device', width, height, density, orientation});
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

    private enqueue(operation: () => Promise<void>): Promise<void> {
        const result = this.lifecycle.then(operation, operation);
        this.lifecycle = result.catch(() => undefined);
        return result;
    }

    private disposePanel(): void { if (this.panel) this.panel.dispose(); this.panel = undefined; }
}

function previewHtml(webview: vscode.Webview): string {
    const nonce = Math.random().toString(36).slice(2);
    return `<!doctype html><html><body style="margin:0;overflow:auto;background:#1e1e1e">
<img id="frame" alt="TotalCross preview" tabindex="0" style="display:block;max-width:100%;image-rendering:auto">
<script nonce="${nonce}">
const vscode = acquireVsCodeApi(); const frame = document.getElementById('frame');
let device = {width:360,height:592,density:1,orientation:'portrait'};
function intrinsicPointer(event) { const rect = frame.getBoundingClientRect(); return [Math.round((event.clientX - rect.left) * frame.naturalWidth / rect.width), Math.round((event.clientY - rect.top) * frame.naturalHeight / rect.height)]; }
function sendResize() { vscode.postMessage({command:'resize', values:[device.width,device.height,device.density]}); }
window.addEventListener('message', event => { if (event.data.type === 'frame') frame.src = event.data.data; if (event.data.type === 'device') { device = event.data; frame.style.width = device.width + 'px'; frame.style.height = device.height + 'px'; sendResize(); } });
new ResizeObserver(sendResize).observe(frame);
frame.addEventListener('pointerdown', event => { const point = intrinsicPointer(event); vscode.postMessage({command:'pointer', values:[point[0],point[1],event.button,true]}); });
frame.addEventListener('pointerup', event => { const point = intrinsicPointer(event); vscode.postMessage({command:'pointer', values:[point[0],point[1],event.button,false]}); });
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
    context.subscriptions.push({dispose: () => { void manager.stop().catch((error) => console.error('Unable to stop TotalCross preview:', error)); }});
    return manager;
}
