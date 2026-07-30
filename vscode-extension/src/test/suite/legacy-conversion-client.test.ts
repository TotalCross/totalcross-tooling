/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import {promises as fs, existsSync} from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as vscode from 'vscode';
import {companionCommand, conversionEventFromOutput, conversionPlanFromOutput, conversionPlanSummary, runConversion} from '../../migration/legacy-conversion-client';

suite('Legacy conversion companion client', () => {
    test('uses the packaged JAR without a shell and parses its versioned plan event', () => {
        const command = companionCommand('/extension', 'java', ['analyze', '--project', '/project']);
        assert.equal(command.executable, 'java');
        assert.deepEqual(command.args.slice(0, 4), ['-jar', path.join('/extension', 'companion', 'totalcross-tooling.jar'), 'convert-project', 'analyze']);
        const plan = conversionPlanFromOutput('{"schemaVersion":1,"event":"conversion-plan","plan":{"moves":[{"source":"App.java","destination":"src/main/java/App.java","kind":"main-source"}],"generatedFiles":[{"destination":"build.gradle","kind":"gradle-build"}],"mainWindowCandidates":[],"sdkCandidates":[],"scriptEvidence":[{"script":"build.sh","line":1,"kind":"launcher","arguments":["java"],"secretPresent":false}],"launcherArguments":[],"deployArguments":[],"warnings":["select"]}}\n');
        assert.equal(plan.warnings[0], 'select');
        const summary = conversionPlanSummary(plan).join('\n');
        assert.ok(summary.includes('launcher build.sh:1 java'));
        assert.ok(summary.includes('main-source: App.java -> src/main/java/App.java'));
        assert.ok(summary.includes('gradle-build: build.gradle'));
        assert.equal(conversionEventFromOutput('{"event":"conversion-applied","journal":"/journal"}\n', 'conversion-applied').journal, '/journal');
    });

    test('executes the installed VSIX companion against a legacy project', async function() {
        const extension = vscode.extensions.all.find((item) => item.packageJSON.name === 'vscode-totalcross' && item.packageJSON.publisher === 'TotalCross');
        const jar = extension && path.join(extension.extensionPath, 'companion', 'totalcross-tooling.jar');
        if (!jar || !existsSync(jar)) { this.skip(); return; }
        const project = await fs.mkdtemp(path.join(os.tmpdir(), 'totalcross-installed-conversion-'));
        try {
            await fs.writeFile(path.join(project, 'App.java'), 'public class App extends totalcross.ui.MainWindow {}');
            await fs.writeFile(path.join(project, 'legacy.sh'), 'java totalcross.Launcher 7.6.0\njava tc.Deploy App -android /q\n');
            const output = await runConversion(companionCommand(extension.extensionPath, 'java', ['analyze', '--project', project]), project);
            const plan = conversionPlanFromOutput(output);
            assert.equal(plan.mainWindowCandidates[0].className, 'App');
            assert.deepEqual(plan.deployArguments[0].platforms, ['-android']);
            assert.ok(plan.generatedFiles.some((file) => file.destination === 'build.gradle'));
        } finally {
            await fs.rm(project, {recursive: true, force: true});
        }
    });
});
