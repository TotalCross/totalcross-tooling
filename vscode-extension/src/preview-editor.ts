/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */

import {promises as fs} from 'fs';
import * as path from 'path';

export interface PreviewSelection {
    className: string;
    selected: boolean;
    error?: string;
}
export interface PreviewEditorModel {
    project: string;
    classOutput: string;
}

export function isJavaFile(file: string): boolean { return file.toLowerCase().endsWith('.java'); }

export function packageName(source: string): string {
    const match = /(?:^|\n)\s*package\s+([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*)\s*;/m.exec(source);
    return match?.[1] ?? '';
}

export function topLevelClassName(source: string): string | undefined {
    // This deliberately handles the source shapes supported by the Java extension:
    // a top-level class, including public/abstract/final declarations.
    const match = /(?:^|\n)\s*(?:public\s+)?(?:(?:abstract|final|strictfp)\s+)*class\s+([A-Za-z_$][\w$]*)\b/m.exec(source);
    return match?.[1];
}

export function sourceClassName(source: string, fileName: string): string | undefined {
    const simple = topLevelClassName(source) ?? path.basename(fileName, path.extname(fileName));
    if (!/^[A-Za-z_$][\w$]*$/.test(simple)) return undefined;
    const pkg = packageName(source);
    return pkg ? `${pkg}.${simple}` : simple;
}

export function isWorkspaceFile(file: string, workspaceRoot: string): boolean {
    const root = path.resolve(workspaceRoot);
    const candidate = path.resolve(file);
    const relative = path.relative(root, candidate);
    return relative === '' || (relative !== '..' && !relative.startsWith(`..${path.sep}`) && !path.isAbsolute(relative));
}

export function classFilePath(className: string, classOutput: string): string {
    return path.join(classOutput, ...className.split('.')) + '.class';
}

export async function compiledClassExists(className: string, classOutput: string): Promise<boolean> {
    try {
        await fs.access(classFilePath(className, classOutput));
        return true;
    } catch (_) {
        return false;
    }
}

export function parseSelection(value: string): PreviewSelection | undefined {
    try {
        const parsed = JSON.parse(value) as Partial<PreviewSelection>;
        if (typeof parsed.className !== 'string' || typeof parsed.selected !== 'boolean') return undefined;
        return {className: parsed.className, selected: parsed.selected, error: typeof parsed.error === 'string' ? parsed.error : undefined};
    } catch (_) {
        return undefined;
    }
}
