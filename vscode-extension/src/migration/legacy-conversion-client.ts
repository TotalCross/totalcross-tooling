/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import {spawn} from 'child_process';
import * as path from 'path';

export interface LegacyConversionPlan {
    moves: Array<{source: string; destination: string; kind: string}>;
    mainWindowCandidates: Array<{className: string; source: string; evidence: string}>;
    sdkCandidates: Array<{version: string; script: string; line: number; evidenceKind: string}>;
    scriptEvidence: Array<{script: string; line: number; kind: string; arguments: string[]; secretPresent: boolean}>;
    launcherArguments: Array<{script: string; line: number; platforms: string[]; arguments: string[]}>;
    deployArguments: Array<{script: string; line: number; platforms: string[]; arguments: string[]}>;
    warnings: string[];
}

export interface ConversionCommand { executable: string; args: string[]; }

/** Builds the shell-free companion invocation used by installed and development VSIX instances. */
export function companionCommand(extensionPath: string, javaCommand: string, arguments_: string[]): ConversionCommand {
    return {executable: javaCommand, args: ['-jar', path.join(extensionPath, 'companion', 'totalcross-tooling.jar'), 'convert-project', ...arguments_]};
}

export function conversionPlanFromOutput(output: string): LegacyConversionPlan {
    const event = conversionEventFromOutput(output, 'conversion-plan');
    if (!event || !event.plan || !Array.isArray(event.plan.moves) || !Array.isArray(event.plan.scriptEvidence) || !Array.isArray(event.plan.warnings)) {
        throw new Error('The TotalCross conversion companion did not return a valid conversion plan.');
    }
    return event.plan as LegacyConversionPlan;
}

/** Formats the reviewed shared-engine plan for an Output channel without adding TypeScript conversion rules. */
export function conversionPlanSummary(plan: LegacyConversionPlan): string[] {
    const lines = ['Detected evidence:'];
    append(lines, plan.scriptEvidence, (item) => `  ${item.kind} ${item.script}:${item.line} ${item.arguments.join(' ')}${item.secretPresent ? ' (secret redacted)' : ''}`);
    lines.push('MainWindow candidates:');
    append(lines, plan.mainWindowCandidates, (item) => `  ${item.className} (${item.evidence}, ${item.source})`);
    lines.push('SDK candidates:');
    append(lines, plan.sdkCandidates, (item) => `  ${item.version} (${item.evidenceKind}, ${item.script}:${item.line})`);
    lines.push('Proposed moves:');
    append(lines, plan.moves, (item) => `  ${item.kind}: ${item.source} -> ${item.destination}`);
    lines.push('Launcher and Deploy mappings:');
    append(lines, [...plan.launcherArguments, ...plan.deployArguments], (item) =>
        `  ${item.script}:${item.line} platforms=[${item.platforms.join(', ')}] arguments=[${item.arguments.join(' ')}]`);
    lines.push('Generated files on Apply: settings.gradle, build.gradle, gradlew, gradlew.bat, gradle/wrapper/.');
    lines.push('Warnings and conflicts:');
    append(lines, plan.warnings, (warning) => `  ${warning}`);
    return lines;
}

function append<T>(lines: string[], values: T[], format: (value: T) => string): void {
    if (values.length === 0) { lines.push('  (none)'); return; }
    values.slice(0, 20).forEach((value) => lines.push(format(value)));
    if (values.length > 20) { lines.push(`  ... ${values.length - 20} additional entries`); }
}

export function conversionEventFromOutput(output: string, name: string): any {
    return output.split(/\r?\n/).filter((line) => line.trim()).map((line) => JSON.parse(line))
        .find((item) => item && item.event === name);
}

/** Executes the companion without a shell; all analysis and mutation remain in shared Java tooling. */
export function runConversion(command: ConversionCommand, workingDirectory: string): Promise<string> {
    return new Promise((resolve, reject) => {
        const child = spawn(command.executable, command.args, {cwd: workingDirectory, shell: false});
        let output = '';
        child.stdout.on('data', (chunk: Buffer) => output += chunk.toString());
        child.stderr.on('data', (chunk: Buffer) => output += chunk.toString());
        child.on('error', reject);
        child.on('close', (code: number | null) => code === 0 ? resolve(output) : reject(new Error(output || `conversion companion exited with ${code}`)));
    });
}
