/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TotalCrossRunTask extends DefaultTask {
    @TaskAction public void runApplication() {
        Path descriptor = getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json").get().getAsFile().toPath();
        try {
            Matcher matcher = Pattern.compile("\\\"pid\\\"\\s*:\\s*(\\d+)").matcher(Files.readString(descriptor));
            if (!matcher.find()) {
                getLogger().lifecycle("TotalCross run uses the external preview host after application classes are compiled");
                return;
            }
            long pid = Long.parseLong(matcher.group(1));
            if (!ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                throw new IllegalStateException("preview coordinator is not running: " + pid);
            }
            getLogger().lifecycle("TotalCross run uses the external preview host (PID {})", pid);
        } catch (java.io.IOException e) {
            throw new org.gradle.api.GradleException("Unable to inspect TotalCross preview session", e);
        }
    }
}
