/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import {ChildProcess, SpawnOptions, spawn} from 'child_process';
import {existsSync, promises as fs} from 'fs';
import * as path from 'path';
import {BuildTool, ProjectLayout} from './project-layout';

export interface PreviewEvent { kind: string; message?: string; [key: string]: any; }
export interface PreviewCommand { executable: string; args: string[]; }
export interface PreviewProjectModel { mainClass: string; classOutput: string; }
export type PreviewProcessSpawner = (executable: string, args: readonly string[], options: SpawnOptions) => ChildProcess;

export function previewEnvironment(environment: NodeJS.ProcessEnv, platform: NodeJS.Platform, additionalOptions: string[] = []): NodeJS.ProcessEnv {
    const requiredOptions = [...additionalOptions, '-Djava.awt.headless=true'];
    if (platform === 'darwin') requiredOptions.push('-Dapple.awt.UIElement=true');
    const inheritedOptions = environment.JAVA_TOOL_OPTIONS?.trim();
    return {
        ...environment,
        JAVA_TOOL_OPTIONS: inheritedOptions
            ? `${inheritedOptions} ${requiredOptions.join(' ')}`
            : requiredOptions.join(' ')
    };
}

export function mavenExecutable(root: string, platform: NodeJS.Platform, exists: (file: string) => boolean = existsSync): string {
    const wrapper = platform === 'win32' ? 'mvnw.cmd' : 'mvnw';
    if (exists(path.join(root, wrapper))) return platform === 'win32' ? '.\\mvnw.cmd' : './mvnw';
    return platform === 'win32' ? 'mvn.cmd' : 'mvn';
}

export function previewCommand(layout: ProjectLayout, platform: NodeJS.Platform, launchWindow = true): PreviewCommand {
    if (layout.buildTool === 'maven') {
        return {executable: mavenExecutable(layout.root, platform), args: ['compile', 'totalcross:preview'].concat(launchWindow ? [] : ['-Dtotalcross-preview.noLaunch=true'])};
    }
    return {executable: platform === 'win32' ? 'gradlew.bat' : './gradlew', args: ['totalcrossPreview', '--console=plain'].concat(launchWindow ? [] : ['-Ptotalcross-preview.noLaunch=true'])};
}

export function buildCommand(layout: ProjectLayout, platform: NodeJS.Platform): PreviewCommand {
    if (layout.buildTool === 'maven') return {executable: mavenExecutable(layout.root, platform), args: ['compile']};
    return {executable: platform === 'win32' ? 'gradlew.bat' : './gradlew', args: ['totalcrossProjectModel', '--console=plain']};
}

export class PreviewClient {
    private process?: ChildProcess;
    private processCompletion?: Promise<void>;
    private readiness?: Promise<void>;
    private stopCompletion?: Promise<void>;
    private readonly listeners: Array<(event: PreviewEvent) => void> = [];
    private lastEvent?: PreviewEvent;

    public constructor(
        private readonly layout: ProjectLayout,
        private readonly platform: NodeJS.Platform = process.platform,
        private readonly spawnProcess: PreviewProcessSpawner = spawn,
        private readonly jvmArgs: string[] = []
    ) {}

    public onEvent(listener: (event: PreviewEvent) => void): void { this.listeners.push(listener); }
    public root(): string { return this.layout.root; }
    public buildTool(): BuildTool { return this.layout.buildTool; }

    public async start(): Promise<void> {
        if (this.process && !this.process.killed) return;
        const command = previewCommand(this.layout, this.platform);
        const child = this.spawnProcess(command.executable, command.args, {
            cwd: this.layout.root,
            shell: false,
            env: previewEnvironment(process.env, this.platform, this.jvmArgs)
        });
        this.process = child;
        child.stdout!.on('data', (chunk: Buffer) => this.readOutput(chunk.toString()));
        child.stderr!.on('data', (chunk: Buffer) => this.emit({kind: 'diagnostic', message: chunk.toString()}));
        let resolveReady: () => void = () => undefined;
        let rejectReady: (error: Error) => void = () => undefined;
        this.readiness = new Promise<void>((resolve, reject) => { resolveReady = resolve; rejectReady = reject; });
        this.processCompletion = new Promise((resolve) => {
            let settled = false;
            const finish = () => {
                if (settled) return;
                settled = true;
                resolve();
            };
            child.on('close', (code: number | null) => {
                if (this.process === child) this.process = undefined;
                this.emit({kind: 'closed', message: `preview launcher exited with ${code}`});
                if (code === 0) resolveReady();
                else rejectReady(new Error(`preview launcher exited with ${code}`));
                finish();
            });
            child.on('error', (error: Error) => {
                if (this.process === child) this.process = undefined;
                this.emit({kind: 'error', message: error.message});
                rejectReady(error);
                finish();
            });
        });
        this.emit({kind: 'start', message: command.executable});
    }

    public ready(): Promise<void> { return this.readiness ?? Promise.resolve(); }

    public async reload(): Promise<void> {
        await this.run(buildCommand(this.layout, this.platform));
        this.emit({kind: 'build-succeeded', message: this.layout.root});
    }

    public async projectModel(): Promise<PreviewProjectModel> {
        const root = this.layout.buildTool === 'gradle' ? this.layout.packageOutputRoot : path.join(this.layout.packageOutputRoot, 'totalcross');
        const modelFile = path.join(root, 'project-model.json');
        let model: any;
        try {
            model = JSON.parse(await fs.readFile(modelFile, 'utf8'));
        } catch (error) {
            if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
                const generationTask = this.layout.buildTool === 'gradle'
                    ? 'totalcrossProjectModel'
                    : 'compile totalcross:preview';
                throw new Error(`Preview project model is missing at ${modelFile}; ${generationTask} did not complete successfully or was not executed.`);
            }
            throw error;
        }
        if (!model || typeof model.mainClass !== 'string' || !model.mainClass.trim()) throw new Error('preview project model has no mainClass');
        // Older generated models predate classOutput. Keep the reader tolerant for
        // those models; when the field exists it remains the authoritative path.
        const classOutput = typeof model.classOutput === 'string' && model.classOutput.trim()
            ? model.classOutput : path.join(this.layout.root, this.layout.buildTool === 'gradle' ? 'build/classes/java/main' : 'target/classes');
        return {mainClass: model.mainClass, classOutput: path.resolve(classOutput)};
    }

    public async mainClass(): Promise<string> {
        return (await this.projectModel()).mainClass;
    }

    public stop(): Promise<void> {
        if (this.stopCompletion) return this.stopCompletion;
        const stop = this.stopOwnedPreview();
        const completion = stop.finally(() => {
            if (this.stopCompletion === completion) this.stopCompletion = undefined;
        });
        this.stopCompletion = completion;
        return completion;
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
            const child = this.spawnProcess(command.executable, command.args, {cwd: this.layout.root, shell: false});
            let output = '';
            child.stdout!.on('data', (chunk: Buffer) => output += chunk.toString());
            child.stderr!.on('data', (chunk: Buffer) => output += chunk.toString());
            child.on('error', reject);
            child.on('close', (code: number | null) => code === 0 ? resolve() : reject(new Error(output || `build exited with ${code}`)));
        });
    }

    private async stopOwnedPreview(): Promise<void> {
        const launcher = this.process;
        const launcherCompletion = this.processCompletion;
        if (launcherCompletion) await launcherCompletion;
        try {
            await this.run(this.stopCommand());
            this.emit({kind: 'closed', message: 'preview stopped'});
        } finally {
            if (this.process === launcher) this.process = undefined;
        }
    }

    private stopCommand(): PreviewCommand {
        return this.layout.buildTool === 'maven'
            ? {executable: mavenExecutable(this.layout.root, this.platform), args: ['totalcross:preview-stop']}
            : {executable: this.platform === 'win32' ? 'gradlew.bat' : './gradlew', args: ['totalcrossPreviewStop', '--console=plain']};
    }

    private emit(event: PreviewEvent): void {
        this.lastEvent = event;
        this.listeners.forEach((listener) => listener(event));
    }
}
