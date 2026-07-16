/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

import * as vscode from 'vscode';

exports.user = function(value: string) {
        if(!value) {
            return 'user is required!';
        }
        return null;
};
