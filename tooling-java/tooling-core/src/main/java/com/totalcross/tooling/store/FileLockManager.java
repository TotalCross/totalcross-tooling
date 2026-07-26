// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Coordinates installers with a lock file beside the target coordinate. */
public final class FileLockManager {
    public Lock acquire(Path lockPath) throws IOException {
        Files.createDirectories(lockPath.toAbsolutePath().normalize().getParent());
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            while (true) {
                try {
                    FileLock lock = channel.tryLock();
                    if (lock != null) return new LockHandle(channel, lock);
                } catch (OverlappingFileLockException ignored) {
                    // Another installer in this JVM owns the coordinate lock.
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for " + lockPath, interrupted);
                }
            }
        } catch (IOException | RuntimeException failure) {
            channel.close();
            throw failure;
        }
    }

    public interface Lock extends AutoCloseable {
        @Override void close() throws IOException;
    }

    private record LockHandle(FileChannel channel, FileLock fileLock) implements Lock {
        @Override public void close() throws IOException {
            fileLock.release();
            channel.close();
        }
    }

}
