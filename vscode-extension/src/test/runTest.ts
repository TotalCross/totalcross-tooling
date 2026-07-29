/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as path from 'path';
import * as os from 'os';
import {spawn} from 'child_process';
import {existsSync, promises as fs} from 'fs';

import {downloadAndUnzipVSCode, runTests} from '@vscode/test-electron';

// A VS Code extension host sets this for Node-based extension processes. The
// integration runner must launch the downloaded Electron application instead.
delete process.env.ELECTRON_RUN_AS_NODE;

async function downloadedVSCodeExecutable(extensionDevelopmentPath: string): Promise<string> {
	const downloaded = await downloadAndUnzipVSCode({extensionDevelopmentPath});
	const modernMacExecutable = path.join(path.dirname(downloaded), 'Code');
	return process.platform === 'darwin' && existsSync(modernMacExecutable) ? modernMacExecutable : downloaded;
}

function run(command: string, args: string[]): Promise<void> {
	return new Promise((resolve, reject) => {
		const child = spawn(command, args, {shell: false, stdio: 'inherit'});
		child.on('error', reject);
		child.on('close', (code) => code === 0 ? resolve() : reject(new Error(`${path.basename(command)} exited with ${code}`)));
	});
}

function codeCli(vscodeExecutablePath: string): string {
	return process.platform === 'darwin'
		? path.resolve(path.dirname(vscodeExecutablePath), '..', 'Resources', 'app', 'bin', 'code')
		: vscodeExecutablePath;
}

async function installVsix(vscodeExecutablePath: string, vsixPath: string, extensionId: string): Promise<{extensionPath: string; temporaryRoot: string}> {
	const temporaryRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'totalcross-vsix-test-'));
	const extensionsDir = path.join(temporaryRoot, 'extensions');
	try {
		await run(codeCli(vscodeExecutablePath), ['--install-extension', vsixPath, '--force', `--extensions-dir=${extensionsDir}`, `--user-data-dir=${path.join(temporaryRoot, 'user-data')}`]);
		const entries = await fs.readdir(extensionsDir, {withFileTypes: true});
		const extension = entries.find((entry) => entry.isDirectory() && entry.name.toLowerCase().startsWith(`${extensionId.toLowerCase()}-`));
		if (!extension) { throw new Error('VS Code did not install the TotalCross VSIX.'); }
		return {extensionPath: path.join(extensionsDir, extension.name), temporaryRoot};
	} catch (error) {
		await fs.rm(temporaryRoot, {recursive: true, force: true});
		throw error;
	}
}

async function main() {
	try {
		// The folder containing the Extension Manifest package.json
		// Passed to `--extensionDevelopmentPath`
		const extensionDevelopmentPath = path.resolve(__dirname, '../../');
		const manifest = JSON.parse(await fs.readFile(path.join(extensionDevelopmentPath, 'package.json'), 'utf8'));

		// The path to test runner
		// Passed to --extensionTestsPath
		const extensionTestsPath = path.resolve(__dirname, './suite/index');
		const vscodeExecutablePath = await downloadedVSCodeExecutable(extensionDevelopmentPath);
		const vsixArgument = process.argv.indexOf('--vsix');
		const vsixPath = vsixArgument >= 0 ? path.resolve(process.argv[vsixArgument + 1]) : undefined;
		const installed = vsixPath ? await installVsix(vscodeExecutablePath, vsixPath, `${manifest.publisher}.${manifest.name}`) : undefined;

		// Download VS Code, unzip it and run the integration test
		try {
			await runTests({
				vscodeExecutablePath,
				extensionDevelopmentPath: installed?.extensionPath || extensionDevelopmentPath,
				extensionTestsPath,
				// macOS limits the Unix-domain socket path used by Electron; keep test
				// state outside the repository's long workspace path.
				launchArgs: ['--user-data-dir=/tmp/tc-vscode-test-user-data']
			});
		} finally {
			if (installed) { await fs.rm(installed.temporaryRoot, {recursive: true, force: true}); }
		}
	} catch (err) {
		console.error('Failed to run tests');
		process.exit(1);
	}
}

main();
