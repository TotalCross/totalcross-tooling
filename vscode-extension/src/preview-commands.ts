/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as vscode from 'vscode';
import {promises as fs} from 'fs';
import * as path from 'path';
import {detectProjectLayout, ProjectLayout, MixedProjectLayout} from './project-layout';
import {PreviewClient, PreviewEvent} from './preview-client';
import {compiledClassExists, isJavaFile, isWorkspaceFile, parseSelection, sourceClassName} from './preview-editor';
import {configPath, discoverMainWindowCandidates, PreviewConfiguration, readPreviewConfiguration, writePreviewConfiguration} from './preview-config';

function asLayout(value: ProjectLayout | MixedProjectLayout | undefined): ProjectLayout | undefined {
    return value && value.buildTool !== 'mixed' ? value : undefined;
}

export class PreviewManager {
    private client?: PreviewClient;
    private watcher?: vscode.FileSystemWatcher;
    private reloadTimer?: NodeJS.Timeout;
    private frameTimer?: NodeJS.Timeout;
    private selectionTimer?: NodeJS.Timeout;
    private editorTimer?: NodeJS.Timeout;
    private editorListener?: vscode.Disposable;
    private panel?: vscode.WebviewPanel;
    private controlFile?: string;
    private lifecycle: Promise<void> = Promise.resolve();
    private stopCompletion?: Promise<void>;
    private pendingClass?: string;
    private revivalClass?: string;
    private editorGeneration = 0;
    private pollingGeneration = 0;
    private pollingInFlight = false;
    public constructor(private readonly output = vscode.window.createOutputChannel('TotalCross Preview')) {}

    public start(): Promise<void> { return this.enqueue(() => this.startPreview()); }

    private async startPreview(existingPanel?: vscode.WebviewPanel, selectedFolder?: vscode.WorkspaceFolder): Promise<void> {
        const folders = vscode.workspace.workspaceFolders || [];
        const folder = selectedFolder || (folders.length <= 1 ? folders[0] : await vscode.window.showWorkspaceFolderPick({ placeHolder: 'Select the TotalCross project to preview' }));
        if (!folder) throw new Error('TotalCross project not found in this VS Code instance.');
        const layout = asLayout(await detectProjectLayout(folder.uri.fsPath, process.platform));
        if (!layout) throw new Error('Unsupported or mixed TotalCross project.');
        await this.ensurePreviewConfiguration(layout.root);
        await this.stopPreview();
        this.panel = existingPanel || vscode.window.createWebviewPanel('totalcrossPreview', 'TotalCross Preview', vscode.ViewColumn.Beside,
            {enableScripts: true, retainContextWhenHidden: true});
        this.panel.webview.html = previewHtml(this.panel.webview);
        this.panel.webview.onDidReceiveMessage((message) => this.sendControl(layout.root, layout.buildTool, message));
        const panel = this.panel;
        panel.onDidDispose(() => {
            if (this.panel !== panel) return;
            this.panel = undefined;
            void this.requestStop(false).catch((error) => this.show({kind: 'error', message: error.message}));
        });
        const previewRoot = layout.buildTool === 'gradle' ? layout.packageOutputRoot : path.join(layout.packageOutputRoot, 'totalcross');
        this.controlFile = path.join(previewRoot, 'preview-control.txt');
        const jvmArgs = vscode.workspace.getConfiguration('totalcross.livePreview').get<string[]>('jvmArgs', []);
        this.client = new PreviewClient(layout, process.platform, undefined, jvmArgs);
        this.client.onEvent((event) => this.show(event));
        await this.client.start();
        await this.client.ready();
        await this.client.loadSelectionCapability();
        await this.applyDeviceProfile(layout.root, layout.buildTool);
        this.startFramePolling(previewRoot);
        this.editorListener = vscode.window.onDidChangeActiveTextEditor(() => this.scheduleActiveEditorPreview());
        this.watcher = vscode.workspace.createFileSystemWatcher(new vscode.RelativePattern(folder, '**/*.{java,xml,properties,gradle,gradle.kts,pom.xml,totalcross-preview.json}'));
        this.watcher.onDidChange(() => this.scheduleReload());
        this.watcher.onDidCreate(() => this.scheduleReload());
        this.watcher.onDidDelete(() => this.scheduleReload());
        if (this.revivalClass) {
            const className = this.revivalClass;
            this.revivalClass = undefined;
            const model = await this.readProjectModel(this.client);
            await this.presentClassName(className, model.classOutput);
        } else {
            this.scheduleActiveEditorPreview();
        }
    }

    public async run(): Promise<void> { await this.start(); }
    public stop(): Promise<void> { return this.requestStop(true); }
    public reload(): Promise<void> { return this.enqueue(async () => this.client ? this.reloadAfterBuild() : this.startPreview()); }
    public revive(panel: vscode.WebviewPanel, state: unknown): Promise<void> {
        const workspace = typeof state === 'object' && state !== null && typeof (state as {workspace?: unknown}).workspace === 'string'
            ? vscode.Uri.parse((state as {workspace: string}).workspace) : undefined;
        const folder = workspace ? vscode.workspace.getWorkspaceFolder(workspace) : undefined;
        const presentation = typeof state === 'object' && state !== null && typeof (state as {presentation?: unknown}).presentation === 'object'
            ? (state as {presentation: {className?: unknown}}).presentation : undefined;
        this.revivalClass = typeof presentation?.className === 'string' ? presentation.className : undefined;
        return this.enqueue(async () => {
            if (!folder) {
                panel.dispose();
                throw new Error('The workspace for the serialized TotalCross preview is no longer open.');
            }
            await this.startPreview(panel, folder);
        });
    }
    public serializeState(): {workspace?: string; presentation?: {className?: string}} {
        const folder = this.client ? vscode.workspace.getWorkspaceFolder(vscode.Uri.file(this.client.root())) : undefined;
        return {workspace: folder?.uri.toString(), presentation: {className: this.pendingClass}};
    }
    public async openPreviewConfig(): Promise<void> {
        const folder = await this.selectWorkspaceFolder();
        if (!folder) return;
        const file = configPath(folder.uri.fsPath);
        await this.ensurePreviewConfiguration(folder.uri.fsPath);
        await vscode.window.showTextDocument(await vscode.workspace.openTextDocument(file));
    }

    public async selectPreviewMainWindow(): Promise<void> {
        const folder = await this.selectWorkspaceFolder();
        if (!folder) return;
        const candidates = await discoverMainWindowCandidates(folder.uri.fsPath);
        if (candidates.length === 0) {
            vscode.window.showInformationMessage('No TotalCross MainWindow class was found in this workspace.');
            return;
        }
        const selected = await vscode.window.showQuickPick(candidates.map((candidate) => ({label: candidate.className, description: candidate.file, candidate})), {
            title: 'Select the TotalCross Preview MainWindow'
        });
        if (!selected) return;
        const file = configPath(folder.uri.fsPath);
        const configuration = await readPreviewConfiguration(file);
        configuration.mainWindow = selected.candidate.className;
        await writePreviewConfiguration(file, configuration);
        if (this.client) await this.sendControl(this.client.root(), this.client.buildTool(), {command: 'reload', values: [configuration.mainWindow]});
    }

    private requestStop(disposePanel: boolean): Promise<void> {
        if (this.stopCompletion) return this.stopCompletion;
        const result = this.enqueue(() => this.stopPreview(disposePanel));
        this.stopCompletion = result.finally(() => {
            if (this.stopCompletion === completion) this.stopCompletion = undefined;
        });
        const completion = this.stopCompletion;
        return completion;
    }

    private async stopPreview(disposePanel = true): Promise<void> {
        const client = this.client;
        this.client = undefined;
        if (this.reloadTimer) clearTimeout(this.reloadTimer);
        this.reloadTimer = undefined;
        if (this.editorTimer) clearTimeout(this.editorTimer);
        this.editorTimer = undefined;
        this.editorListener?.dispose();
        this.editorListener = undefined;
        if (this.watcher) this.watcher.dispose();
        this.watcher = undefined;
        this.stopFramePolling();
        this.pendingClass = undefined;
        if (disposePanel) this.disposePanel();
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
        const model = await this.readProjectModel(this.client);
        const editor = vscode.window.activeTextEditor;
        if (editor && isJavaFile(editor.document.fileName) && isWorkspaceFile(editor.document.fileName, this.client.root())) {
            await this.presentActiveEditor(editor, model.classOutput);
        } else {
            await this.sendControl(this.client.root(), this.client.buildTool(), {command: 'reload', values: [model.mainClass]});
            this.show({kind: 'reload-requested', message: model.mainClass});
        }
    }

    private show(event: PreviewEvent): void {
        this.output.appendLine(`[${event.kind}] ${event.message || ''}`);
        if (event.kind === 'error') vscode.window.showErrorMessage(`TotalCross preview: ${event.message || 'unknown error'}`);
    }

    private startFramePolling(outputRoot: string): void {
        const frame = path.join(outputRoot, 'preview-frame.png');
        const interval = Math.max(100, vscode.workspace.getConfiguration('totalcross.livePreview').get<number>('framePollInterval', 500));
        const selection = path.join(outputRoot, 'preview-selection.json');
        this.selectionTimer = setInterval(() => {
            this.pollSelection(selection, frame).catch((error) => this.show({kind: 'error', message: error.message}));
        }, Math.min(interval, 50));
        this.frameTimer = setInterval(() => {
            this.pollFrame(frame).catch((error) => this.show({kind: 'error', message: error.message}));
        }, interval);
    }

    private async pollSelection(selection: string, frame: string): Promise<void> {
        if (!this.panel || !this.pendingClass) return;
        try {
            const marker = parseSelection(await fs.readFile(selection, 'utf8'));
            if (marker?.className !== this.pendingClass) return;
            const requested = this.pendingClass;
            this.pendingClass = undefined;
            if (!marker.selected) {
                this.show({kind: 'selection-failed', message: marker.error || requested});
                await this.clearPanel();
                return;
            }
            this.show({kind: 'selection-ready', message: requested});
            await this.pollFrame(frame);
        } catch (_) { /* The coordinator has not written the selection marker yet. */ }
    }

    private async pollFrame(frame: string): Promise<void> {
        if (!this.panel || this.pendingClass || this.pollingInFlight) return;
        this.pollingInFlight = true;
        const generation = this.pollingGeneration;
        try {
            const data = (await fs.readFile(frame)).toString('base64');
            if (generation === this.pollingGeneration && this.panel) {
                this.panel.webview.postMessage({type: 'frame', data: `data:image/png;base64,${data}`});
            }
        } catch (_) { /* The coordinator has not produced its first frame yet. */ }
        finally { this.pollingInFlight = false; }
    }

    private scheduleActiveEditorPreview(): void {
        if (this.editorTimer) clearTimeout(this.editorTimer);
        const generation = ++this.editorGeneration;
        this.editorTimer = setTimeout(() => {
            this.editorTimer = undefined;
            this.previewActiveEditor(generation).catch((error) => this.show({kind: 'error', message: error.message}));
        }, 150);
    }

    private async previewActiveEditor(generation: number): Promise<void> {
        const client = this.client;
        const editor = vscode.window.activeTextEditor;
        if (!client || !editor || !isJavaFile(editor.document.fileName) || !isWorkspaceFile(editor.document.fileName, client.root())) return;
        const model = await this.readProjectModel(client);
        if (generation !== this.editorGeneration || client !== this.client) return;
        await this.presentActiveEditor(editor, model.classOutput);
    }

    private async selectWorkspaceFolder(): Promise<vscode.WorkspaceFolder | undefined> {
        const folders = vscode.workspace.workspaceFolders || [];
        return folders.length <= 1 ? folders[0] : vscode.window.showWorkspaceFolderPick({placeHolder: 'Select the TotalCross project'});
    }

    private async ensurePreviewConfiguration(root: string): Promise<PreviewConfiguration> {
        const file = configPath(root);
        const configuration = await readPreviewConfiguration(file);
        if (configuration.mainWindow && /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+$/.test(configuration.mainWindow)) return configuration;
        const candidates = await discoverMainWindowCandidates(root);
        const preferred = candidates.filter((candidate) => candidate.preferred);
        const selectable = preferred.length === 1 ? preferred : candidates.length === 1 ? candidates : candidates;
        if (selectable.length === 1) {
            configuration.mainWindow = selectable[0].className;
            await writePreviewConfiguration(file, configuration);
            return configuration;
        }
        if (selectable.length > 1) {
            const selected = await vscode.window.showQuickPick(selectable.map((candidate) => ({label: candidate.className, description: candidate.file, candidate})), {
                title: 'Select the TotalCross Preview MainWindow'
            });
            if (selected) {
                configuration.mainWindow = selected.candidate.className;
                await writePreviewConfiguration(file, configuration);
            }
        } else {
            await writePreviewConfiguration(file, configuration);
        }
        return configuration;
    }

    private async readProjectModel(client: PreviewClient): Promise<{mainClass: string; classOutput: string}> {
        const compatible = client as PreviewClient & {projectModel?: () => Promise<{mainClass: string; classOutput: string}>};
        if (compatible.projectModel) return compatible.projectModel();
        return {mainClass: await client.mainClass(), classOutput: path.join(client.root(), 'build', 'classes', 'java', 'main')};
    }

    private async presentActiveEditor(editor: vscode.TextEditor, classOutput: string): Promise<void> {
        const client = this.client;
        if (!client) return;
        const className = sourceClassName(editor.document.getText(), editor.document.fileName);
        await this.presentClassName(className, classOutput);
    }

    private async presentClassName(className: string | undefined, classOutput: string): Promise<void> {
        const client = this.client;
        if (!client) return;
        if (!client.supportsSelection()) {
            this.pendingClass = undefined;
            await this.clearPanel();
            this.show({kind: 'error', message: 'The TotalCross preview plugin is outdated and does not support editor selection. Update the Gradle/Maven tooling and restart Preview.'});
            return;
        }
        if (!className || !(await compiledClassExists(className, classOutput))) {
            this.pendingClass = undefined;
            await this.clearPanel();
            return;
        }
        this.pendingClass = className;
        this.pollingGeneration++;
        await this.clearPanel();
        await fs.rm(path.join(this.previewRoot(client), 'preview-selection.json'), {force: true});
        await this.sendControl(client.root(), client.buildTool(), {command: 'show', values: [className]});
        this.show({kind: 'selection-requested', message: className});
    }

    private previewRoot(client: PreviewClient): string {
        return client.buildTool() === 'gradle' ? path.join(client.root(), 'build', 'totalcross') : path.join(client.root(), 'target', 'totalcross');
    }

    private async clearPanel(): Promise<void> {
        this.pollingGeneration++;
        if (this.panel) await this.panel.webview.postMessage({type: 'clear'});
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

    private stopFramePolling(): void {
        if (this.frameTimer) clearInterval(this.frameTimer);
        if (this.selectionTimer) clearInterval(this.selectionTimer);
        this.frameTimer = undefined;
        this.selectionTimer = undefined;
        this.pollingGeneration++;
        this.pollingInFlight = false;
    }

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

    private disposePanel(): void {
        const panel = this.panel;
        this.panel = undefined;
        panel?.dispose();
    }
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
window.addEventListener('message', event => { if (event.data.type === 'frame') frame.src = event.data.data; if (event.data.type === 'clear') frame.removeAttribute('src'); if (event.data.type === 'device') { device = event.data; frame.style.width = device.width + 'px'; frame.style.height = device.height + 'px'; sendResize(); } });
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
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewReload', () => manager.reload()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewStop', () => manager.stop()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewSelectMainWindow', () => manager.selectPreviewMainWindow()));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewOpenConfig', () => manager.openPreviewConfig()));
    context.subscriptions.push(vscode.window.registerWebviewPanelSerializer('totalcrossPreview', {
        deserializeWebviewPanel: (panel, state) => manager.revive(panel, state)
    }));
    context.subscriptions.push(vscode.commands.registerCommand('extension.previewDiagnostics', () => manager.showDiagnostics()));
    context.subscriptions.push({dispose: () => { void manager.stop().catch((error) => console.error('Unable to stop TotalCross preview:', error)); }});
    return manager;
}
