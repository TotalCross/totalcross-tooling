// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.download;

import com.totalcross.tooling.store.ChecksumVerifier;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Downloads to a temporary sibling, verifies the digest, and atomically promotes the file. */
public final class HttpDownloader implements Downloader {
    @Override
    public void download(DownloadRequest request) throws IOException {
        Files.createDirectories(request.destination().toAbsolutePath().normalize().getParent());
        Path part = request.destination().resolveSibling(request.destination().getFileName() + ".part");
        Files.deleteIfExists(part);
        HttpURLConnection connection = (HttpURLConnection) request.source().toURL().openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);
        connection.connect();
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new IOException("Download failed: HTTP " + connection.getResponseCode());
        }
        try (var input = connection.getInputStream()) {
            Files.copy(input, part, StandardCopyOption.REPLACE_EXISTING);
            ChecksumVerifier.verify(part, request.sha256());
            Files.move(part, request.destination(), StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(part);
            connection.disconnect();
        }
    }
}
