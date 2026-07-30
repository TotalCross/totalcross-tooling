/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import {promises as fs} from 'fs';
import * as path from 'path';

const xmlParser = require('xml-js');

const GRADLE_BUILD_FILES = ['build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts'];

export type ProjectClassification =
    | {kind: 'not-maven'}
    | {kind: 'gradle-present'}
    | {kind: 'non-totalcross-maven'}
    | {kind: 'eligible'; pomPath: string; projectIdentity: string}
    | {kind: 'invalid-pom'; error: Error};

function asArray(value: any): any[] {
    return value === undefined || value === null ? [] : Array.isArray(value) ? value : [value];
}

function text(value: any): string | undefined {
    return value && typeof value._text === 'string' ? value._text.trim() : undefined;
}

function hasCoordinates(entries: any[], groupId: string, artifactId: string): boolean {
    return entries.some((entry) => text(entry && entry.groupId) === groupId && text(entry && entry.artifactId) === artifactId);
}

/** Parses enough Maven XML to distinguish a TotalCross dependency or build plugin. */
export function isTotalCrossPom(contents: string): boolean {
    const document = JSON.parse(xmlParser.xml2json(contents, {compact: true}));
    const project = document && document.project;
    if (!project) {
        return false;
    }
    const dependencies = asArray(project.dependencies && project.dependencies.dependency);
    const plugins = asArray(project.build && project.build.plugins && project.build.plugins.plugin);
    return hasCoordinates(dependencies, 'com.totalcross', 'totalcross-sdk')
        || hasCoordinates(plugins, 'com.totalcross', 'totalcross-maven-plugin');
}

/** A Maven coordinate is stable project evidence without writing project settings. */
export function mavenProjectIdentity(contents: string): string {
    const document = JSON.parse(xmlParser.xml2json(contents, {compact: true}));
    const project = document && document.project;
    const parent = project && project.parent;
    const groupId = text(project && project.groupId) || text(parent && parent.groupId) || 'unknown-group';
    const artifactId = text(project && project.artifactId) || 'unknown-artifact';
    return `${groupId}:${artifactId}`;
}

/** Inspects only root-level build files; nested Maven modules never affect this result. */
export async function classifyProject(rootPath: string): Promise<ProjectClassification> {
    const pomPath = path.join(rootPath, 'pom.xml');
    try {
        await fs.access(pomPath);
    } catch (_) {
        return {kind: 'not-maven'};
    }

    for (const file of GRADLE_BUILD_FILES) {
        try {
            await fs.access(path.join(rootPath, file));
            return {kind: 'gradle-present'};
        } catch (_) {
            // Test the remaining root build files.
        }
    }

    try {
        const contents = await fs.readFile(pomPath, 'utf8');
        return isTotalCrossPom(contents)
            ? {kind: 'eligible', pomPath, projectIdentity: mavenProjectIdentity(contents)}
            : {kind: 'non-totalcross-maven'};
    } catch (error) {
        return {kind: 'invalid-pom', error: error instanceof Error ? error : new Error(String(error))};
    }
}
