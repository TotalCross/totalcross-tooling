/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import com.totalcross.tooling.build.*;
import com.totalcross.tooling.cli.ToolingCli;
import com.totalcross.tooling.jdk.JdkCatalogResolver;
import com.totalcross.tooling.jdk.JdkInstallation;
import com.totalcross.tooling.jdk.JdkRequest;
import java.io.IOException;
import java.nio.file.*;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

public abstract class TotalCrossPreviewTask extends DefaultTask {
    @OutputFile public abstract RegularFileProperty getSessionFile();
    @InputFiles public abstract ConfigurableFileCollection getWatchedInputs();
    @Optional @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract org.gradle.api.file.DirectoryProperty getJdkPath();
    private final Property<String> applicationClass;

    public TotalCrossPreviewTask() {
        getSessionFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/preview-session.json"));
        getWatchedInputs().from(getProject().fileTree("src/main"));
        getOutputs().upToDateWhen(task -> false);
        applicationClass = getProject().getObjects().property(String.class);
        applicationClass.convention(getProject().getName());
    }
    @Input public Property<String> getApplicationClass() { return applicationClass; }

    @TaskAction public void startPreview() throws Exception {
        Path project = getProject().getProjectDir().toPath().toAbsolutePath().normalize();
        Path session = getSessionFile().get().getAsFile().toPath();
        String resolvedApplicationClass = discoverApplicationClass(project.resolve("build/classes/java/main"), applicationClass.get());
        Files.createDirectories(session.getParent());
        Files.writeString(session, new PreviewSessionDescriptor(1, BuildTool.GRADLE, project, session,
            resolvedApplicationClass, null).toJson());
        getLogger().lifecycle("TotalCross preview session ready: {}", session);
        Path classes = project.resolve("build/classes/java/main");
        if (!hasCompiledClasses(classes)) {
            getLogger().lifecycle("TotalCross preview coordinator deferred until compiled application classes exist");
            return;
        }
        if (!Boolean.parseBoolean(String.valueOf(getProject().findProperty("totalcross.preview.noLaunch")))) {
            launchSharedPreview(resolvedApplicationClass, session);
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

    private static boolean hasCompiledClasses(Path output) throws IOException {
        if (!Files.isDirectory(output)) return false;
        try (Stream<Path> paths = Files.walk(output)) {
            return paths.anyMatch(Files::isRegularFile);
        }
    }

    private void launchSharedPreview(String resolvedApplicationClass, Path session) throws Exception {
        var mainSourceSet = getProject().getExtensions().getByType(SourceSetContainer.class)
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        String value = mainSourceSet.getRuntimeClasspath().getFiles().stream()
            .map(java.io.File::getAbsolutePath).collect(Collectors.joining(java.io.File.pathSeparator));
        Path configuredJdk = getJdkPath().isPresent()
            ? getJdkPath().get().getAsFile().toPath() : null;
        JdkInstallation toolingJdk = JdkCatalogResolver.production().resolve(
            new JdkRequest("17", configuredJdk, null));
        Path frame = session.resolveSibling("preview-frame.png");
        Path control = session.resolveSibling("preview-control.txt");
        Path log = session.resolveSibling("preview.log");
        Files.deleteIfExists(frame);
        Process process = new ProcessBuilder(previewCommand(toolingJdk.home(), projectDirectory().toPath(),
            resolvedApplicationClass, value, frame, control))
            .directory(projectDirectory()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!awaitFirstFrame(process, frame)) throw new IOException("TotalCross preview coordinator exited before its first frame: " + log);
        Files.writeString(session, Files.readString(session).replaceFirst("}$",
            ",\"pid\":" + process.pid() + "}"));
        getLogger().lifecycle("TotalCross preview coordinator started with PID {} after first frame", process.pid());
    }

    static List<String> previewCommand(Path toolingJdk, Path project, String applicationClass,
        String classpath, Path frame, Path control) throws Exception {
        String java = toolingJdk.resolve("bin").resolve(
            System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        return List.of(java, "-cp", ToolingCli.runtimeClasspath(), ToolingCli.class.getName(), "preview",
            "--project", project.toString(), "--main", applicationClass, "--classpath", classpath,
            "--jdk-path", toolingJdk.toString(), "--frame-file", frame.toString(), "--control-file", control.toString());
    }

    private boolean awaitFirstFrame(Process process, Path frame) throws InterruptedException, IOException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(frame) && Files.size(frame) > 0) return true;
            if (!process.isAlive()) return false;
            Thread.sleep(100);
        }
        process.destroyForcibly();
        return false;
    }

    private java.io.File projectDirectory() { return getProject().getProjectDir(); }

}
