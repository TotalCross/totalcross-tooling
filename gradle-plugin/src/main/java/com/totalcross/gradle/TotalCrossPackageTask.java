/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.process.ExecOperations;

/** Invokes {@code tc.Deploy} with the SDK and JDK resolved by the plugin. */
@DisableCachingByDefault(because = "tc.Deploy writes platform-specific outputs outside Gradle's control")
public abstract class TotalCrossPackageTask extends org.gradle.api.DefaultTask {
    private Configuration runtimeConfiguration;

    @Inject
    protected abstract ExecOperations getExecOperations();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationJar();

    @Classpath
    public abstract ConfigurableFileCollection getRuntimeClasspath();

    @Internal
    public abstract ConfigurableFileCollection getRetrolambdaClasspath();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getExternalResources();

    @Input public abstract Property<String> getApplicationName();
    @Input @Optional public abstract Property<String> getSdkVersion();
    @Input public abstract ListProperty<String> getPlatforms();
    @Input @Optional public abstract Property<String> getActivationKey();
    @Input public abstract Property<Boolean> getTotalcrossLib();
    @Input @Optional public abstract Property<String> getLogLevel();
    @Input public abstract ListProperty<String> getDeployArguments();
    @Optional @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE) public abstract DirectoryProperty getTotalcrossHome();
    @Optional @InputFile @PathSensitive(PathSensitivity.ABSOLUTE) public abstract RegularFileProperty getDeploySdkJar();
    @Optional @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE) public abstract DirectoryProperty getJdkPath();
    @Optional @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE) public abstract DirectoryProperty getCertificates();
    @OutputDirectory public abstract DirectoryProperty getOutputDirectory();

    public void setRuntimeConfiguration(Configuration runtimeConfiguration) {
        this.runtimeConfiguration = runtimeConfiguration;
    }

    @TaskAction
    public void packageApplication() throws IOException {
        if (runtimeConfiguration == null) {
            throw new GradleException("TotalCross runtimeClasspath was not configured");
        }
        ResolvedArtifact sdkArtifact = findSdkArtifact();
        String sdkVersion = getSdkVersion().getOrElse(sdkArtifact.getModuleVersion().getId().getVersion());
        int targetVersion = JavaTargetCompatibility.targetVersion(getApplicationJar().get().getAsFile().toPath());
        boolean useRetrolambda = JavaTargetCompatibility.requiresRetrolambda(sdkVersion, targetVersion);
        Path output = getOutputDirectory().get().getAsFile().toPath();
        Files.createDirectories(output);
        File sdkHome = new SdkResolver(getProject().getGradle().getGradleUserHomeDir().toPath(), new ArchiveDownloader())
                .resolve(sdkVersion, optionalDirectory(getTotalcrossHome()));
        boolean useJdk11 = JavaTargetCompatibility.usesJdk11(sdkVersion);
        File jdkHome = useJdk11
                ? new JdkResolver(getProject().getGradle().getGradleUserHomeDir().toPath(), new ArchiveDownloader(), "11").resolve(null)
                : new JdkResolver(getProject().getGradle().getGradleUserHomeDir().toPath(), new ArchiveDownloader()).resolve(optionalDirectory(getJdkPath()));
        File java = new File(jdkHome, "bin" + File.separator + (isWindows() ? "java.exe" : "java"));

        String applicationName = getApplicationName().get();
        boolean library = getTotalcrossLib().get();
        String deployName = library ? libraryName(applicationName) : applicationName;
        Path stagedJar = output.resolve(deployName + ".jar");
        Files.copy(getApplicationJar().get().getAsFile().toPath(), stagedJar, StandardCopyOption.REPLACE_EXISTING);
        if (useRetrolambda) {
            getLogger().lifecycle("Lowering Java 8 application bytecode to Java 7 with Retrolambda 2.5.7 and JDK 11 for TotalCross SDK {}", sdkVersion);
            lowerWithRetrolambda(stagedJar, output, java);
        }

        List<Path> packageEntries = collectPackageEntries(output);
        writePackageFile(output, packageEntries);
        List<String> arguments = buildArguments(stagedJar, output, deployName, library, sdkVersion);
        File configuredDeploySdkJar = optionalFile(getDeploySdkJar());
        File deploySdkJar = configuredDeploySdkJar != null
                ? configuredDeploySdkJar
                : new File(sdkHome, "dist/totalcross-sdk.jar");
        getLogger().lifecycle("Deploying TotalCross application {} with SDK {} (Java target {})", deployName, sdkVersion, targetVersion);
        getExecOperations().javaexec(spec -> {
            spec.setExecutable(java.getAbsolutePath());
            spec.classpath(getProject().files(deploySdkJar, stagedJar.toFile(), getRuntimeClasspath()));
            spec.getMainClass().set("tc.Deploy");
            spec.setWorkingDir(output.toFile());
            spec.environment("TOTALCROSS3_HOME", sdkHome.getAbsolutePath());
            spec.args(arguments);
        });

        if (library) {
            Path tcz = findGeneratedTcz(output, deployName);
            addToJar(tcz, getApplicationJar().get().getAsFile().toPath());
        }
    }

    private ResolvedArtifact findSdkArtifact() {
        Set<ResolvedArtifact> candidates = new LinkedHashSet<>();
        for (ResolvedArtifact artifact : runtimeConfiguration.getResolvedConfiguration().getResolvedArtifacts()) {
            var id = artifact.getModuleVersion().getId();
            if ("com.totalcross".equals(id.getGroup()) && "totalcross-sdk".equals(id.getName())) {
                candidates.add(artifact);
            }
        }
        if (candidates.isEmpty()) {
            throw new GradleException("totalcrossPackage requires runtime dependency com.totalcross:totalcross-sdk:<version>");
        }
        Set<String> versions = new LinkedHashSet<>();
        candidates.forEach(artifact -> versions.add(artifact.getModuleVersion().getId().getVersion()));
        if (versions.size() != 1) {
            throw new GradleException("Multiple TotalCross SDK versions resolved: " + String.join(", ", versions));
        }
        return candidates.iterator().next();
    }

    private List<Path> collectPackageEntries(Path output) throws IOException {
        List<Path> entries = new ArrayList<>();
        Path libraries = output.resolve("totalcross-libs");
        for (File dependency : getRuntimeClasspath().getFiles()) {
            if (!dependency.isFile()) continue;
            try (ZipFile zip = new ZipFile(dependency)) {
                var zipEntries = zip.entries();
                while (zipEntries.hasMoreElements()) {
                    ZipEntry entry = zipEntries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith("Lib.tcz")) {
                        Path destination = libraries.resolve(Path.of(entry.getName()).getFileName().toString());
                        Files.createDirectories(destination.getParent());
                        try (var input = zip.getInputStream(entry)) {
                            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                        entries.add(destination.toAbsolutePath());
                    }
                }
            } catch (java.util.zip.ZipException ignored) {
                // Gradle classpaths may contain non-JAR entries; they cannot contain a TCZ library.
            }
        }
        for (File resource : getExternalResources().getFiles()) {
            entries.add(resource.toPath().toAbsolutePath());
        }
        return entries;
    }

    private static void writePackageFile(Path output, List<Path> entries) throws IOException {
        Path allPackage = output.resolve("all.pkg");
        if (entries.isEmpty()) {
            Files.deleteIfExists(allPackage);
            return;
        }
        List<String> lines = entries.stream().map(path -> "[L]" + path).toList();
        Files.write(allPackage, lines, StandardCharsets.UTF_8);
    }

    private List<String> buildArguments(Path jar, Path output, String applicationName, boolean library, String sdkVersion) {
        List<String> arguments = new ArrayList<>();
        arguments.add(jar.toAbsolutePath().toString());
        if (!library) {
            getPlatforms().get().stream().map(TotalCrossPackageTask::platformArgument).forEach(arguments::add);
        }
        arguments.add("/n");
        arguments.add(applicationName);
        arguments.add("/p");
        arguments.addAll(logLevelArguments(sdkVersion, getLogLevel().getOrNull()));
        String activationKey = getActivationKey().getOrNull();
        if (activationKey != null && !activationKey.isBlank()) {
            arguments.add("/r");
            arguments.add(activationKey);
        }
        File certificates = optionalDirectory(getCertificates());
        if (certificates != null) {
            arguments.add("/m");
            arguments.add(certificates.getAbsolutePath());
        }
        arguments.add("/o");
        arguments.add(output.toAbsolutePath().toString());
        arguments.addAll(getDeployArguments().get());
        return arguments;
    }

    private static String platformArgument(String platform) {
        return platform.startsWith("-") ? platform : "-" + platform;
    }

    static List<String> logLevelArguments(String sdkVersion, String configuredLevel) {
        if (configuredLevel == null || configuredLevel.isBlank()) return List.of();
        String level = configuredLevel.toLowerCase(Locale.ROOT);
        if (!List.of("quiet", "normal", "verbose", "debug").contains(level)) {
            throw new GradleException("TotalCross deploy logLevel must be quiet, normal, verbose, or debug; found " + configuredLevel + ".");
        }
        if (JavaTargetCompatibility.usesJdk11(sdkVersion)) {
            return "verbose".equals(level) ? List.of("/v") : List.of();
        }
        return List.of("/log-level", level);
    }

    private static File optionalDirectory(DirectoryProperty property) {
        return property.isPresent() ? property.get().getAsFile() : null;
    }

    private static File optionalFile(RegularFileProperty property) {
        return property.isPresent() ? property.get().getAsFile() : null;
    }

    private static String libraryName(String name) {
        return name.endsWith("Lib") ? name : name + "Lib";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

    private void lowerWithRetrolambda(Path stagedJar, Path output, File java) throws IOException {
        Path work = output.resolve("retrolambda");
        Path input = work.resolve("input");
        Path transformed = work.resolve("output");
        SdkResolver.deleteTree(work);
        Files.createDirectories(input);
        unpackJar(stagedJar, input);
        String classpath = getRuntimeClasspath().getFiles().stream()
                .map(File::getAbsolutePath)
                .reduce(input.toAbsolutePath().toString(), (left, right) -> left + File.pathSeparator + right);
        getExecOperations().javaexec(spec -> {
            spec.setExecutable(java.getAbsolutePath());
            spec.classpath(getRetrolambdaClasspath());
            spec.getMainClass().set("net.orfjackal.retrolambda.Main");
            spec.systemProperty("retrolambda.bytecodeVersion", "51");
            spec.systemProperty("retrolambda.inputDir", input.toAbsolutePath().toString());
            spec.systemProperty("retrolambda.outputDir", transformed.toAbsolutePath().toString());
            spec.systemProperty("retrolambda.classpath", classpath);
            spec.systemProperty("retrolambda.defaultMethods", "false");
        });
        repackJar(transformed, stagedJar);
    }

    private static void unpackJar(Path jar, Path destination) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) throw new IOException("Unsafe JAR entry: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (InputStream input = zip.getInputStream(entry)) {
                        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static void repackJar(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) throw new IOException("Retrolambda did not produce output: " + source);
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(target))) {
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                    String name = source.relativize(file).toString().replace(File.separatorChar, '/');
                    output.putNextEntry(new JarEntry(name));
                    Files.copy(file, output);
                    output.closeEntry();
                }
            }
        }
    }


    private static Path findGeneratedTcz(Path output, String name) throws IOException {
        try (var paths = Files.walk(output)) {
            return paths.filter(path -> path.getFileName().toString().equals(name + ".tcz")).findFirst()
                    .orElseThrow(() -> new IOException("tc.Deploy did not generate " + name + ".tcz"));
        }
    }

    private static void addToJar(Path tcz, Path jar) throws IOException {
        URI jarUri = URI.create("jar:" + jar.toUri());
        try (FileSystem fileSystem = FileSystems.newFileSystem(jarUri, Map.of("create", "false"))) {
            Files.copy(tcz, fileSystem.getPath("/" + tcz.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
