/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const {execFileSync} = require('child_process');

const archive = process.argv[2];
if (!archive) {
    throw new Error('Usage: node tools/verify-vsix.js <extension.vsix>');
}

const entries = new Set(execFileSync('unzip', ['-Z1', archive], {encoding: 'utf8'}).trim().split('\n'));
const required = [
    'extension/package.json',
    'extension/out/extension.js',
    'extension/node_modules/env-paths/package.json',
    'extension/node_modules/fs-extra/package.json',
    'extension/node_modules/jsonfile/package.json',
    'extension/node_modules/node-ssh/package.json',
    'extension/node_modules/xml-js/package.json'
];
const excluded = ['extension/src/', 'extension/out/test/', 'extension/.vscode-test/'];
const missing = required.filter((entry) => !entries.has(entry));
const unexpected = excluded.filter((prefix) => Array.from(entries).some((entry) => entry.startsWith(prefix)));
if (missing.length || unexpected.length) {
    throw new Error(`Invalid VSIX contents: missing=${missing.join(',') || 'none'} unexpected=${unexpected.join(',') || 'none'}`);
}
console.log(`VSIX verified: ${required.length} runtime entries and no development sources.`);
