/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import {promises as fs} from 'fs';
import * as os from 'os';
import * as vscode from 'vscode';
import {companionCommand, conversionPlanFromOutput, runConversion} from './legacy-conversion-client';

/** Presents an analysis produced by the packaged CLI; this module never classifies or moves project files. */
export async function analyzeLegacyProject(context: vscode.ExtensionContext, folder?: vscode.WorkspaceFolder): Promise<void> {
    const selected = folder || (vscode.workspace.workspaceFolders || [])[0];
    if (!selected) {
        vscode.window.showErrorMessage('Select a folder to analyze as a TotalCross project.');
        return;
    }
    const temporary = await fs.mkdtemp(`${os.tmpdir()}/totalcross-conversion-`);
    const plan = `${temporary}/conversion-plan.json`;
    const output = vscode.window.createOutputChannel('TotalCross Project Conversion');
    try {
        const javaCommand = vscode.workspace.getConfiguration('totalcross').get<string>('livePreview.javaCommand', 'java');
        const command = companionCommand(context.extensionPath, javaCommand, ['analyze', '--project', selected.uri.fsPath, '--plan', plan]);
        const result = await vscode.window.withProgress({location: vscode.ProgressLocation.Notification, title: 'Analyzing TotalCross project...'},
            () => runConversion(command, selected.uri.fsPath));
        const conversion = conversionPlanFromOutput(result);
        output.appendLine(`Moves: ${conversion.moves.length}`);
        output.appendLine(`MainWindow candidates: ${conversion.mainWindowCandidates.length}`);
        output.appendLine(`SDK candidates: ${conversion.sdkCandidates.length}`);
        conversion.warnings.forEach((warning) => output.appendLine(`Warning: ${warning}`));
        output.show(true);
        vscode.window.showInformationMessage(`TotalCross analysis found ${conversion.moves.length} proposed moves and ${conversion.warnings.length} warnings.`);
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        output.appendLine(message);
        output.show(true);
        vscode.window.showErrorMessage(`Unable to analyze the TotalCross project: ${message}`);
    } finally {
        await fs.rm(temporary, {recursive: true, force: true});
    }
}
