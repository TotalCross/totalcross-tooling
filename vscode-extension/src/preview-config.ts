/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import {promises as fs} from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';

export interface PreviewConfiguration {
    mainWindow: string;
    launcherArgs: string[];
    classpath: string[];
    [key: string]: unknown;
}
const DEFAULT_CONFIGURATION: PreviewConfiguration = {mainWindow: '', launcherArgs: [], classpath: []};

export function configPath(root: string): string { return path.join(root, 'totalcross-preview.json'); }

export async function readPreviewConfiguration(file: string): Promise<PreviewConfiguration> {
    try {
        const parsed = JSON.parse(await fs.readFile(file, 'utf8')) as Record<string, unknown>;
        return normalizeConfiguration(parsed);
    } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
        return {...DEFAULT_CONFIGURATION};
    }
}

export function normalizeConfiguration(value: Record<string, unknown>): PreviewConfiguration {
    return {
        ...value,
        mainWindow: typeof value.mainWindow === 'string' ? value.mainWindow : '',
        launcherArgs: Array.isArray(value.launcherArgs) ? value.launcherArgs.filter((item): item is string => typeof item === 'string') : [],
        classpath: Array.isArray(value.classpath) ? value.classpath.filter((item): item is string => typeof item === 'string') : []
    };
}

export async function writePreviewConfiguration(file: string, configuration: PreviewConfiguration): Promise<void> {
    const directory = path.dirname(file);
    await fs.mkdir(directory, {recursive: true});
    const temporary = `${file}.${process.pid}.${Date.now()}.tmp`;
    await fs.writeFile(temporary, `${JSON.stringify(configuration, null, 2)}\n`, 'utf8');
    await fs.rename(temporary, file);
}

export interface MainWindowCandidate {className: string; file: string; preferred: boolean;}

export function mainWindowCandidates(sources: Array<{file: string; text: string}>): MainWindowCandidate[] {
    const candidates: MainWindowCandidate[] = [];
    const simpleToFqn = new Map<string, string>();
    for (const source of sources) {
        const pkg = /(?:^|\n)\s*package\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)\s*;/m.exec(source.text)?.[1] ?? '';
        const regex = /\bclass\s+([A-Za-z_$][\w$]*)[^\{;]*\bextends\s+(?:totalcross\.ui\.)?MainWindow\b/g;
        let match: RegExpExecArray | null;
        while ((match = regex.exec(source.text))) {
            const className = pkg ? `${pkg}.${match[1]}` : match[1];
            candidates.push({className, file: source.file, preferred: false});
            simpleToFqn.set(match[1], className);
        }
    }
    const preferred = new Set<string>();
    for (const source of sources) {
        const regex = /TotalCrossApplication\.run\s*\(\s*([A-Za-z_$][\w$.]*)\.class/g;
        let match: RegExpExecArray | null;
        while ((match = regex.exec(source.text))) preferred.add(simpleToFqn.get(match[1]) ?? match[1]);
    }
    return candidates.map((candidate) => ({...candidate, preferred: preferred.has(candidate.className)}));
}

export async function discoverMainWindowCandidates(root: string): Promise<MainWindowCandidate[]> {
    const files = await vscode.workspace.findFiles(new vscode.RelativePattern(vscode.Uri.file(root), '**/*.java'), '**/{build,out,target,.gradle,node_modules}/**');
    const sources = await Promise.all(files.map(async (file) => ({file: file.fsPath, text: Buffer.from(await vscode.workspace.fs.readFile(file)).toString('utf8')})));
    return mainWindowCandidates(sources);
}

export function configurationClassPath(configuration: PreviewConfiguration, root: string): string[] {
    const legacy = ['classOutputPaths', 'resourcePaths', 'dependencyPaths'].flatMap((key) => {
        const value = configuration[key];
        return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === 'string') : [];
    });
    return [...configuration.classpath, ...legacy].map((entry) => path.resolve(root, entry));
}
