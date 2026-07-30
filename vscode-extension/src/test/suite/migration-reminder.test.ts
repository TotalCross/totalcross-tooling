/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as assert from 'assert';
import {CONVERT_NOW, DONT_ASK_AGAIN, handleMigrationReminderResponse} from '../../migration/migration-reminder';
import {clearMigrationReminder, migrationReminderKey, shouldShowMigrationReminder} from '../../migration/reminder-state';

suite('Maven to Gradle migration reminder', () => {
    test('routes Convert Now to the specific workspace folder', async () => {
        let executed: any[] | undefined;
        const folder: any = {uri: {toString: () => 'file:///project'}};
        const context: any = {workspaceState: {get: () => undefined, update: () => Promise.resolve()}};
        await handleMigrationReminderResponse(context, folder, CONVERT_NOW, 'com.example:project', 10, (command: string, uri: any) => {
            executed = [command, uri];
            return Promise.resolve();
        });
        assert.deepEqual(executed, ['extension.convertMavenProjectToGradle', folder.uri]);
    });

    test('postpones dismissal and the visible reminder action identically', async () => {
        const values: {[key: string]: any} = {};
        const context: any = {workspaceState: {
            get: (key: string) => values[key],
            update: (key: string, value: any) => { values[key] = value; return Promise.resolve(); }
        }};
        const folder: any = {uri: {toString: () => 'file:///project'}};
        await handleMigrationReminderResponse(context, folder, undefined, 'com.example:project', 99, () => Promise.resolve());
        assert.equal(values[migrationReminderKey('file:///project', 'com.example:project')], 99 + 86400000);
    });

    test('suppresses and re-enables only the selected project identity and folder', async () => {
        const values: {[key: string]: any} = {};
        const context: any = {workspaceState: {
            get: (key: string) => values[key],
            update: (key: string, value: any) => { values[key] = value; return Promise.resolve(); }
        }};
        const folder: any = {uri: {toString: () => 'file:///one/'}};
        const identity = 'com.example:project';
        await handleMigrationReminderResponse(context, folder, DONT_ASK_AGAIN, identity, 10, () => Promise.resolve());
        assert.equal(shouldShowMigrationReminder(context, 'file:///one', identity, Number.MAX_SAFE_INTEGER - 1), false);
        assert.equal(shouldShowMigrationReminder(context, 'file:///two', identity, 10), true);
        assert.equal(shouldShowMigrationReminder(context, 'file:///one', 'com.example:other', 10), true);
        await clearMigrationReminder(context, 'file:///one', identity);
        assert.equal(shouldShowMigrationReminder(context, 'file:///one', identity, 10), true);
    });
});
