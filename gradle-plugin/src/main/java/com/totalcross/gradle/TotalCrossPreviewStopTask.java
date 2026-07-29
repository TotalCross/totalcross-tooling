/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TotalCrossPreviewStopTask extends DefaultTask {
    @TaskAction public void stopPreview() throws java.io.IOException {
        var file = getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json").get().getAsFile().toPath();
        if (!Files.isRegularFile(file)) return;
        Matcher matcher = Pattern.compile("\"pid\"\\s*:\\s*(\\d+)").matcher(Files.readString(file));
        if (matcher.find()) {
            ProcessHandle.of(Long.parseLong(matcher.group(1))).ifPresent(process -> {
                process.descendants().forEach(ProcessHandle::destroy);
                process.destroy();
            });
        }
        if (Files.deleteIfExists(file)) getLogger().lifecycle("TotalCross preview session stopped");
    }
}
