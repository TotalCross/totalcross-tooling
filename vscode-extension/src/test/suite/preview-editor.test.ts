/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import * as assert from 'assert';
import {parseSelection, sourceClassName, isWorkspaceFile, classFilePath} from '../../preview-editor';

suite('Preview editor integration', () => {
    test('extracts a packaged top-level class name', () => {
        assert.strictEqual(sourceClassName('package com.example;\npublic final class ButtonView {}', '/tmp/ButtonView.java'), 'com.example.ButtonView');
    });

    test('falls back to the Java filename for a package-free source', () => {
        assert.strictEqual(sourceClassName('class MainWindow {}', '/tmp/MainWindow.java'), 'MainWindow');
    });

    test('keeps editor paths inside the selected workspace', () => {
        assert.strictEqual(isWorkspaceFile('/tmp/project/src/App.java', '/tmp/project'), true);
        assert.strictEqual(isWorkspaceFile('/tmp/project-other/src/App.java', '/tmp/project'), false);
    });

    test('resolves compiled class paths with nested packages', () => {
        assert.strictEqual(classFilePath('com.example.ButtonView', '/tmp/project/build/classes'), '/tmp/project/build/classes/com/example/ButtonView.class');
    });

    test('accepts only a class-scoped selection marker', () => {
        assert.deepStrictEqual(parseSelection('{"className":"com.example.ButtonView","selected":true}'), {
            className: 'com.example.ButtonView', selected: true, error: undefined
        });
        assert.strictEqual(parseSelection('{"selected":true}'), undefined);
        const stale = parseSelection('{"className":"other.ButtonView","selected":true}');
        assert.ok(stale);
        assert.notStrictEqual(stale?.className, 'com.example.ButtonView');
    });
});
