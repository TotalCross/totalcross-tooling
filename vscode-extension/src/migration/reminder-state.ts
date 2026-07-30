/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

export const MIGRATION_REMINDER_DELAY_MS = 24 * 60 * 60 * 1000;
const REMINDER_PREFIX = 'totalcross.mavenToGradleReminder';

interface WorkspaceState {
    get<T>(section: string): T | undefined;
    update(section: string, value: any): Thenable<void>;
}

interface ReminderContext {
    workspaceState: WorkspaceState;
}

/**
 * Combines a normalized workspace URI with the Maven project coordinate. The
 * coordinate keeps the preference tied to the selected project rather than to
 * a generic folder name, while the URI keeps equivalent projects in separate
 * workspace folders isolated.
 */
export function migrationReminderKey(folderUri: string, projectIdentity = 'legacy'): string {
    const normalizedUri = folderUri.replace(/\/+$/, '');
    return `${REMINDER_PREFIX}.${encodeURIComponent(normalizedUri)}.${encodeURIComponent(projectIdentity)}`;
}

export function shouldShowMigrationReminder(context: ReminderContext, folderUri: string, projectIdentity: string, now: number): boolean {
    const deadline = context.workspaceState.get<number>(migrationReminderKey(folderUri, projectIdentity));
    return deadline === undefined || now >= deadline;
}

export function postponeMigrationReminder(context: ReminderContext, folderUri: string, projectIdentity: string, now: number): Thenable<void> {
    return context.workspaceState.update(migrationReminderKey(folderUri, projectIdentity), now + MIGRATION_REMINDER_DELAY_MS);
}

export function suppressMigrationReminder(context: ReminderContext, folderUri: string, projectIdentity: string): Thenable<void> {
    return context.workspaceState.update(migrationReminderKey(folderUri, projectIdentity), Number.MAX_SAFE_INTEGER);
}

export function clearMigrationReminder(context: ReminderContext, folderUri: string, projectIdentity = 'legacy'): Thenable<void> {
    return context.workspaceState.update(migrationReminderKey(folderUri, projectIdentity), undefined);
}
