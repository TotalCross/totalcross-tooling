/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipFile;

import javax.tools.ToolProvider;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TotalCrossPluginFunctionalTest {
    @TempDir Path projectDirectory;

    @Test
    void packagesApplicationWithConfiguredSdkAndJdk() throws Exception {
        Path repository = projectDirectory.resolve("repository");
        createFakeSdk(repository);
        Path sdkHome = projectDirectory.resolve("sdk-home");
        Files.createDirectories(sdkHome.resolve("etc"));
        Files.writeString(projectDirectory.resolve("asset.txt"), "asset");
        Files.createDirectories(projectDirectory.resolve("src/main/java/example"));
        Files.writeString(projectDirectory.resolve("src/main/java/example/App.java"), "package example; public class App { }");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'functional-app'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), buildScript(repository, sdkHome, "com.totalcross.application"));

        var result = GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments("totalcrossPackage", "--stacktrace")
                .build();

        Path output = projectDirectory.resolve("build/totalcross");
        assertTrue(result.getOutput().contains("Deploying TotalCross application Demo"));
        assertTrue(Files.isRegularFile(output.resolve("Demo.tcz")));
        assertTrue(Files.readString(output.resolve("all.pkg")).contains(projectDirectory.resolve("asset.txt").toAbsolutePath().toString()));
        assertTrue(Files.readString(output.resolve("deploy.args")).contains("-linux|/n|Demo"));
    }

    @Test
    void packagesLibraryWithLibraryPluginByDefault() throws Exception {
        Path repository = projectDirectory.resolve("repository");
        createFakeSdk(repository);
        Path sdkHome = projectDirectory.resolve("sdk-home");
        Files.createDirectories(sdkHome.resolve("etc"));
        Files.createDirectories(projectDirectory.resolve("src/main/java/example"));
        Files.writeString(projectDirectory.resolve("src/main/java/example/Library.java"), "package example; public class Library { }");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'functional-library'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), buildScript(repository, sdkHome, "com.totalcross.library"));

        var result = GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments("totalcrossPackage", "--stacktrace")
                .build();

        Path output = projectDirectory.resolve("build/totalcross");
        Path applicationJar = projectDirectory.resolve("build/libs/functional-library.jar");
        assertTrue(result.getOutput().contains("Deploying TotalCross application DemoLib"));
        assertTrue(Files.isRegularFile(output.resolve("DemoLib.tcz")));
        assertFalse(Files.readString(output.resolve("deploy.args")).contains("linux"));
        try (ZipFile jar = new ZipFile(applicationJar.toFile())) {
            assertTrue(jar.getEntry("DemoLib.tcz") != null);
        }
    }

    @Test
    void usesConfiguredDeploySdkJarBeforeTheSdkDistributionJar() throws Exception {
        Path repository = projectDirectory.resolve("repository");
        createFakeSdk(repository);
        Path patchedDeployJar = createFakeDeployJar(projectDirectory.resolve("patched-totalcross-sdk.jar"), "patched");
        Path sdkHome = projectDirectory.resolve("sdk-home");
        Files.createDirectories(sdkHome.resolve("etc"));
        Files.createDirectories(sdkHome.resolve("dist"));
        createFakeDeployJar(sdkHome.resolve("dist/totalcross-sdk.jar"), "distribution");
        Files.createDirectories(projectDirectory.resolve("src/main/java/example"));
        Files.writeString(projectDirectory.resolve("src/main/java/example/App.java"), "package example; public class App { }");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'custom-deployer'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), buildScript(repository, sdkHome, "com.totalcross.application", patchedDeployJar));

        GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments("totalcrossPackage", "--stacktrace")
                .build();

        assertEquals("patched", Files.readString(projectDirectory.resolve("build/totalcross/deploy.source")));
    }

    @Test
    void writesEquivalentProjectAndPreviewSessionDescriptors() throws Exception {
        Files.createDirectories(projectDirectory.resolve("src/main/java/example"));
        Files.writeString(projectDirectory.resolve("src/main/java/example/App.java"),
                "package example; public class App { }\n");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'App'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n");

        var result = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("totalcrossPreview", "-Ptotalcross-preview.noLaunch=true", "--stacktrace").build();

        assertTrue(result.getOutput().contains("TotalCross preview session ready"));
        assertTrue(Files.isRegularFile(projectDirectory.resolve("build/totalcross/project-model.json")));
        assertTrue(Files.isRegularFile(projectDirectory.resolve("build/totalcross/preview-session.json")));
        String model = Files.readString(projectDirectory.resolve("build/totalcross/project-model.json"));
        assertTrue(model.contains("\"schemaVersion\":1"));
        assertTrue(model.contains("\"mainClass\":\"example.App\""));
        assertTrue(Files.readString(projectDirectory.resolve("build/totalcross/preview-session.json")).contains("GRADLE"));
    }

    @Test
    void ordersClassesBeforeTheProjectModelBeforePreviewAndRun() throws Exception {
        Files.createDirectories(projectDirectory.resolve("src/main/java/example"));
        Files.writeString(projectDirectory.resolve("src/main/java/example/App.java"),
                "package example; public class App { }\n");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'App'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n");

        for (String previewTask : List.of("totalcrossPreview", "totalcrossRun")) {
            String output = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                    .withArguments(previewTask, "--dry-run", "--console=plain").build().getOutput();
            assertBefore(output, ":classes", ":totalcrossProjectModel");
            assertBefore(output, ":totalcrossProjectModel", ":" + previewTask);
        }
    }

    private static void assertBefore(String output, String first, String second) {
        assertTrue(output.indexOf(first) >= 0, "missing task " + first + " in:\n" + output);
        assertTrue(output.indexOf(second) >= 0, "missing task " + second + " in:\n" + output);
        assertTrue(output.indexOf(first) < output.indexOf(second),
                first + " must run before " + second + " in:\n" + output);
    }

    @Test
    void analyzes_projects_through_the_shared_conversion_task() throws Exception {
        Files.createDirectories(projectDirectory.resolve("legacy"));
        Files.writeString(projectDirectory.resolve("legacy/App.java"),
                "package example; public class App extends totalcross.ui.MainWindow {}\n");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'conversion-app'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n");

        var result = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("totalcrossConvertProject", "--stacktrace").build();

        Path plan = projectDirectory.resolve("build/totalcross/conversion-plan.json");
        assertTrue(result.getOutput().contains("TotalCross conversion plan"));
        assertTrue(Files.isRegularFile(plan));
        assertTrue(Files.readString(plan).contains("example.App"));
    }

    @Test
    void applies_explicit_conversion_selections_to_the_shared_plan() throws Exception {
        Files.createDirectories(projectDirectory.resolve("legacy"));
        Files.writeString(projectDirectory.resolve("legacy/App.java"), "package example; public class App extends totalcross.ui.MainWindow {}\n");
        Files.writeString(projectDirectory.resolve("legacy/Other.java"), "package example; public class Other extends totalcross.ui.MainWindow {}\n");
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'conversion-app'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n"
                + "totalcrossConvertProject { selectedMainWindow = 'example.App'; selectedSdkVersion = '7.6.0'; selectedJavaTarget = 11 }\n");

        GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("totalcrossConvertProject", "--stacktrace").build();

        String plan = Files.readString(projectDirectory.resolve("build/totalcross/conversion-plan.json"));
        assertTrue(plan.contains("\"className\":\"example.App\""));
        assertFalse(plan.contains("\"className\":\"example.Other\""));
    }

    @Test
    void previewRunsAgainWhenItsDescriptorAlreadyExistsAndRunDependsOnPreview() throws Exception {
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'preview-app'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n");

        var first = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("totalcrossPreview", "--stacktrace").build();
        var second = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("totalcrossRun", "--stacktrace").build();

        assertTrue(first.getOutput().contains("TotalCross preview session ready"));
        assertTrue(second.getOutput().contains("TotalCross preview session ready"));
    }

    @Test
    void discoversApplicationFqnFromCompiledOutput() throws Exception {
        Path output = projectDirectory.resolve("build/classes/java/main");
        Files.createDirectories(output.resolve("totalcross/sample/main"));
        Files.write(output.resolve("totalcross/sample/main/TCSample.class"), new byte[] {0});

        assertEquals("totalcross.sample.main.TCSample",
                TotalCrossPreviewTask.discoverApplicationClass(output, "TCSample"));
    }

    @Test
    void previewUsesTheSelectedJdkForTheCliAndWorker() throws Exception {
        Path selectedJdk = Path.of("/tmp/selected-jdk");
        List<String> command = TotalCrossPreviewTask.previewCommand(selectedJdk, "preview",
                Path.of("/tmp/frame.png"), Path.of("/tmp/control.txt"));

        String java = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        assertEquals(selectedJdk.resolve("bin").resolve(java).toString(), command.get(0));
        assertTrue(command.contains("-Djava.awt.headless=true"));
        assertTrue(command.contains("preview"));
        List<String> run = TotalCrossPreviewTask.previewCommand(selectedJdk, "run",
                Path.of("/tmp/frame.png"), Path.of("/tmp/control.txt"));
        assertTrue(run.contains("run"));
        assertFalse(run.contains("-Djava.awt.headless=true"));
        assertEquals("/tmp/project-model.json", command.get(command.indexOf("--model") + 1));
        assertEquals("/totalcross-preview.json", command.get(command.indexOf("--config") + 1));
        assertEquals(selectedJdk.toString(), command.get(command.indexOf("--jdk-path") + 1));
    }

    @Test
    void rejectsGradleRuntimesOlderThanJava17() {
        GradleException failure = assertThrows(GradleException.class,
                () -> TotalCrossPlugin.requireJava17(JavaVersion.toVersion(11)));

        assertTrue(failure.getMessage().contains("require Java 17"));
    }

    @Test
    void exposesOnlyThePublicPackageTask() throws Exception {
        Files.writeString(projectDirectory.resolve("settings.gradle"), "rootProject.name = 'package-tasks'\n");
        Files.writeString(projectDirectory.resolve("build.gradle"), "plugins { id 'com.totalcross.application' }\n");

        var result = GradleRunner.create().withProjectDir(projectDirectory.toFile()).withPluginClasspath()
                .withArguments("tasks", "--all").build();

        assertTrue(result.getOutput().contains("totalcrossPackage"));
        assertFalse(result.getOutput().contains("totalcrossTypedPackage"));
    }

    private String buildScript(Path repository, Path sdkHome, String pluginId) {
        return buildScript(repository, sdkHome, pluginId, null);
    }

    private String buildScript(Path repository, Path sdkHome, String pluginId, Path deploySdkJar) {
        String javaHome = System.getProperty("java.home").replace("\\", "\\\\");
        String deploySdkJarConfiguration = deploySdkJar == null
                ? ""
                : "deploySdkJar = file('%s')".formatted(deploySdkJar.toAbsolutePath());
        return """
                plugins {
                    id 'java'
                    id '%s'
                }
                repositories { maven { url = uri('%s') } }
                dependencies { implementation 'com.totalcross:totalcross-sdk:7.3.0' }
                totalcross {
                    applicationName = 'Demo'
                    platforms = ['linux']
                    totalcrossHome = file('%s')
                    jdkPath = file('%s')
                    externalResources.from('asset.txt')
                    %s
                }
                """.formatted(pluginId, repository.toUri(), sdkHome.toAbsolutePath(), javaHome, deploySdkJarConfiguration);
    }

    private static void createFakeSdk(Path repository) throws IOException {
        Path module = repository.resolve("com/totalcross/totalcross-sdk/7.3.0");
        Files.createDirectories(module);
        createFakeDeployJar(module.resolve("totalcross-sdk-7.3.0.jar"), "repository");
        Files.writeString(module.resolve("totalcross-sdk-7.3.0.pom"), "<project><modelVersion>4.0.0</modelVersion><groupId>com.totalcross</groupId><artifactId>totalcross-sdk</artifactId><version>7.3.0</version></project>");
    }

    private static Path createFakeDeployJar(Path target, String sourceMarker) throws IOException {
        Path source = target.resolveSibling(target.getFileName() + ".source/tc/Deploy.java");
        Path classes = target.resolveSibling(target.getFileName() + ".classes");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package tc;
                import java.nio.file.*;
                public class Deploy {
                    public static void main(String[] args) throws Exception {
                        String name = args[args.length - 1];
                        for (int i = 0; i < args.length - 1; i++) if (args[i].equals("/n")) name = args[i + 1];
                        Path output = Path.of(args[0]).getParent();
                        Files.writeString(output.resolve(name + ".tcz"), "fake tcz");
                        Files.writeString(output.resolve("deploy.source"), "%s");
                        Files.writeString(output.resolve("deploy.args"), String.join("|", args));
                    }
                }
                """.formatted(sourceMarker));
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.toString(), source.toString());
        if (result != 0) throw new IOException("Could not compile fake tc.Deploy");
        Files.createDirectories(target.getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(target))) {
            Path classFile = classes.resolve("tc/Deploy.class");
            jar.putNextEntry(new JarEntry("tc/Deploy.class"));
            Files.copy(classFile, jar);
            jar.closeEntry();
        }
        return target;
    }
}
