/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.file.RegularFileProperty;

public abstract class ProjectModelTask extends DefaultTask {
    public ProjectModelTask() { getOutputFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/project-model.json")); }
    @OutputFile public abstract RegularFileProperty getOutputFile();

    @TaskAction public void writeModel() throws IOException {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path descriptor = getOutputFile().get().getAsFile().toPath();
        SourceSetContainer sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Path classes = main.getOutput().getClassesDirs().getSingleFile().toPath();
        String mainClass = TotalCrossPreviewTask.discoverApplicationClass(classes, getProject().getName());
        List<Path> dependencies = main.getRuntimeClasspath().getFiles().stream().map(java.io.File::toPath).toList();
        JavaPluginExtension javaExtension = getProject().getExtensions().findByType(JavaPluginExtension.class);
        int target = javaExtension == null ? 17 : Integer.parseInt(javaExtension.getTargetCompatibility().getMajorVersion());
        ProjectModel model = new ProjectModel(BuildTool.GRADLE, project,
            main.getAllJava().getSrcDirs().stream().map(java.io.File::toPath).map(SourceRoot::new).toList(),
            main.getResources().getSrcDirs().stream().map(java.io.File::toPath).map(ResourceRoot::new).toList(),
            new ClassOutput(classes), new DependencyClasspath(dependencies),
            new JavaCompatibilityPolicy(Runtime.version().feature(), 17, target), new RetrolambdaPlan(false, "modern default"),
            new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, descriptor, mainClass, null));
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, model.preview().toJson());
        getLogger().lifecycle("TotalCross project model: {}", descriptor);
    }

}
