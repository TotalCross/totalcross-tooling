/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import com.totalcross.tooling.build.*;
import java.nio.file.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;

@Mojo(name = "preview")
public final class TotalCrossPreviewMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.basedir}")
    private String projectDirectory = ".";
    @Parameter(defaultValue = "${project.build.directory}")
    private String buildDirectory = "target";

    @Override public void execute() throws MojoExecutionException {
        try {
            Path project = Paths.get(projectDirectory == null ? "." : projectDirectory).toAbsolutePath().normalize();
            Path descriptor = Paths.get(buildDirectory == null ? "target" : buildDirectory,
                "totalcross", "preview-session.json").toAbsolutePath().normalize();
            Files.createDirectories(descriptor.getParent());
            Files.write(descriptor, new PreviewSessionDescriptor(1, BuildTool.MAVEN, project, descriptor,
                project.getFileName().toString(), null).toJson().getBytes("UTF-8"));
            getLog().info("TotalCross preview session ready: " + descriptor);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create TotalCross preview session", e);
        }
    }
}
