/*
 * Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

export function validateLogin(value: string) {
        if(!value) {
            return 'Login cannot be empty!';
        }
        return null;
}

export function validatePassword(value: string) {
    if(!value) {
        return 'Password cannot be empty!';
    }
    return null;
}
