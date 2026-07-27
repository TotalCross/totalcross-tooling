/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import {livePreviewTest} from '../../live-preview';

suite('Live Preview Test Suite', () => {
	test('normalizes the initial preview orientation', () => {
		const landscape = livePreviewTest.defaultConfigFromValues(360, 592, 'landscape', 'android');
		const portrait = livePreviewTest.defaultConfigFromValues(592, 360, 'portrait', 'android');

		assert.deepEqual([landscape.width, landscape.height], [592, 360]);
		assert.deepEqual([portrait.width, portrait.height], [360, 592]);
		assert.deepEqual(landscape.launcherArgs, ['width', '592', 'height', '360']);
	});

	test('extracts Java type names and compiled class paths', () => {
		assert.equal(livePreviewTest.packageName('package com.example.preview;\nclass Screen {}'), 'com.example.preview');
		assert.equal(livePreviewTest.topLevelClassName('public final class Screen extends MainWindow {}'), 'Screen');
		assert.equal(
			livePreviewTest.relativeClassPath('/tmp/classes', '/tmp/classes/com/example/Screen$Inner.class'),
			'com.example.Screen'
		);
		assert.equal(livePreviewTest.relativeClassPath('/tmp/classes', '/tmp/other/Screen.class'), undefined);
	});

	test('builds shell-free Java arguments and adds the macOS UI element setting', () => {
		const jvmArgs = livePreviewTest.previewJvmArgs({get: () => []} as any, 'darwin');
		const args = livePreviewTest.buildJavaArguments(
			jvmArgs,
			'/tmp/SDK With Spaces/totalcross-sdk.jar',
			'/tmp/workspace with spaces/totalcross.preview.json',
			41234
		);

		assert.equal(jvmArgs[0], '-Dapple.awt.UIElement=true');
		assert.ok(args.includes('/tmp/SDK With Spaces/totalcross-sdk.jar'));
		assert.ok(args.includes('/tmp/workspace with spaces/totalcross.preview.json'));
		assert.equal(args[args.indexOf('--host') + 1], '127.0.0.1');
		assert.equal(args[args.indexOf('--port') + 1], '41234');
	});
});
