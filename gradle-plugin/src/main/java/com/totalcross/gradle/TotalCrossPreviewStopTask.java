/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.preview.PreviewProcessTerminator;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TotalCrossPreviewStopTask extends DefaultTask {
    @TaskAction public void stopPreview() throws java.io.IOException {
        var file = getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json").get().getAsFile().toPath();
        if (PreviewProcessTerminator.stop(file)) getLogger().lifecycle("TotalCross preview session stopped");
    }
}
