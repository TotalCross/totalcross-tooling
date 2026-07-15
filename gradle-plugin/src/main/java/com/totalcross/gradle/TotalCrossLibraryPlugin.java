/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

/** Registers TotalCross packaging with library output enabled by default. */
public final class TotalCrossLibraryPlugin extends TotalCrossPlugin {
    @Override
    protected boolean isLibraryPlugin() {
        return true;
    }
}
