/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import * as path from 'path';
import {companionCommand, conversionEventFromOutput, conversionPlanFromOutput, conversionPlanSummary} from '../../migration/legacy-conversion-client';

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
});
