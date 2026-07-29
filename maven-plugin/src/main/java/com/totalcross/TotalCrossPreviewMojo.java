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
import java.util.stream.Stream;
import com.totalcross.tooling.cli.ToolingCli;
import java.io.IOException;

@Mojo(name = "preview", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TotalCrossPreviewMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.basedir}")
    private String projectDirectory = ".";
    @Parameter(defaultValue = "${project.build.directory}")
    private String buildDirectory = "target";
    @Parameter(property = "totalcross.mainClass")
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
            String applicationClass = resolveApplicationClass(project);
            Files.write(descriptor, new PreviewSessionDescriptor(1, BuildTool.MAVEN, project, descriptor,
                applicationClass, null).toJson().getBytes("UTF-8"));
            getLog().info("TotalCross preview session ready: " + descriptor);
            if (!noLaunch) launchSharedPreview(applicationClass, project, descriptor);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to create TotalCross preview session", e);
        }
    }

    private String resolveApplicationClass(Path project) throws Exception {
        if (mainClass != null && !mainClass.trim().isEmpty()) return mainClass.trim();
        String fallback = mavenProject == null || mavenProject.getArtifactId() == null
            ? project.getFileName().toString() : mavenProject.getArtifactId();
        Path output = project.resolve("target/classes");
        if (mavenProject != null && mavenProject.getBuild() != null && mavenProject.getBuild().getOutputDirectory() != null) {
            output = Paths.get(mavenProject.getBuild().getOutputDirectory());
        }
        String discovered = discoverMainClass(output, fallback);
        if (discovered != null) {
            getLog().info("TotalCross main class discovered: " + discovered);
            return discovered;
        }
        return fallback;
    }

    static String discoverMainClass(Path output, String simpleName) throws Exception {
        if (output == null || simpleName == null || !Files.isDirectory(output)) return null;
        String classFile = simpleName + ".class";
        try (Stream<Path> paths = Files.walk(output)) {
            List<String> candidates = paths.filter(Files::isRegularFile)
                .filter(path -> classFile.equals(path.getFileName().toString()))
                .map(output::relativize)
                .map(path -> path.toString().replace(File.separatorChar, '.'))
                .map(path -> path.substring(0, path.length() - ".class".length()))
                .collect(java.util.stream.Collectors.toList());
            if (candidates.size() > 1) {
                throw new IllegalStateException("More than one compiled main class named " + simpleName + " was found: " + candidates);
            }
            return candidates.isEmpty() ? null : candidates.get(0);
        }
    }

    private void launchSharedPreview(String applicationClass, Path project, Path descriptor) throws Exception {
        List<String> classpath = mavenProject.getRuntimeClasspathElements();
        String java = Paths.get(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        String cli = ToolingCli.runtimeClasspath();
        Path frame = descriptor.resolveSibling("preview-frame.png");
        Path control = descriptor.resolveSibling("preview-control.txt");
        Process process = new ProcessBuilder(java, "-cp", cli, ToolingCli.class.getName(), "preview",
            "--project", project.toString(), "--main", applicationClass, "--classpath",
            String.join(File.pathSeparator, classpath), "--frame-file", frame.toString(), "--control-file", control.toString())
            .directory(project.toFile()).redirectErrorStream(true).start();
        String event = process.inputReader().readLine();
        if (event == null) throw new IOException("TotalCross preview coordinator exited before starting");
        Files.writeString(descriptor, Files.readString(descriptor).replaceFirst("}$",
            ",\"pid\":" + process.pid() + "}"));
        getLog().info("TotalCross preview coordinator started with PID " + process.pid() + ": " + event);
    }
}
