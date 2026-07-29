/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import com.totalcross.tooling.build.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.*;
import com.totalcross.tooling.jdk.JdkCatalogResolver;
import com.totalcross.tooling.jdk.JdkInstallation;
import com.totalcross.tooling.jdk.JdkRequest;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import com.totalcross.tooling.cli.ToolingCli;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
    @Parameter(property = "totalcross.jdkPath")
    private String jdkPath;
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
        JdkInstallation toolingJdk = JdkCatalogResolver.production().resolve(
            new JdkRequest("17", jdkPath == null || jdkPath.isBlank() ? null : Paths.get(jdkPath), null));
        Path frame = descriptor.resolveSibling("preview-frame.png");
        Path control = descriptor.resolveSibling("preview-control.txt");
        Path log = descriptor.resolveSibling("preview.log");
        Files.deleteIfExists(frame);
        List<String> command = previewCommand(toolingJdk.home(), project, applicationClass,
            String.join(File.pathSeparator, classpath), frame, control);
        long pid = launchCoordinator(command, project, log);
        if (!awaitFirstFrame(pid, frame)) throw new IOException("TotalCross preview coordinator exited before its first frame: " + log);
        Files.writeString(descriptor, Files.readString(descriptor).replaceFirst("}$",
            ",\"pid\":" + pid + "}"));
        getLog().info("TotalCross preview coordinator started with PID " + pid + " after first frame");
    }

    static List<String> previewCommand(Path toolingJdk, Path project, String applicationClass,
        String classpath, Path frame, Path control) throws Exception {
        String java = toolingJdk.resolve("bin").resolve(
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        return List.of(java, "-cp", ToolingCli.runtimeClasspath(), ToolingCli.class.getName(), "preview",
            "--project", project.toString(), "--main", applicationClass, "--classpath", classpath,
            "--jdk-path", toolingJdk.toString(), "--frame-file", frame.toString(), "--control-file", control.toString());
    }

    private long launchCoordinator(List<String> command, Path project, Path log) throws Exception {
        if (System.getProperty("os.name", "").toLowerCase().startsWith("windows")) {
            Process process = new ProcessBuilder(command).directory(project.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
            return process.pid();
        }
        int logIndex = command.size();
        StringBuilder script = new StringBuilder("nohup \"$0\"");
        for (int index = 1; index < logIndex; index++) script.append(" \"${").append(index).append("}\"");
        script.append(" </dev/null >\"${").append(logIndex).append("}\" 2>&1 & echo $!");
        List<String> shell = new java.util.ArrayList<>();
        shell.add("sh");
        shell.add("-c");
        shell.add(sessionDetachScript(script));
        shell.addAll(command);
        shell.add(log.toString());
        Process launcher = new ProcessBuilder(shell).directory(project.toFile()).redirectErrorStream(true).start();
        try (BufferedReader output = new BufferedReader(new InputStreamReader(launcher.getInputStream(), StandardCharsets.UTF_8))) {
            String value = output.readLine();
            if (!launcher.waitFor(5, TimeUnit.SECONDS)) throw new IOException("Unable to detach TotalCross preview coordinator");
            if (value == null || value.isBlank()) throw new IOException("Detached TotalCross preview coordinator did not return a PID");
            return Long.parseLong(value.trim());
        }
    }

    private static String sessionDetachScript(StringBuilder command) {
        String launcher = System.getProperty("os.name", "").toLowerCase().startsWith("mac")
            && Files.isExecutable(Path.of("/usr/bin/perl"))
            ? "/usr/bin/perl -MPOSIX -e 'POSIX::setsid(); exec @ARGV'"
            : "setsid";
        String value = command.toString();
        return value.replaceFirst("nohup ", "nohup " + launcher + " ");
    }

    private boolean awaitFirstFrame(long pid, Path frame) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(frame) && Files.size(frame) > 0) return true;
            if (!ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) return false;
            Thread.sleep(100);
        }
        ProcessHandle.of(pid).ifPresent(process -> {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        });
        return false;
    }
}
