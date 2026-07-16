/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("network")
class SdkSourceNetworkTest {
    private final SdkReleaseLocator locator = new GitHubSdkReleaseLocator();

    @Test
    void locatesRelease720AndStartsPartialDownloadFromGitHub() throws Exception {
        URI archive = locator.findGitHubArchive("7.2.0").orElseThrow();

        assertEquals(GitHubSdkReleaseLocator.githubArchiveUri("7.2.0"), archive);
        assertPartialDownload(archive);
    }

    @Test
    void fallsBackToS3ForRelease584ThatDoesNotExistOnGitHub() throws Exception {
        assertTrue(locator.findGitHubArchive("5.8.4").isEmpty());

        assertPartialDownload(SdkResolver.s3ArchiveUri("5.8.4"));
    }

    private static void assertPartialDownload(URI source) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Range", "bytes=0-0");
        try {
            int status = connection.getResponseCode();
            assertTrue(status >= 200 && status < 300, () -> "Expected successful partial response from " + source + ", got HTTP " + status);
            try (InputStream input = connection.getInputStream()) {
                assertNotEquals(-1, input.read(), "The archive response must contain at least one byte");
            }
        } finally {
            connection.disconnect();
        }
    }
}
