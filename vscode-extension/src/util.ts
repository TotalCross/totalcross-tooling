/*
 * Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

const fs = require('fs-extra');
const envPaths = require('env-paths');
const paths = envPaths('TotalCross', {suffix: null});


export function getTotalCrossConfigHomePath() : string{
    return paths.config;
}
