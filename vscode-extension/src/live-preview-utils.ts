/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */
import * as fs from 'fs';
import * as http from 'http';
import * as net from 'net';
import * as path from 'path';
import * as vscode from 'vscode';

export type PreviewConfig = {
  mainWindow: string;
  launcherArgs: string[];
  buildCommand: string;
  classOutputPaths: string[];
  resourcePaths: string[];
  dependencyPaths: string[];
  previewMode: 'windowed' | 'headless';
  reloadMode: 'fast' | 'full';
  width: number;
  height: number;
  scale: number;
  platform: string;
  headlessOutput: string;
};

export type MainWindowCandidate = {
  className: string;
  uri: vscode.Uri;
  preferred: boolean;
};

export type ControlResponse = { ok: boolean; body: string; error?: string; };

export const DEFAULT_CONFIG: PreviewConfig = {
  mainWindow: '',
  launcherArgs: ['width', '500', 'height', '600'],
  buildCommand: '',
  classOutputPaths: ['build/classes/java/main'],
  resourcePaths: ['src/main/resources'],
  dependencyPaths: ['build/libs', 'lib'],
  previewMode: 'windowed',
  reloadMode: 'fast',
  width: 500,
  height: 600,
  scale: 1,
  platform: 'android',
  headlessOutput: 'build/totalcross-preview/preview.png'
};

export async function discoverMainWindows(): Promise<MainWindowCandidate[]> {
  const files = await vscode.workspace.findFiles('**/*.java', '**/{build,out,target,.gradle,node_modules}/**');
  const runReferences = new Set<string>();
  const candidates: MainWindowCandidate[] = [];
  const simpleNames = new Map<string, string>();

  for (const uri of files) {
    const text = Buffer.from(await vscode.workspace.fs.readFile(uri)).toString('utf8');
    const pkg = packageName(text);
    const classRegex = /\bclass\s+([A-Za-z_][\w]*)[^\{;]*\bextends\s+(?:totalcross\.ui\.)?MainWindow\b/g;
    let classMatch: RegExpExecArray | null;
    while ((classMatch = classRegex.exec(text))) {
      const fqn = pkg ? `${pkg}.${classMatch[1]}` : classMatch[1];
      simpleNames.set(classMatch[1], fqn);
      candidates.push({className: fqn, uri, preferred: false});
    }
  }

  for (const uri of files.filter((file) => /Run.*Application\.java$/.test(file.fsPath))) {
    const text = Buffer.from(await vscode.workspace.fs.readFile(uri)).toString('utf8');
    const runRegex = /TotalCrossApplication\.run\s*\(\s*([A-Za-z_][\w.]*)\.class/g;
    let runMatch: RegExpExecArray | null;
    while ((runMatch = runRegex.exec(text))) {
      runReferences.add(runMatch[1]);
      runReferences.add(simpleNames.get(runMatch[1]) ?? runMatch[1]);
    }
  }

  for (const candidate of candidates) {
    const simple = candidate.className.substring(candidate.className.lastIndexOf('.') + 1);
    candidate.preferred = runReferences.has(candidate.className) || runReferences.has(simple);
  }
  return candidates;
}

export async function sourceClassName(uri: vscode.Uri): Promise<string | undefined> {
  const text = Buffer.from(await vscode.workspace.fs.readFile(uri)).toString('utf8');
  const pkg = packageName(text);
  const className = topLevelClassName(text) ?? path.basename(uri.fsPath, '.java');
  if (!isSimpleClassName(className)) return undefined;
  return pkg ? `${pkg}.${className}` : className;
}

export function topLevelClassName(text: string): string | undefined {
  const match = /\b(?:public\s+)?(?:(?:abstract|final)\s+)*class\s+([A-Za-z_][\w]*)\b/.exec(text);
  return match ? match[1] : undefined;
}

export function compiledClassExists(className: string, config: PreviewConfig, workspaceFolder: vscode.WorkspaceFolder): boolean {
  const relative = className.replace(/\./g, path.sep) + '.class';
  for (const outputPath of config.classOutputPaths ?? []) {
    if (!outputPath || outputPath.includes('*')) continue;
    const root = path.isAbsolute(outputPath) ? outputPath : path.join(workspaceFolder.uri.fsPath, outputPath);
    if (fs.existsSync(path.join(root, relative))) return true;
  }
  return false;
}

export function previewJvmArgs(settings: Pick<vscode.WorkspaceConfiguration, 'get'>, platform = process.platform): string[] {
  const args = [...settings.get<string[]>('jvmArgs', [])];
  if (platform === 'darwin' && !args.some((arg) => arg.startsWith('-Dapple.awt.UIElement='))) {
    args.unshift('-Dapple.awt.UIElement=true');
  }
  return args;
}

export function buildJavaArguments(jvmArgs: string[], classpath: string, configPath: string, port: number): string[] {
  return [...jvmArgs, '-cp', classpath, 'com.totalcross.livepreview.LivePreviewServer', '--config', configPath,
    '--host', '127.0.0.1', '--port', String(port)];
}

export function previewPort(port: number): number {
  if (!Number.isInteger(port) || port < 0 || port > 65535) throw new Error('totalcross.livePreview.port must be an integer from 0 to 65535.');
  return port;
}

export function formatProcess(command: string, args: string[]): string { return JSON.stringify([command, ...args]); }
export function isJavaSource(uri: vscode.Uri): boolean { return uri.fsPath.endsWith('.java'); }
export function isSimpleClassName(value: string): boolean { return /^[A-Za-z_][\w]*$/.test(value); }

export async function findFreePort(host: string): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.on('error', reject);
    server.listen(0, host, () => {
      const address = server.address();
      server.close(() => typeof address === 'object' && address ? resolve(address.port) : reject(new Error('Could not allocate a preview port.')));
    });
  });
}

export async function waitForPreview(baseUrl: string, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await getHealth(baseUrl)) return;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  throw new Error(`Preview service did not start at ${baseUrl}.`);
}

function getHealth(baseUrl: string): Promise<boolean> {
  return new Promise((resolve) => {
    const request = http.get(`${baseUrl}/health`, (response) => { response.resume(); resolve(response.statusCode === 200); });
    request.on('error', () => resolve(false));
    request.setTimeout(1000, () => { request.destroy(); resolve(false); });
  });
}

export function postControl(url: string, timeoutMs: number, body?: unknown): Promise<ControlResponse> {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (response: ControlResponse) => { if (!settled) { settled = true; resolve(response); } };
    const bodyText = body === undefined ? undefined : JSON.stringify(body);
    const request = http.request(url, {method: 'POST', headers: bodyText === undefined ? undefined : {
      'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(bodyText)
    }}, (response) => {
      const chunks: Buffer[] = [];
      response.on('data', (chunk) => chunks.push(Buffer.from(chunk)));
      response.on('end', () => {
        const body = Buffer.concat(chunks).toString('utf8');
        const ok = response.statusCode !== undefined && response.statusCode >= 200 && response.statusCode < 300;
        finish({ok, body, error: responseError(body)});
      });
    });
    request.on('error', (error) => finish({ok: false, body: error.message, error: error.message}));
    request.setTimeout(timeoutMs, () => { request.destroy(); finish({ok: false, body: `Request timed out after ${timeoutMs}ms.`, error: `Request timed out after ${timeoutMs}ms.`}); });
    request.end(bodyText);
  });
}

export function controlTimeoutMs(): number {
  return Math.max(1000, vscode.workspace.getConfiguration('totalcross.livePreview').get<number>('controlTimeout', 30000));
}

function responseError(body: string): string | undefined {
  try { const parsed = JSON.parse(body); return typeof parsed.error === 'string' && parsed.error.length > 0 ? parsed.error : undefined; }
  catch { return undefined; }
}

export function packageName(text: string): string {
  const match = /\bpackage\s+([A-Za-z_][\w.]*)\s*;/.exec(text);
  return match ? match[1] : '';
}

export function isValidClassName(value: string): boolean { return /^[A-Za-z_][\w]*(\.[A-Za-z_][\w]*)+$/.test(value); }

export function defaultConfigFromSettings(): PreviewConfig {
  const settings = vscode.workspace.getConfiguration('totalcross.livePreview');
  return defaultConfigFromValues(settings.get<number>('width', DEFAULT_CONFIG.width), settings.get<number>('height', DEFAULT_CONFIG.height),
    settings.get<string>('orientation', 'portrait'), settings.get<string>('deviceProfile', DEFAULT_CONFIG.platform));
}

export function defaultConfigFromValues(configuredWidth: number, configuredHeight: number, orientation: string, deviceProfile: string): PreviewConfig {
  let width = Math.max(1, configuredWidth);
  let height = Math.max(1, configuredHeight);
  if (orientation === 'landscape' && width < height) [width, height] = [height, width];
  else if (orientation === 'portrait' && width > height) [width, height] = [height, width];
  return {...DEFAULT_CONFIG, launcherArgs: ['width', String(width), 'height', String(height)], width, height, platform: deviceProfile};
}

export function classOutputPattern(outputPath: string, workspaceFolder: vscode.WorkspaceFolder): vscode.RelativePattern {
  if (path.isAbsolute(outputPath)) return new vscode.RelativePattern(vscode.Uri.file(outputPath), '**/*.class');
  return new vscode.RelativePattern(workspaceFolder, `${toGlobPath(outputPath)}/**/*.class`);
}

export function relativeClassPath(outputRoot: string, classFile: string): string | undefined {
  const relative = path.relative(outputRoot, classFile);
  if (!relative || relative.startsWith('..') || path.isAbsolute(relative) || !relative.endsWith('.class')) return undefined;
  return toGlobPath(relative.substring(0, relative.length - '.class'.length).replace(/\$.*$/, '')).replace(/\//g, '.');
}

export function toGlobPath(value: string): string { return value.replace(/\\/g, '/').split(path.sep).join('/'); }

export function escapeHtml(value: string): string {
  return value.replace(/[&<>"']/g, (char) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[char] || char));
}

export const livePreviewTest = {
  buildJavaArguments, defaultConfigFromValues, defaultConfigFromSettings, packageName, previewJvmArgs, relativeClassPath, topLevelClassName
};
