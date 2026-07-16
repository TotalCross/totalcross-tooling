/*
 * SPDX-FileCopyrightText: 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Serializes downloads that populate one shared Gradle cache directory. */
final class CacheLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private CacheLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    static CacheLock acquire(Path directory) throws IOException {
        Files.createDirectories(directory);
        FileChannel channel = FileChannel.open(directory.resolve(".download.lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            return new CacheLock(channel, channel.lock());
        } catch (IOException failure) {
            channel.close();
            throw failure;
        }
    }

    @Override
    public void close() throws IOException {
        lock.release();
        channel.close();
    }
}
