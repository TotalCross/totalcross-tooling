/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import com.totalcross.tooling.build.*;
import java.nio.file.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.jar.JarFile;

@Mojo(name = "preview")
public final class TotalCrossPreviewMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.basedir}")
    private String projectDirectory = ".";
    @Parameter(defaultValue = "${project.build.directory}")
    private String buildDirectory = "target";
    @Parameter(property = "totalcross.mainClass", defaultValue = "${project.artifactId}")
    private String mainClass;
    @Parameter(property = "totalcross.preview.noLaunch", defaultValue = "false")
    private boolean noLaunch;
    @Component
    private MavenProject mavenProject;

    @Override public void execute() throws MojoExecutionException {
        try {
            Path project = Paths.get(projectDirectory == null ? "." : projectDirectory).toAbsolutePath().normalize();
            Path descriptor = Paths.get(buildDirectory == null ? "target" : buildDirectory,
                "totalcross", "preview-session.json").toAbsolutePath().normalize();
            Files.createDirectories(descriptor.getParent());
            String applicationClass = mainClass == null || mainClass.trim().isEmpty()
                ? project.getFileName().toString() : mainClass;
            Files.write(descriptor, new PreviewSessionDescriptor(1, BuildTool.MAVEN, project, descriptor,
                applicationClass, null).toJson().getBytes("UTF-8"));
            getLog().info("TotalCross preview session ready: " + descriptor);
            if (!noLaunch) launchDesktopPreview(applicationClass, project);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create TotalCross preview session", e);
        }
    }

    private void launchDesktopPreview(String applicationClass, Path project) throws Exception {
        List<String> classpath = mavenProject.getRuntimeClasspathElements();
        if (!containsLauncher(classpath)) {
            getLog().info("TotalCross SDK launcher not found; descriptor-only preview session");
            return;
        }
        String java = Paths.get(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        Process process = new ProcessBuilder(java, "-cp", String.join(File.pathSeparator, classpath),
            "totalcross.Launcher", applicationClass).directory(project.toFile()).inheritIO().start();
        getLog().info("TotalCross preview window started with PID " + process.pid());
    }

    private static boolean containsLauncher(List<String> entries) {
        for (String entry : entries) {
            File file = new File(entry);
            if (file.isDirectory() && new File(file, "totalcross/Launcher.class").isFile()) return true;
            if (file.isFile()) {
                try (JarFile jar = new JarFile(file)) {
                    if (jar.getEntry("totalcross/Launcher.class") != null) return true;
                } catch (Exception ignored) { }
            }
        }
        return false;
    }
}
