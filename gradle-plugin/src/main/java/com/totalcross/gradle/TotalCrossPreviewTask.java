/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import java.io.IOException;
import java.nio.file.*;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.*;

public abstract class TotalCrossPreviewTask extends DefaultTask {
    public TotalCrossPreviewTask() {
        getSessionFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json"));
        getWatchedInputs().from(getProject().fileTree("src/main"));
    }
    @OutputFile public abstract RegularFileProperty getSessionFile();
    @InputFiles public abstract ConfigurableFileCollection getWatchedInputs();

    @TaskAction public void startPreview() throws IOException {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path session = getSessionFile().get().getAsFile().toPath();
        Files.createDirectories(session.getParent());
        Files.writeString(session, new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, session,
            getProject().getName(), null).toJson());
        getLogger().lifecycle("TotalCross preview session ready: {}", session);
    }
}
