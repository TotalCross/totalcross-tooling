/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import {spawn} from 'child_process';
import * as path from 'path';

export interface LegacyConversionPlan {
    moves: unknown[];
    mainWindowCandidates: unknown[];
    sdkCandidates: unknown[];
    warnings: string[];
}

export interface ConversionCommand { executable: string; args: string[]; }

/** Builds the shell-free companion invocation used by installed and development VSIX instances. */
export function companionCommand(extensionPath: string, javaCommand: string, arguments_: string[]): ConversionCommand {
    return {executable: javaCommand, args: ['-jar', path.join(extensionPath, 'companion', 'totalcross-tooling.jar'), 'convert-project', ...arguments_]};
}

export function conversionPlanFromOutput(output: string): LegacyConversionPlan {
    const event = output.split(/\r?\n/).filter((line) => line.trim()).map((line) => JSON.parse(line))
        .find((item) => item && item.event === 'conversion-plan');
    if (!event || !event.plan || !Array.isArray(event.plan.moves) || !Array.isArray(event.plan.warnings)) {
        throw new Error('The TotalCross conversion companion did not return a valid conversion plan.');
    }
    return event.plan as LegacyConversionPlan;
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
