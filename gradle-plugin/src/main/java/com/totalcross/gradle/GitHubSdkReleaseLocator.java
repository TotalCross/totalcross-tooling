/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Optional;

/** Checks for a versioned TotalCross release before its archive is downloaded. */
final class GitHubSdkReleaseLocator implements SdkReleaseLocator {
    private static final String API_PREFIX = "https://api.github.com/repos/TotalCross/totalcross/releases/tags/v";
    private static final String DOWNLOAD_PREFIX = "https://github.com/TotalCross/totalcross/releases/download/v";

    @Override
    public Optional<URI> findGitHubArchive(String version) throws IOException {
        URI release = URI.create(API_PREFIX + version);
        HttpURLConnection connection = (HttpURLConnection) release.toURL().openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "totalcross-gradle-plugin");
        try {
            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_NOT_FOUND) return Optional.empty();
            if (status < 200 || status >= 300) {
                throw new IOException("GitHub release lookup failed for " + version + ": HTTP " + status);
            }
            try (var ignored = connection.getInputStream()) {
                return Optional.of(githubArchiveUri(version));
            }
        } finally {
            connection.disconnect();
        }
    }

    static URI githubArchiveUri(String version) {
        return URI.create(DOWNLOAD_PREFIX + version + "/TotalCross-" + version + ".zip");
    }
}
