/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/** Locates a TotalCross SDK archive published by a GitHub release. */
@FunctionalInterface
interface SdkReleaseLocator {
    Optional<URI> findGitHubArchive(String version) throws IOException;
}
