/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import * as vscode from 'vscode';
import {activateLivePreview, deactivateLivePreview} from '../../live-preview';

suite('Extension Test Suite', () => {
	vscode.window.showInformationMessage('Start all tests.');

	test('registers Live Preview commands on activation', async () => {
		const context = {subscriptions: [] as vscode.Disposable[]} as vscode.ExtensionContext;
		const disposable = activateLivePreview(context);
		const commands = await vscode.commands.getCommands(true);
		for (const command of [
			'totalcross.startPreview',
			'totalcross.openLivePreview',
			'totalcross.stopPreview',
			'totalcross.reloadPreview',
			'totalcross.openPreviewConfig'
		]) {
			assert.ok(commands.includes(command), `Expected registered command ${command}`);
		}
		disposable.dispose();
		deactivateLivePreview();
	});
});
