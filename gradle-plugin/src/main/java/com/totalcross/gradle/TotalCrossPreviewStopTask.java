/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import java.nio.file.Files;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TotalCrossPreviewStopTask extends DefaultTask {
    @TaskAction public void stopPreview() throws java.io.IOException {
        var file = getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json").get().getAsFile().toPath();
        if (Files.deleteIfExists(file)) getLogger().lifecycle("TotalCross preview session stopped");
    }
}
