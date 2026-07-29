/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

'use strict';

const {execFileSync} = require('child_process');
const fs = require('fs/promises');
const os = require('os');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const output = path.resolve(ROOT, process.argv[2] || 'totalcross-preview.vsix');
const epoch = new Date('1980-01-01T00:00:00.000Z');

function xml(value) {
    return String(value).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

async function copy(source, target) {
    await fs.mkdir(path.dirname(target), {recursive: true});
    await fs.cp(source, target, {recursive: true, dereference: true});
}

async function present(file) {
    try { await fs.access(file); return true; } catch (_) { return false; }
}

async function copyRuntimePackage(name, sourceRoot, extensionRoot, copied) {
    const source = path.join(sourceRoot, 'node_modules', ...name.split('/'));
    if (!(await present(source))) {
        throw new Error(`Runtime dependency ${name} is missing below ${sourceRoot}`);
    }
    const target = path.join(extensionRoot, 'node_modules', ...name.split('/'));
    if (copied.has(target)) { return; }
    copied.add(target);
    await copy(source, target);
    const manifest = JSON.parse(await fs.readFile(path.join(source, 'package.json'), 'utf8'));
    const dependencies = {...manifest.dependencies, ...manifest.optionalDependencies};
    for (const dependency of Object.keys(dependencies).sort()) {
        if (await present(path.join(source, 'node_modules', ...dependency.split('/')))) {
            await copyRuntimePackage(dependency, source, extensionRoot, copied);
        } else if (await present(path.join(ROOT, 'node_modules', ...dependency.split('/')))) {
            await copyRuntimePackage(dependency, ROOT, extensionRoot, copied);
        }
    }
}

async function filesBelow(root, relative = '') {
    const entries = await fs.readdir(path.join(root, relative), {withFileTypes: true});
    const files = [];
    for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
        const child = path.join(relative, entry.name);
        if (entry.isDirectory()) { files.push(...await filesBelow(root, child)); }
        else if (entry.isFile()) { files.push(child); }
    }
    return files;
}

async function normalizeTimes(root) {
    for (const relative of await filesBelow(root)) {
        await fs.utimes(path.join(root, relative), epoch, epoch);
    }
}

function manifest(extension) {
    return `<?xml version="1.0" encoding="utf-8"?>\n<PackageManifest Version="2.0.0" xmlns="http://schemas.microsoft.com/developer/vsx-schema/2011">\n  <Metadata>\n    <Identity Language="en-US" Id="${xml(extension.name)}" Version="${xml(extension.version)}" Publisher="${xml(extension.publisher)}" />\n    <DisplayName>${xml(extension.displayName)}</DisplayName>\n    <Description xml:space="preserve">${xml(extension.description)}</Description>\n    <Categories>${xml((extension.categories || []).join(','))}</Categories>\n    <Properties>\n      <Property Id="Microsoft.VisualStudio.Code.Engine" Value="${xml(extension.engines.vscode)}" />\n      <Property Id="Microsoft.VisualStudio.Code.ExtensionDependencies" Value="${xml((extension.extensionDependencies || []).join(','))}" />\n      <Property Id="Microsoft.VisualStudio.Code.ExecutesCode" Value="true" />\n    </Properties>\n    <License>extension/LICENSE.txt</License>\n    <Icon>extension/icon.png</Icon>\n  </Metadata>\n  <Installation><InstallationTarget Id="Microsoft.VisualStudio.Code"/></Installation>\n  <Dependencies/>\n  <Assets>\n    <Asset Type="Microsoft.VisualStudio.Code.Manifest" Path="extension/package.json" Addressable="true" />\n    <Asset Type="Microsoft.VisualStudio.Services.Content.Details" Path="extension/readme.md" Addressable="true" />\n    <Asset Type="Microsoft.VisualStudio.Services.Content.Changelog" Path="extension/changelog.md" Addressable="true" />\n    <Asset Type="Microsoft.VisualStudio.Services.Content.License" Path="extension/LICENSE.txt" Addressable="true" />\n    <Asset Type="Microsoft.VisualStudio.Services.Icons.Default" Path="extension/icon.png" Addressable="true" />\n  </Assets>\n</PackageManifest>\n`;
}

async function main() {
    const extension = JSON.parse(await fs.readFile(path.join(ROOT, 'package.json'), 'utf8'));
    const staging = await fs.mkdtemp(path.join(os.tmpdir(), 'totalcross-vsix-'));
    try {
        const extensionRoot = path.join(staging, 'extension');
        for (const [source, target] of [
            ['package.json', 'package.json'], ['README.md', 'readme.md'], ['CHANGELOG.md', 'changelog.md'],
            ['LICENSE', 'LICENSE.txt'], ['NOTICE', 'NOTICE'], ['AUTHORS.md', 'AUTHORS.md'], ['icon.png', 'icon.png'],
            ['out', 'out'], ['resources', 'resources']
        ]) {
            await copy(path.join(ROOT, source), path.join(extensionRoot, target));
        }
        await fs.rm(path.join(extensionRoot, 'out', 'test'), {recursive: true, force: true});
        const copied = new Set();
        for (const dependency of Object.keys(extension.dependencies || {}).sort()) {
            await copyRuntimePackage(dependency, ROOT, extensionRoot, copied);
        }
        await fs.writeFile(path.join(staging, 'extension.vsixmanifest'), manifest(extension));
        await fs.writeFile(path.join(staging, '[Content_Types].xml'), '<?xml version="1.0" encoding="utf-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="json" ContentType="application/json"/><Default Extension="vsixmanifest" ContentType="text/xml"/><Default Extension="png" ContentType="image/png"/></Types>');
        await normalizeTimes(staging);
        await fs.rm(output, {force: true});
        const files = await filesBelow(staging);
        execFileSync('zip', ['-X', '-q', output, '-@'], {cwd: staging, input: `${files.join('\n')}\n`});
    } finally {
        await fs.rm(staging, {recursive: true, force: true});
    }
}

main().catch((error) => {
    console.error(error instanceof Error ? error.stack : String(error));
    process.exitCode = 1;
});
