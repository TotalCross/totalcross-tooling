/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import * as path from 'path';
import {companionCommand, conversionEventFromOutput, conversionPlanFromOutput} from '../../migration/legacy-conversion-client';

suite('Legacy conversion companion client', () => {
    test('uses the packaged JAR without a shell and parses its versioned plan event', () => {
        const command = companionCommand('/extension', 'java', ['analyze', '--project', '/project']);
        assert.equal(command.executable, 'java');
        assert.deepEqual(command.args.slice(0, 4), ['-jar', path.join('/extension', 'companion', 'totalcross-tooling.jar'), 'convert-project', 'analyze']);
        const plan = conversionPlanFromOutput('{"schemaVersion":1,"event":"conversion-plan","plan":{"moves":[],"mainWindowCandidates":[],"sdkCandidates":[],"warnings":["select"]}}\n');
        assert.equal(plan.warnings[0], 'select');
        assert.equal(conversionEventFromOutput('{"event":"conversion-applied","journal":"/journal"}\n', 'conversion-applied').journal, '/journal');
    });
});
