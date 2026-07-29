/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
import {mavenExecutable, previewCommand} from '../../preview-client';
import {ProjectLayout} from '../../project-layout';

suite('Preview client', () => {
    const layout: ProjectLayout = {buildTool: 'gradle', root: '/tmp/app', packageCommand: '', packageOutputRoot: '', linuxArmInstallDirectory: ''};

    test('uses wrapper without a shell', () => {
        const command = previewCommand(layout, 'darwin');
        assert.strictEqual(command.executable, './gradlew');
        assert.deepStrictEqual(command.args, ['totalcrossPreview', '--console=plain']);
    });

    test('compiles Maven projects before opening preview', () => {
        const command = previewCommand({...layout, buildTool: 'maven'}, 'darwin');
        assert.strictEqual(command.executable, 'mvn');
        assert.deepStrictEqual(command.args, ['compile', 'totalcross:preview']);
    });

    test('prefers Maven Wrapper when it is present', () => {
        assert.strictEqual(mavenExecutable('/project', 'darwin', () => true), './mvnw');
        assert.strictEqual(mavenExecutable('/project', 'win32', () => true), '.\\mvnw.cmd');
        assert.strictEqual(mavenExecutable('/project', 'darwin', () => false), 'mvn');
    });
});
