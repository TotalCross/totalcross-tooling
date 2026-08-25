/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import com.totalcross.tooling.preview.PreviewProcessTerminator;
import java.nio.file.Path;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "preview-stop")
public final class TotalCrossPreviewStopMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.directory}")
    private String buildDirectory = "target";

    @Override public void execute() throws MojoExecutionException {
        try {
            Path descriptor = Path.of(buildDirectory, "totalcross", "preview-session.json");
            if (PreviewProcessTerminator.stop(descriptor)) getLog().info("TotalCross preview session stopped");
        } catch (Exception failure) {
            throw new MojoExecutionException("Unable to stop TotalCross preview", failure);
        }
    }
}
