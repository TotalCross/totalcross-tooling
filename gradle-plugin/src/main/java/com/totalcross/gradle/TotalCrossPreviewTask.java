/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.jar.JarFile;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

public abstract class TotalCrossPreviewTask extends DefaultTask {
    @OutputFile public abstract RegularFileProperty getSessionFile();
    @InputFiles public abstract ConfigurableFileCollection getWatchedInputs();
    private final Property<String> applicationClass;

    public TotalCrossPreviewTask() {
        getSessionFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json"));
        getWatchedInputs().from(getProject().fileTree("src/main"));
        applicationClass = getProject().getObjects().property(String.class);
        applicationClass.convention(getProject().getName());
    }
    @Input public Property<String> getApplicationClass() { return applicationClass; }

    @TaskAction public void startPreview() throws IOException {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path session = getSessionFile().get().getAsFile().toPath();
        Files.createDirectories(session.getParent());
        Files.writeString(session, new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, session,
            applicationClass.get(), null).toJson());
        getLogger().lifecycle("TotalCross preview session ready: {}", session);
        if (!Boolean.parseBoolean(String.valueOf(getProject().findProperty("totalcross.preview.noLaunch")))) {
            launchDesktopPreview();
        }
    }

    private void launchDesktopPreview() throws IOException {
        var classpath = getProject().getConfigurations().getByName("runtimeClasspath").getFiles();
        if (!containsLauncher(classpath)) {
            getLogger().lifecycle("TotalCross SDK launcher not found; descriptor-only preview session");
            return;
        }
        String separator = java.io.File.pathSeparator;
        String value = classpath.stream().map(java.io.File::getAbsolutePath).collect(Collectors.joining(separator));
        String java = Paths.get(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        Process process = new ProcessBuilder(java, "-cp", value,
            "totalcross.Launcher", applicationClass.get())
            .directory(projectDirectory()).inheritIO().start();
        getLogger().lifecycle("TotalCross preview window started with PID {}", process.pid());
    }

    private java.io.File projectDirectory() { return getProject().getProjectDir(); }

    private static boolean containsLauncher(java.util.Set<java.io.File> files) {
        for (java.io.File file : files) {
            if (file.isDirectory() && Files.isRegularFile(file.toPath().resolve("totalcross/Launcher.class"))) return true;
            if (file.isFile()) {
                try (JarFile jar = new JarFile(file)) {
                    if (jar.getEntry("totalcross/Launcher.class") != null) return true;
                } catch (IOException ignored) { }
            }
        }
        return false;
    }
}
