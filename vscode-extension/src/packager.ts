/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as vscode from 'vscode';
import {detectProjectLayout} from './project-layout';

export async function packageProject(): Promise<void> {
    const workspaceFolders = vscode.workspace.workspaceFolders;
    if (!workspaceFolders) {
        vscode.window.showErrorMessage('TotalCross project not found in this VS Code instance.');
        return;
    }
    const layout = await detectProjectLayout(workspaceFolders[0].uri.fsPath, process.platform);
    if (layout && layout.buildTool === 'mixed') {
        vscode.window.showErrorMessage('This project contains both Maven and Gradle build files. Complete or select the migration before packaging.');
        return;
    }
    if (!layout) {
        vscode.window.showErrorMessage('No supported TotalCross project found. Expected a Gradle wrapper and build file, or pom.xml.');
        return;
    }
    const terminal = vscode.window.createTerminal({
        name: `TotalCross Packager (${layout.buildTool})`,
        cwd: layout.root
    });
    terminal.show();
    terminal.sendText(layout.packageCommand, true);
}
