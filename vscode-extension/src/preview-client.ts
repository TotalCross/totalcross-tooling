/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import {ChildProcess, spawn} from 'child_process';
import {promises as fs} from 'fs';
import * as path from 'path';
import {BuildTool, ProjectLayout} from './project-layout';

export interface PreviewEvent { kind: string; message?: string; [key: string]: any; }
export interface PreviewCommand { executable: string; args: string[]; }

export function previewCommand(layout: ProjectLayout, platform: NodeJS.Platform, launchWindow = true): PreviewCommand {
    if (layout.buildTool === 'maven') {
        return {executable: platform === 'win32' ? 'mvn.cmd' : 'mvn', args: ['totalcross:preview'].concat(launchWindow ? [] : ['-Dtotalcross.preview.noLaunch=true'])};
    }
    return {executable: platform === 'win32' ? 'gradlew.bat' : './gradlew', args: ['totalcrossPreview', '--console=plain'].concat(launchWindow ? [] : ['-Ptotalcross.preview.noLaunch=true'])};
}

export class PreviewClient {
    private process?: ChildProcess;
    private readonly listeners: Array<(event: PreviewEvent) => void> = [];
    private lastEvent?: PreviewEvent;

    public constructor(private readonly layout: ProjectLayout, private readonly platform: NodeJS.Platform = process.platform) {}

    public onEvent(listener: (event: PreviewEvent) => void): void { this.listeners.push(listener); }

    public async start(): Promise<void> {
        if (this.process && !this.process.killed) return;
        const command = previewCommand(this.layout, this.platform);
        this.process = spawn(command.executable, command.args, {cwd: this.layout.root, shell: false});
        this.process.stdout!.on('data', (chunk: Buffer) => this.readOutput(chunk.toString()));
        this.process.stderr!.on('data', (chunk: Buffer) => this.emit({kind: 'diagnostic', message: chunk.toString()}));
        this.process.on('close', (code: number | null) => {
            this.process = undefined;
            this.emit({kind: 'closed', message: `preview exited with ${code}`});
        });
        this.process.on('error', (error: Error) => this.emit({kind: 'error', message: error.message}));
        this.emit({kind: 'start', message: command.executable});
    }

    public async reload(): Promise<void> {
        const command = previewCommand(this.layout, this.platform, false);
        await this.run(command);
        this.emit({kind: 'reload-ready', message: this.layout.root});
    }

    public stop(): void {
        if (this.process) this.process.kill();
        this.process = undefined;
        this.emit({kind: 'closed', message: 'preview stopped'});
    }

    public diagnostics(): PreviewEvent | undefined { return this.lastEvent; }

    private readOutput(output: string): void {
        output.split(/\r?\n/).filter((line) => line.trim()).forEach((line) => {
            try { this.emit(JSON.parse(line)); }
            catch (_) { this.emit({kind: 'diagnostic', message: line}); }
        });
    }

    private run(command: PreviewCommand): Promise<void> {
        return new Promise((resolve, reject) => {
            const child = spawn(command.executable, command.args, {cwd: this.layout.root, shell: false});
            let output = '';
            child.stdout.on('data', (chunk: Buffer) => output += chunk.toString());
            child.stderr.on('data', (chunk: Buffer) => output += chunk.toString());
            child.on('error', reject);
            child.on('close', (code: number | null) => code === 0 ? resolve() : reject(new Error(output || `build exited with ${code}`)));
        });
    }

    private emit(event: PreviewEvent): void {
        this.lastEvent = event;
        this.listeners.forEach((listener) => listener(event));
    }
}
