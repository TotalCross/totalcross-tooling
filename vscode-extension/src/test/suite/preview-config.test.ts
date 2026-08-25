/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
import {configurationClassPath, mainWindowCandidates, normalizeConfiguration} from '../../preview-config';

suite('Preview configuration', () => {
    test('preserves unknown fields while normalizing canonical values', () => {
        const configuration = normalizeConfiguration({futureSetting: true, mainWindow: 'demo.App', launcherArgs: ['width', 360], classpath: ['lib']});
        assert.strictEqual(configuration.futureSetting, true);
        assert.deepStrictEqual(configuration.launcherArgs, ['width']);
        assert.deepStrictEqual(configuration.classpath, ['lib']);
    });

    test('merges canonical and legacy classpath inputs relative to the workspace', () => {
        const configuration = normalizeConfiguration({classpath: ['lib'], classOutputPaths: ['build/classes'], resourcePaths: ['src/main/resources']});
        assert.deepStrictEqual(configurationClassPath(configuration, '/tmp/project'), [
            '/tmp/project/lib', '/tmp/project/build/classes', '/tmp/project/src/main/resources'
        ]);
    });

    test('prefers MainWindow classes referenced by TotalCrossApplication.run', () => {
        const candidates = mainWindowCandidates([
            {file: '/tmp/App.java', text: 'package demo; class App extends MainWindow {}'},
            {file: '/tmp/Run.java', text: 'class Run { void go() { TotalCrossApplication.run(App.class); } }'}
        ]);
        assert.deepStrictEqual(candidates, [{className: 'demo.App', file: '/tmp/App.java', preferred: true}]);
    });
});
