/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

import { runTests } from '@vscode/test-electron';

// A VS Code extension host sets this for Node-based extension processes. The
// integration runner must launch the downloaded Electron application instead.
delete process.env.ELECTRON_RUN_AS_NODE;

async function main() {
	const userDataDir = fs.mkdtempSync(path.join(os.tmpdir(), 'tc-vscode-test-'));
	try {
		// The folder containing the Extension Manifest package.json
		// Passed to `--extensionDevelopmentPath`
		const extensionDevelopmentPath = path.resolve(__dirname, '../../');

		// The path to test runner
		// Passed to --extensionTestsPath
		const extensionTestsPath = path.resolve(__dirname, './suite/index');

		// Download VS Code, unzip it and run the integration test
		await runTests({
			extensionDevelopmentPath,
			extensionTestsPath,
			launchArgs: [`--user-data-dir=${userDataDir}`],
		});
	} catch (err) {
		console.error('Failed to run tests');
		process.exit(1);
	} finally {
		(fs as any).rmSync(userDataDir, { recursive: true, force: true });
	}
}

main();
