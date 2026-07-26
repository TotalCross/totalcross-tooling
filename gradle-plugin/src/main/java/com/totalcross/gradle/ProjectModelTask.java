/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;

public abstract class ProjectModelTask extends DefaultTask {
    public ProjectModelTask() { getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/project-model.json")); }
    @OutputFile public abstract RegularFileProperty getOutputFile();

    @TaskAction public void writeModel() throws IOException {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path descriptor = getOutputFile().get().getAsFile().toPath();
        ProjectModel model = new ProjectModel(BuildTool.GRADLE, project,
            ListRoot.of(project.resolve("src/main/java")), ListResource.of(project.resolve("src/main/resources")),
            new ClassOutput(project.resolve("build/classes/java/main")), new DependencyClasspath(java.util.List.of()),
            new JavaCompatibilityPolicy(Runtime.version().feature(), 17, 8), new RetrolambdaPlan(false, "modern default"),
            new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, descriptor, getProject().getName(), null));
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, model.preview().toJson());
        getLogger().lifecycle("TotalCross project model: {}", descriptor);
    }

    private static final class ListRoot {
        static java.util.List<SourceRoot> of(Path path) { return java.util.List.of(new SourceRoot(path)); }
    }
    private static final class ListResource {
        static java.util.List<ResourceRoot> of(Path path) { return java.util.List.of(new ResourceRoot(path)); }
    }
}
