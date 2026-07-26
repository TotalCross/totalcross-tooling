// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Runs external tools with captured streams, timeouts, and forced cleanup. */
public class ProcessRunner {
    public ProcessResult run(ProcessRequest request) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(request.command());
        if (request.workingDirectory() != null) builder.directory(request.workingDirectory().toFile());
        builder.environment().putAll(request.environment());
        Process process = builder.start();
        byte[] stdout;
        byte[] stderr;
        boolean finished = process.waitFor(request.timeout().toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroy();
            if (!process.waitFor(1, TimeUnit.SECONDS)) process.destroyForcibly();
            stdout = readQuietly(process.getInputStream());
            stderr = readQuietly(process.getErrorStream());
            return new ProcessResult(-1, text(stdout), text(stderr), true);
        }
        stdout = process.getInputStream().readAllBytes();
        stderr = process.getErrorStream().readAllBytes();
        return new ProcessResult(process.exitValue(), text(stdout), text(stderr), false);
    }

    private static String text(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] readQuietly(java.io.InputStream stream) {
        try {
            return stream.readAllBytes();
        } catch (IOException closed) {
            return new byte[0];
        }
    }
}
