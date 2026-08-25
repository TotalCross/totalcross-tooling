/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import * as vscode from 'vscode';
import {activate} from '../../extension';

suite('Extension Test Suite', () => {
	vscode.window.showInformationMessage('Start all tests.');

	test('registers canonical Preview commands on activation', async () => {
		const context = {subscriptions: [] as vscode.Disposable[]} as vscode.ExtensionContext;
		activate(context);
		const commands = await vscode.commands.getCommands(true);
		for (const command of [
			'extension.preview',
			'extension.previewReload',
			'extension.previewStop',
			'extension.previewSelectMainWindow',
			'extension.previewOpenConfig'
		]) {
			assert.ok(commands.includes(command), `Expected registered command ${command}`);
		}
		await new Promise((resolve) => setTimeout(resolve, 0));
		for (const disposable of context.subscriptions.splice(0)) disposable.dispose();
	});
});
