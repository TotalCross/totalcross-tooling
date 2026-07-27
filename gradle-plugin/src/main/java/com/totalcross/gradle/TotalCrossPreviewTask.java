/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
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
        getOutputs().upToDateWhen(task -> false);
        applicationClass = getProject().getObjects().property(String.class);
        applicationClass.convention(getProject().getName());
    }
    @Input public Property<String> getApplicationClass() { return applicationClass; }

    @TaskAction public void startPreview() throws IOException {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path session = getSessionFile().get().getAsFile().toPath();
        String resolvedApplicationClass = discoverApplicationClass(project.resolve("build/classes/java/main"), applicationClass.get());
        Files.createDirectories(session.getParent());
        Files.writeString(session, new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, session,
            resolvedApplicationClass, null).toJson());
        getLogger().lifecycle("TotalCross preview session ready: {}", session);
        if (!Boolean.parseBoolean(String.valueOf(getProject().findProperty("totalcross.preview.noLaunch")))) {
            launchDesktopPreview(resolvedApplicationClass);
        }
    }

    static String discoverApplicationClass(Path output, String simpleName) throws IOException {
        if (simpleName == null || simpleName.trim().isEmpty() || simpleName.contains(".") || !Files.isDirectory(output)) {
            return simpleName;
        }
        String classFile = simpleName + ".class";
        try (Stream<Path> paths = Files.walk(output)) {
            List<String> candidates = paths.filter(Files::isRegularFile)
                .filter(path -> classFile.equals(path.getFileName().toString()))
                .map(output::relativize)
                .map(path -> path.toString().replace(File.separatorChar, '.'))
                .map(path -> path.substring(0, path.length() - ".class".length()))
                .collect(Collectors.toList());
            if (candidates.size() > 1) {
                throw new IOException("More than one compiled application class named " + simpleName + " was found: " + candidates);
            }
            if (!candidates.isEmpty()) {
                return candidates.get(0);
            }
        }
        return simpleName;
    }

    private void launchDesktopPreview(String resolvedApplicationClass) throws IOException {
        var mainSourceSet = getProject().getExtensions().getByType(SourceSetContainer.class)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var classpath = mainSourceSet.getRuntimeClasspath().getFiles();
        if (!containsLauncher(classpath)) {
            getLogger().lifecycle("TotalCross SDK launcher not found; descriptor-only preview session");
            return;
        }
        String separator = java.io.File.pathSeparator;
        String value = classpath.stream().map(java.io.File::getAbsolutePath).collect(Collectors.joining(separator));
        String java = Paths.get(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        Process process = new ProcessBuilder(java, "-cp", value,
            "totalcross.Launcher", resolvedApplicationClass)
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
