/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */
import * as childProcess from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';

import {
  DEFAULT_CONFIG, PreviewConfig,
  classOutputPattern, compiledClassExists, controlTimeoutMs, defaultConfigFromSettings,
  buildJavaArguments, discoverMainWindows, escapeHtml, findFreePort, formatProcess, isJavaSource,
  isValidClassName,
  postControl, previewJvmArgs, previewPort, relativeClassPath,
  sourceClassName, waitForPreview
} from './live-preview-utils';

export {livePreviewTest} from './live-preview-utils';

const VIEW_TYPE = 'totalcrossLivePreview';
const CONFIG_FILE = 'totalcross.preview.json';
const PREVIEW_HOST = '127.0.0.1';
let session: PreviewSession | undefined;
let activation: vscode.Disposable | undefined;

export function activateLivePreview(context: vscode.ExtensionContext): vscode.Disposable {
  if (activation) {
    return activation;
  }
  session = new PreviewSession();
  const activeSession = session;
  activation = vscode.Disposable.from(
    activeSession,
    vscode.commands.registerCommand('totalcross.startPreview', () => session?.start()),
    vscode.commands.registerCommand('totalcross.openLivePreview', () => session?.start()),
    vscode.commands.registerCommand('totalcross.stopPreview', () => session?.stop()),
    vscode.commands.registerCommand('totalcross.reloadPreview', () => session?.reload()),
    vscode.commands.registerCommand('totalcross.openPreviewConfig', () => session?.openConfig()),
    vscode.window.registerWebviewPanelSerializer(VIEW_TYPE, {
      async deserializeWebviewPanel(panel: vscode.WebviewPanel): Promise<void> {
        await session?.revive(panel);
      }
    }),
    {
      dispose: () => {
        if (session === activeSession) {
          session = undefined;
        }
        activation = undefined;
      }
    }
  );
  context.subscriptions.push(activation);
  return activation;
}

export function deactivateLivePreview(): void {
  const currentActivation = activation;
  activation = undefined;
  currentActivation?.dispose();
  if (session) {
    session.dispose();
    session = undefined;
  }
}

class PreviewSession implements vscode.Disposable {
  private readonly output = vscode.window.createOutputChannel('TotalCross Live Preview');
  private readonly disposables: vscode.Disposable[] = [];
  private panel: vscode.WebviewPanel | undefined;
  private process: childProcess.ChildProcess | undefined;
  private selectedPort = 0;
  private baseUrl = '';
  private activePreviewTimer: NodeJS.Timeout | undefined;
  private busy = false;
  private readonly classWatchers: vscode.FileSystemWatcher[] = [];
  private watchedConfig: PreviewConfig | undefined;
  private watchedWorkspaceFolder: vscode.WorkspaceFolder | undefined;
  private activeClassName: string | undefined;

  constructor() {
    this.disposables.push(this.output);
    this.disposables.push(
      vscode.window.onDidChangeActiveTextEditor((editor) => {
        if (editor && isJavaSource(editor.document.uri)) {
          this.scheduleActiveEditorPreview();
        }
      })
    );
  }

  async start(existingPanel?: vscode.WebviewPanel): Promise<void> {
    if (this.busy) {
      return;
    }
    this.busy = true;
    try {
      const workspaceFolder = this.workspaceFolder();
      if (!workspaceFolder) {
        vscode.window.showErrorMessage('Open a workspace folder before starting TotalCross Preview.');
        return;
      }
      const configPath = this.configPath(workspaceFolder);
      const config = await this.ensureConfig(configPath);
      if (!(await this.ensureMainWindow(configPath, config))) {
        return;
      }

      const panel = existingPanel ?? this.panel ?? this.createPanel();
      this.attachPanel(panel);
      panel.webview.html = this.loadingHtml('Starting TotalCross preview...');

      await this.startProcess(config, configPath, workspaceFolder);
      this.watchCompiledClasses(config, workspaceFolder);
      panel.webview.html = this.previewHtml(panel.webview, this.baseUrl, config);
      this.scheduleActiveEditorPreview();
    } catch (error) {
      this.reportError(error);
    } finally {
      this.busy = false;
    }
  }

  async reload(): Promise<void> {
    await this.reloadPreview(true, true);
  }

  async revive(panel: vscode.WebviewPanel): Promise<void> {
    this.attachPanel(panel);
    await this.start(panel);
  }

  private async reloadPreview(restartOnFailure: boolean, startIfStopped: boolean): Promise<void> {
    if (this.busy) {
      return;
    }
    if (!this.process) {
      if (startIfStopped) {
        await this.start();
      }
      return;
    }
    this.busy = true;
    try {
      const workspaceFolder = this.workspaceFolder();
      if (!workspaceFolder) {
        return;
      }
      const configPath = this.configPath(workspaceFolder);
      const config = await this.ensureConfig(configPath);
      if (restartOnFailure && config.reloadMode === 'full') {
        this.output.appendLine('Restarting preview process.');
        this.stopProcess();
        await this.startProcess(config, configPath, workspaceFolder);
      } else {
        const response = await postControl(`${this.baseUrl}/reload`, controlTimeoutMs());
        if (response.ok) {
          this.output.appendLine('Preview reloaded in the running server.');
        } else if (restartOnFailure) {
          this.output.appendLine(`Preview reload unavailable: ${response.error ?? response.body}`);
          this.output.appendLine('Restarting preview process.');
          this.stopProcess();
          await this.startProcess(config, configPath, workspaceFolder);
        } else {
          this.output.appendLine(`Preview reload failed: ${response.error ?? response.body}`);
          this.output.appendLine('Keeping the running preview server.');
        }
      }
      this.watchCompiledClasses(config, workspaceFolder);
      if (this.panel) {
        this.panel.webview.html = this.previewHtml(this.panel.webview, this.baseUrl, config);
      }
    } catch (error) {
      this.reportError(error);
    } finally {
      this.busy = false;
    }
  }

  async stop(): Promise<void> {
    if (this.baseUrl) {
      await postControl(`${this.baseUrl}/shutdown`, controlTimeoutMs());
    }
    this.stopProcess();
    this.panel?.dispose();
    this.panel = undefined;
  }

  async openConfig(): Promise<void> {
    const workspaceFolder = this.workspaceFolder();
    if (!workspaceFolder) {
      vscode.window.showErrorMessage('Open a workspace folder before editing TotalCross Preview config.');
      return;
    }
    const configPath = this.configPath(workspaceFolder);
    await this.ensureConfig(configPath);
    const document = await vscode.workspace.openTextDocument(configPath);
    await vscode.window.showTextDocument(document);
  }

  dispose(): void {
    this.stopProcess();
    for (const disposable of this.disposables.splice(0)) {
      disposable.dispose();
    }
  }

  private scheduleActiveEditorPreview(): void {
    if (this.activePreviewTimer) {
      clearTimeout(this.activePreviewTimer);
    }
    this.activePreviewTimer = setTimeout(() => {
      this.previewActiveEditor().catch((error) => this.reportError(error));
    }, 150);
  }

  private async startProcess(config: PreviewConfig, configPath: vscode.Uri, workspaceFolder: vscode.WorkspaceFolder): Promise<void> {
    this.stopProcess();
    const settings = vscode.workspace.getConfiguration('totalcross.livePreview');
    const configuredPort = settings.get<number>('port', 0);
    this.selectedPort = previewPort(configuredPort) || await findFreePort(PREVIEW_HOST);
    this.baseUrl = `http://${PREVIEW_HOST}:${this.selectedPort}`;
    const javaCommand = settings.get<string>('javaCommand', 'java');
    const classpath = this.buildClasspath(config, workspaceFolder);
    const jvmArgs = previewJvmArgs(settings);
    const args = buildJavaArguments(jvmArgs, classpath, configPath.fsPath, this.selectedPort);
    this.output.appendLine(`Starting preview: ${formatProcess(javaCommand, args)}`);
    this.process = childProcess.spawn(javaCommand, args, {
      cwd: workspaceFolder.uri.fsPath,
      shell: false,
      env: process.env
    });
    this.process.stdout?.on('data', (data) => this.output.append(data.toString()));
    this.process.stderr?.on('data', (data) => this.output.append(data.toString()));
    this.process.on('exit', (code, signal) => {
      this.output.appendLine(`Preview process exited with code ${code ?? 'none'} signal ${signal ?? 'none'}.`);
    });
    await waitForPreview(this.baseUrl, 15000);
  }

  private createPanel(): vscode.WebviewPanel {
    return vscode.window.createWebviewPanel(
      VIEW_TYPE,
      'TotalCross Live Preview',
      vscode.ViewColumn.Beside,
      {
        enableScripts: true,
        retainContextWhenHidden: true
      }
    );
  }

  private attachPanel(panel: vscode.WebviewPanel): void {
    if (this.panel === panel) {
      return;
    }
    this.panel = panel;
    panel.webview.options = {
      enableScripts: true
    };
    panel.onDidDispose(() => {
      this.stopProcess();
      if (this.panel === panel) {
        this.panel = undefined;
      }
    }, null, this.disposables);
  }

  private stopProcess(): void {
    if (this.process && !this.process.killed) {
      this.process.kill();
    }
    this.process = undefined;
    this.baseUrl = '';
    this.disposeClassWatchers();
  }

  private watchCompiledClasses(config: PreviewConfig, workspaceFolder: vscode.WorkspaceFolder): void {
    this.disposeClassWatchers();
    this.watchedConfig = config;
    this.watchedWorkspaceFolder = workspaceFolder;
    for (const outputPath of config.classOutputPaths ?? []) {
      if (!outputPath || outputPath.includes('*')) {
        continue;
      }
      const pattern = classOutputPattern(outputPath, workspaceFolder);
      const watcher = vscode.workspace.createFileSystemWatcher(pattern);
      watcher.onDidCreate((uri) => this.onCompiledClassChanged(uri));
      watcher.onDidChange((uri) => this.onCompiledClassChanged(uri));
      watcher.onDidDelete((uri) => this.onCompiledClassChanged(uri));
      this.classWatchers.push(watcher);
      this.disposables.push(watcher);
    }
    if (this.classWatchers.length > 0) {
      this.output.appendLine(`Watching compiled classes in ${this.classWatchers.length} output path(s).`);
    } else {
      this.output.appendLine('No classOutputPaths configured; automatic class-change reload is disabled.');
    }
  }

  private onCompiledClassChanged(uri: vscode.Uri): void {
    if (!this.process) {
      return;
    }
    this.output.appendLine(`Compiled class changed: ${uri.fsPath}`);
    const className = this.changedClassName(uri);
    if (className && this.activeClassName === className) {
      this.scheduleActiveEditorPreview();
    }
  }

  private async previewActiveEditor(): Promise<void> {
    if (!this.process) {
      return;
    }
    const editor = vscode.window.activeTextEditor;
    const workspaceFolder = this.watchedWorkspaceFolder;
    const config = this.watchedConfig;
    if (!editor || !isJavaSource(editor.document.uri)) {
      return;
    }
    if (!workspaceFolder || !config) {
      return;
    }

    const className = await sourceClassName(editor.document.uri);
    this.activeClassName = className;
    if (!className || !compiledClassExists(className, config, workspaceFolder)) {
      await this.clearPreview();
      return;
    }

    const shown = await this.showChangedClass(className);
    if (!shown) {
      await this.clearPreview();
    }
  }

  private async showChangedClass(className: string): Promise<boolean> {
    if (this.busy || !this.process) {
      return false;
    }
    this.busy = true;
    try {
      const response = await postControl(`${this.baseUrl}/show`, controlTimeoutMs(), { className });
      if (response.ok) {
        this.output.appendLine(`Preview is showing ${className}.`);
        return true;
      }
      this.output.appendLine(`Preview cannot show ${className}: ${response.error ?? response.body}`);
      return false;
    } finally {
      this.busy = false;
    }
  }

  private async clearPreview(): Promise<void> {
    if (this.busy || !this.process) {
      return;
    }
    this.busy = true;
    try {
      const response = await postControl(`${this.baseUrl}/clear`, controlTimeoutMs());
      if (response.ok) {
        this.output.appendLine('Preview cleared.');
      } else {
        this.output.appendLine(`Preview clear failed: ${response.error ?? response.body}`);
      }
    } finally {
      this.busy = false;
    }
  }

  private changedClassName(uri: vscode.Uri): string | undefined {
    const config = this.watchedConfig;
    const workspaceFolder = this.watchedWorkspaceFolder;
    if (!config || !workspaceFolder || !uri.fsPath.endsWith('.class')) {
      return undefined;
    }
    for (const outputPath of config.classOutputPaths ?? []) {
      if (!outputPath || outputPath.includes('*')) {
        continue;
      }
      const root = path.isAbsolute(outputPath) ? outputPath : path.join(workspaceFolder.uri.fsPath, outputPath);
      const relative = relativeClassPath(root, uri.fsPath);
      if (relative) {
        return relative;
      }
    }
    return undefined;
  }

  private disposeClassWatchers(): void {
    for (const watcher of this.classWatchers.splice(0)) {
      watcher.dispose();
      const index = this.disposables.indexOf(watcher);
      if (index >= 0) {
        this.disposables.splice(index, 1);
      }
    }
    this.watchedConfig = undefined;
    this.watchedWorkspaceFolder = undefined;
    this.activeClassName = undefined;
  }

  private async ensureConfig(configPath: vscode.Uri): Promise<PreviewConfig> {
    try {
      const bytes = await vscode.workspace.fs.readFile(configPath);
      return {
        ...DEFAULT_CONFIG,
        ...JSON.parse(Buffer.from(bytes).toString('utf8'))
      };
    } catch {
      const config = defaultConfigFromSettings();
      if (fs.existsSync(path.join(path.dirname(configPath.fsPath), 'pom.xml'))) {
        config.buildCommand = 'mvn compile';
        config.classOutputPaths = ['target/classes'];
        config.dependencyPaths = ['target/dependency', 'target/lib'];
        config.headlessOutput = 'target/totalcross-preview/preview.png';
      }
      await vscode.workspace.fs.writeFile(configPath, Buffer.from(`${JSON.stringify(config, null, 2)}\n`, 'utf8'));
      return config;
    }
  }

  private async ensureMainWindow(configPath: vscode.Uri, config: PreviewConfig): Promise<boolean> {
    const candidates = await discoverMainWindows();
    if (config.mainWindow && (isValidClassName(config.mainWindow) || candidates.some((candidate) => candidate.className === config.mainWindow))) {
      return true;
    }

    const preferred = candidates.filter((candidate) => candidate.preferred);
    const selectable = (preferred.length ? preferred : candidates).map((candidate) => ({
      label: candidate.className,
      description: path.relative(this.workspaceFolder()?.uri.fsPath ?? '', candidate.uri.fsPath),
      candidate
    }));

    if (selectable.length > 0) {
      const selected = await vscode.window.showQuickPick(selectable, {
        title: 'Select the TotalCross MainWindow class to preview'
      });
      if (!selected) {
        return false;
      }
      config.mainWindow = selected.candidate.className;
    } else {
      const input = await vscode.window.showInputBox({
        title: 'TotalCross MainWindow',
        prompt: 'Enter the fully qualified MainWindow class name'
      });
      if (!input || !isValidClassName(input)) {
        vscode.window.showErrorMessage('TotalCross Preview requires a valid fully qualified MainWindow class.');
        return false;
      }
      config.mainWindow = input;
    }

    await vscode.workspace.fs.writeFile(configPath, Buffer.from(`${JSON.stringify(config, null, 2)}\n`, 'utf8'));
    return true;
  }

  private buildClasspath(config: PreviewConfig, workspaceFolder: vscode.WorkspaceFolder): string {
    const settings = vscode.workspace.getConfiguration('totalcross.livePreview');
    const extraClasspath = settings.get<string[]>('extraClasspath', []);
    const paths = [
      ...config.classOutputPaths,
      ...config.resourcePaths,
      ...config.dependencyPaths,
      ...extraClasspath
    ];
    const classpath: string[] = [];
    for (const entry of paths) {
      const absolute = path.isAbsolute(entry) ? entry : path.join(workspaceFolder.uri.fsPath, entry);
      classpath.push(absolute);
      if (fs.existsSync(absolute) && fs.statSync(absolute).isDirectory()) {
        classpath.push(path.join(absolute, '*'));
      }
    }
    return classpath.join(path.delimiter);
  }

  private configPath(workspaceFolder: vscode.WorkspaceFolder): vscode.Uri {
    return vscode.Uri.file(path.join(workspaceFolder.uri.fsPath, CONFIG_FILE));
  }

  private workspaceFolder(): vscode.WorkspaceFolder | undefined {
    return vscode.workspace.workspaceFolders?.[0];
  }

  private reportError(error: unknown): void {
    const message = error instanceof Error ? error.message : String(error);
    this.output.appendLine(message);
    vscode.window.showErrorMessage(`TotalCross Live Preview: ${message}`);
  }

  private loadingHtml(message: string): string {
    return `<!DOCTYPE html><html><body><p>${escapeHtml(message)}</p></body></html>`;
  }

  private previewHtml(webview: vscode.Webview, baseUrl: string, config: PreviewConfig): string {
    const nonce = String(Date.now());
    const frameUrl = `${baseUrl}/frame`;
    const settings = vscode.workspace.getConfiguration('totalcross.livePreview');
    const interval = Math.max(100, settings.get<number>('framePollInterval', 500));
    return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src ${webview.cspSource} http://127.0.0.1:* http://localhost:* data:; script-src 'nonce-${nonce}'; style-src 'unsafe-inline';">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    body { margin: 0; padding: 16px; background: var(--vscode-editor-background); color: var(--vscode-editor-foreground); font-family: var(--vscode-font-family); }
    #frame { image-rendering: auto; max-width: 100%; border: 1px solid var(--vscode-panel-border); background: #000; }
    #status { margin: 0 0 12px; }
  </style>
</head>
<body>
  <p id="status">Connected to ${escapeHtml(baseUrl)}</p>
  <img id="frame" alt="TotalCross live preview" width="${config.width}" height="${config.height}">
  <script nonce="${nonce}">
    const frame = document.getElementById('frame');
    const status = document.getElementById('status');
    const frameUrl = '${frameUrl}';
    async function refreshFrame() {
      frame.onload = () => { status.textContent = 'Live preview updated'; };
      frame.onerror = () => { status.textContent = 'Waiting for preview frame...'; };
      frame.src = frameUrl + '?t=' + Date.now();
    }
    refreshFrame();
    setInterval(refreshFrame, ${interval});
  </script>
</body>
</html>`;
  }
}
