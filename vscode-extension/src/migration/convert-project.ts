/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import {spawn} from 'child_process';
import {promises as fs} from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import {DEFAULT_GRADLE_PLUGIN_VERSION} from '../project-generator';
import {GENERATED_GRADLE_MARKER, RenderedGradleProject, renderGradleProject} from './gradle-renderer';
import {readMavenTotalCrossProject} from './maven-project';
import {classifyProject} from './project-classifier';
import {clearMigrationReminder} from './reminder-state';

export interface ConversionResult {
    status: 'converted' | 'plugin-unavailable' | 'already-gradle' | 'unsupported' | 'failed';
    message: string;
    details?: string;
}

type Validator = () => Promise<void>;
const BACKUP_DIRECTORY = '.totalcross-migration-backup';
const GRADLE_PREVIEW_CONFIG = {
    buildCommand: './gradlew classes',
    classOutputPaths: ['build/classes/java/main'],
    resourcePaths: ['src/main/resources'],
    dependencyPaths: ['build/libs'],
    headlessOutput: 'build/totalcross-preview/preview.png'
};
const LEGACY_ECLIPSE_METADATA = [
    '.classpath',
    '.project',
    '.settings/org.eclipse.core.resources.prefs',
    '.settings/org.eclipse.jdt.apt.core.prefs',
    '.settings/org.eclipse.jdt.core.prefs',
    '.settings/org.eclipse.m2e.core.prefs'
];

/** Removes Eclipse/Maven project metadata that would override Gradle import in VS Code. */
export async function removeLegacyEclipseMetadata(root: string): Promise<void> {
    for (const relative of LEGACY_ECLIPSE_METADATA) {
        try { await fs.unlink(path.join(root, relative)); } catch (_) { /* Already absent. */ }
    }
}

async function exists(file: string): Promise<boolean> {
    try { await fs.access(file); return true; } catch (_) { return false; }
}

/** Removes an existing directory without treating a missing path as an error. */
async function removeTree(directory: string): Promise<void> {
    try { await fs.rm(directory, {recursive: true, force: true}); } catch (_) { /* Already absent. */ }
}

async function writeAtomic(file: string, contents: Buffer | string): Promise<void> {
    await fs.mkdir(path.dirname(file), {recursive: true});
    const temporary = `${file}.totalcross-tmp-${process.pid}`;
    await fs.writeFile(temporary, contents);
    await fs.rename(temporary, file);
}

async function copyToBackup(root: string, backup: string, relative: string): Promise<boolean> {
    const source = path.join(root, relative);
    if (!(await exists(source))) {
        return false;
    }
    const destination = path.join(backup, relative);
    await fs.mkdir(path.dirname(destination), {recursive: true});
    await fs.copyFile(source, destination);
    return true;
}

async function restore(root: string, backup: string, originalFiles: Set<string>, changedFiles: string[]): Promise<void> {
    for (const relative of changedFiles) {
        const destination = path.join(root, relative);
        if (originalFiles.has(relative)) {
            await fs.mkdir(path.dirname(destination), {recursive: true});
            await fs.copyFile(path.join(backup, relative), destination);
        } else {
            try { await fs.unlink(destination); } catch (_) { /* It was not written. */ }
        }
    }
}

async function nextPomBackup(root: string): Promise<string> {
    let suffix = '.maven-backup';
    for (let number = 1; await exists(path.join(root, `pom.xml${suffix}`)); number++) {
        suffix = `.maven-backup.${number}`;
    }
    return path.join(root, `pom.xml${suffix}`);
}

async function mergeGitIgnore(root: string): Promise<string | undefined> {
    const file = path.join(root, '.gitignore');
    const existing = (await exists(file)) ? await fs.readFile(file, 'utf8') : '';
    const additions = ['gradle.properties', `${BACKUP_DIRECTORY}/`].filter((entry) => existing.split(/\r?\n/).indexOf(entry) === -1);
    return additions.length ? `${existing}${existing && !existing.endsWith('\n') ? '\n' : ''}${additions.join('\n')}\n` : undefined;
}

async function migratePreviewConfig(root: string, files: Map<string, Buffer | string>): Promise<void> {
    const relative = 'totalcross.preview.json';
    const file = path.join(root, relative);
    if (!(await exists(file))) {
        return;
    }
    const source = await fs.readFile(file, 'utf8');
    let config: {[key: string]: unknown};
    try {
        const parsed = JSON.parse(source);
        if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
            return;
        }
        config = parsed as {[key: string]: unknown};
    } catch (_) {
        return;
    }
    const buildCommand = typeof config.buildCommand === 'string' ? config.buildCommand : '';
    const classOutputPaths = Array.isArray(config.classOutputPaths) ? config.classOutputPaths : [];
    const isMavenPreview = /\bmvn(?:w)?\b/i.test(buildCommand)
        || classOutputPaths.some((entry) => typeof entry === 'string' && entry.replace(/\\/g, '/').startsWith('target/'));
    if (!isMavenPreview) {
        return;
    }
    files.set(relative, `${JSON.stringify({...config, ...GRADLE_PREVIEW_CONFIG}, null, 2)}\n`);
}

/** Repairs the preview descriptor when a generated Gradle project is revisited. */
export async function synchronizeGradlePreviewConfig(root: string): Promise<boolean> {
    const files = new Map<string, Buffer | string>();
    await migratePreviewConfig(root, files);
    const contents = files.get('totalcross.preview.json');
    if (typeof contents !== 'string') {
        return false;
    }
    await writeAtomic(path.join(root, 'totalcross.preview.json'), contents);
    return true;
}

function isMissingPlugin(error: Error): boolean {
    return /com\.totalcross\.application|com\.totalcross\.application\.gradle\.plugin/i.test(error.message)
        && /(not found|could not resolve|could not find|unknown plugin)/i.test(error.message);
}

export function validateGradlePreviewTasks(output: string): void {
    const missing = ['totalcrossPreview', 'totalcrossRun'].filter((task) => !new RegExp(`^\\s*${task}(?:\\s|$)`, 'm').test(output));
    if (missing.length > 0) {
        throw new Error(`The installed TotalCross Gradle plugin does not provide ${missing.join(' and ')}. Configure a released TotalCross Gradle plugin version and run the conversion again.`);
    }
}

function runWrapper(root: string): Promise<void> {
    const command = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';
    return new Promise((resolve, reject) => {
        const child = spawn(command, ['tasks', '--console=plain'], {cwd: root, shell: false});
        let output = '';
        child.stdout.on('data', (chunk) => output += chunk.toString());
        child.stderr.on('data', (chunk) => output += chunk.toString());
        child.on('error', (error) => reject(error));
        child.on('close', (code) => {
            if (code !== 0) {
                reject(new Error(`Gradle Wrapper exited with ${code}.\n${output}`));
                return;
            }
            try {
                validateGradlePreviewTasks(output);
                resolve();
            } catch (error) {
                reject(error);
            }
        });
    });
}

/** Performs file writes atomically and restores pre-existing files after any ordinary validation failure. */
export async function writeAndValidateGradleProject(root: string, rendered: RenderedGradleProject, validate: Validator): Promise<'converted' | 'plugin-unavailable'> {
    const backup = path.join(root, BACKUP_DIRECTORY);
    await removeTree(backup);
    await fs.mkdir(backup, {recursive: true});
    const files = new Map(rendered.files);
    await migratePreviewConfig(root, files);
    const mergedIgnore = await mergeGitIgnore(root);
    if (mergedIgnore !== undefined) { files.set('.gitignore', mergedIgnore); }
    const gradleProperties = files.get('gradle.properties');
    if (typeof gradleProperties === 'string' && await exists(path.join(root, 'gradle.properties'))) {
        const existing = await fs.readFile(path.join(root, 'gradle.properties'), 'utf8');
        const activation = gradleProperties.trim();
        files.set('gradle.properties', /^totalcrossActivationKey=.*$/m.test(existing)
            ? existing.replace(/^totalcrossActivationKey=.*$/m, activation)
            : `${existing}${existing && !existing.endsWith('\n') ? '\n' : ''}${activation}\n`);
    }
    const written = Array.from(files.keys());
    const changed = Array.from(new Set([...written, ...LEGACY_ECLIPSE_METADATA]));
    const originals = new Set<string>();
    try {
        for (const relative of changed) {
            if (await copyToBackup(root, backup, relative)) { originals.add(relative); }
        }
        for (const relative of written) {
            await writeAtomic(path.join(root, relative), files.get(relative)!);
        }
        if (process.platform !== 'win32') {
            const wrapper = path.join(root, 'gradlew');
            if (await exists(wrapper)) {
                await fs.chmod(wrapper, (await fs.stat(wrapper)).mode | 0o111);
            }
        }
        await validate();
        await removeLegacyEclipseMetadata(root);
        await fs.rename(path.join(root, 'pom.xml'), await nextPomBackup(root));
        await removeTree(backup);
        return 'converted';
    } catch (error) {
        const failure = error instanceof Error ? error : new Error(String(error));
        if (isMissingPlugin(failure)) {
            await removeTree(backup);
            return 'plugin-unavailable';
        }
        await restore(root, backup, originals, changed);
        await removeTree(backup);
        throw failure;
    }
}

async function generatedMigrationBuild(root: string): Promise<boolean> {
    try {
        return (await fs.readFile(path.join(root, 'build.gradle'), 'utf8')).indexOf(GENERATED_GRADLE_MARKER) !== -1;
    } catch (_) {
        return false;
    }
}

/** Converts one explicitly selected workspace folder and never invokes a shell with its path. */
export async function convertMavenProjectToGradle(context: vscode.ExtensionContext, folder: vscode.WorkspaceFolder): Promise<ConversionResult> {
    const output = vscode.window.createOutputChannel('TotalCross Migration');
    const root = folder.uri.fsPath;
    try {
        const classification = await classifyProject(root);
        if (classification.kind === 'gradle-present' && !(await generatedMigrationBuild(root))) {
            const result = {status: 'already-gradle' as 'already-gradle', message: 'This project already contains a Gradle build. No conversion was performed.'};
            vscode.window.showInformationMessage(result.message);
            return result;
        }
        if (classification.kind === 'gradle-present') {
            if (await generatedMigrationBuild(root)) {
                await runWrapper(root);
                await removeLegacyEclipseMetadata(root);
            }
            const repaired = await synchronizeGradlePreviewConfig(root);
            const message = repaired
                ? 'This project already contains the generated Gradle build. Its TotalCross preview configuration was synchronized.'
                : 'This project already contains a Gradle build. No conversion was performed.';
            const result = {status: 'already-gradle' as 'already-gradle', message};
            vscode.window.showInformationMessage(result.message);
            return result;
        }
        if (classification.kind !== 'eligible') {
            const result = {status: 'unsupported' as 'unsupported', message: 'This workspace is not a supported TotalCross Maven project.'};
            vscode.window.showErrorMessage(result.message);
            return result;
        }
        const project = await readMavenTotalCrossProject(path.join(root, 'pom.xml'));
        const pluginVersion = vscode.workspace.getConfiguration('totalcross').get<string>('gradlePluginVersion', DEFAULT_GRADLE_PLUGIN_VERSION);
        const rendered = renderGradleProject(project, pluginVersion);
        const status = await vscode.window.withProgress({location: vscode.ProgressLocation.Notification, title: 'Converting TotalCross project to Gradle...'}, () => writeAndValidateGradleProject(root, rendered, () => runWrapper(root)));
        if (status === 'plugin-unavailable') {
            const result = {status, message: `The Gradle files were created, but TotalCross Gradle plugin ${pluginVersion} could not be resolved from the configured release repositories. Configure totalcross.gradlePluginVersion with an available released version, then run the conversion again.`};
            vscode.window.showErrorMessage(result.message);
            return result;
        }
        await clearMigrationReminder(context, folder.uri.toString());
        const result = {status, message: 'The TotalCross project was converted to Gradle successfully.'};
        vscode.window.showInformationMessage(result.message);
        return result;
    } catch (error) {
        const details = error instanceof Error ? error.message : String(error);
        output.appendLine(details);
        output.show(true);
        const result = {status: 'failed' as 'failed', message: `Unable to convert the TotalCross project: ${details}`, details};
        vscode.window.showErrorMessage(result.message);
        return result;
    }
}
