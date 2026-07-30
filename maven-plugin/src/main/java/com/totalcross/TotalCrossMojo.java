/*
 * Copyright (C) 2019-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.totalcross.tooling.deploy.DeployLogLevel;
import com.totalcross.tooling.deploy.DeployPlatform;
import com.totalcross.tooling.deploy.DeployRequest;
import com.totalcross.tooling.deploy.DeployResult;
import com.totalcross.tooling.deploy.DeployToolchain;
import com.totalcross.tooling.deploy.LegacyDeployService;
import com.totalcross.tooling.compatibility.JavaCompatibilityPolicy;
import com.totalcross.tooling.environment.*;
import com.totalcross.tooling.jdk.*;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.sdk.SdkDistributionResolver;
import com.totalcross.tooling.store.ExternalToolResolver;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "package", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class TotalCrossMojo extends AbstractMojo {

    @Parameter
    private String name;

    @Parameter
    private String activationKey;

    @Parameter
    private String[] platforms;

    @Parameter
    private String[] externalResources;

    @Parameter
    private String certificates;

    @Parameter
    private String totalcrossHome;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private String outputDirectory;

    @Parameter(defaultValue = "${project.build.finalName}", required = true)
    private String finalName;

    @Parameter(defaultValue = "${project.packaging}", required = true)
    private String packaging;

    @Parameter
    private boolean totalcrossLib;

    @Parameter
    private String jdkPath;

    @Component
    private MavenProject mavenProject;

    private TCZUtils tczUtils;

    public void execute() throws MojoExecutionException, MojoFailureException {
        tczUtils = new TCZUtils(mavenProject);
        tczUtils.setAdditionalFilePaths(externalResources);
        addDependenciesToClasspath();
        try {
            setupSDKPath();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        deployThroughSharedService();
        if (totalcrossLib) {
            String pathToTCZ = Paths.get(outputDirectory, name + ".tcz").toAbsolutePath().toString();
            tczUtils.addFileToJar(pathToTCZ);
        }
    }

    private void addDependenciesToClasspath() {
        getLog().info("┌───────── Adding Dependencies to classPath ──────────┐");
        int countTCDeps = 0;
        for (Artifact artifact : mavenProject.getArtifacts()) {
            String output = "│ " + artifact.getArtifactId() + ".jar";
            if (tczUtils.extractTCZsFromArtifactDependency(artifact)) {
                output += " (TotalCross Library)";
                countTCDeps++;
            }
            getLog().info(output);
        }
        tczUtils.includeTCZLibsOnAllPKG();
        getLog().info("├─────────────────────────────────────────────────────");
        getLog().info("│ TotalCross Libraries: " + countTCDeps + " | Total: " + mavenProject.getArtifacts().size());
        getLog().info("└─────────────────────────────────────────────────────┘");

    }

    private void setupSDKPath() throws IOException {

        Artifact sdk = mavenProject.getArtifactMap()
                .get(ArtifactUtils.versionlessKey("com.totalcross", "totalcross-sdk"));
        if (sdk == null || sdk.getFile() == null) throw new IOException("TotalCross SDK artifact is not resolved");
        try {
            JavaCompatibilityPolicy.validate(sdk.getVersion(),
                    JavaCompatibilityPolicy.targetVersion(Paths.get(outputDirectory, finalName + "." + packaging)));
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }

        // Setup environment variable
        boolean configuredSdkHome = totalcrossHome != null;
        if (!configuredSdkHome) totalcrossHome = new SdkDistributionResolver(mavenCacheRoot())
                .resolve(sdk.getVersion(), null).toString();
        try {
            JdkInstallation selectedJdk = JdkCatalogResolver.production().resolve(
                    new JdkRequest(JavaCompatibilityPolicy.usesJdk11(sdk.getVersion()) ? "11" : "17",
                            jdkPath == null ? null : Paths.get(jdkPath), null));
            ToolingEnvironment environment = new ToolingEnvironmentResolver(
                    new JdkSelector(new JdkCapabilityProbe(HostPlatform.detect()))).resolve(
                    new ToolingEnvironmentRequest(sdk.getVersion(), Paths.get(totalcrossHome),
                            configuredSdkHome ? "totalcrossHome" : "Maven shared SDK cache",
                            JavaCompatibilityPolicy.targetVersion(Paths.get(outputDirectory, finalName + "." + packaging)),
                            new JdkRequest(JavaCompatibilityPolicy.usesJdk11(sdk.getVersion()) ? "11" : "17",
                                    selectedJdk.home(), null), List.of()));
            totalcrossHome = environment.sdkHome().toString();
            jdkPath = environment.toolingJdk().home().toString();
        } catch (JdkSelectionException | IllegalArgumentException failure) {
            throw new IOException("TotalCross tooling environment is not usable: " + failure.getMessage(), failure);
        }
    }

    private Path mavenCacheRoot() {
        return Paths.get(System.getProperty("user.home"), ".totalcross", "sdk");
    }

    private void deployThroughSharedService() throws MojoExecutionException {
        Artifact sdk = mavenProject.getArtifactMap().get(ArtifactUtils.versionlessKey("com.totalcross", "totalcross-sdk"));
        if (sdk == null || sdk.getFile() == null) throw new MojoExecutionException("TotalCross SDK artifact is not resolved");
        List<Path> artifacts = new ArrayList<>();
        artifacts.add(deploySdkJar(sdk));
        mavenProject.getArtifacts().stream().map(Artifact::getFile).filter(file -> file != null)
                .map(File::toPath).forEach(artifacts::add);
        DeployToolchain toolchain = new DeployToolchain(artifacts);
        if (hasPlatform(DeployPlatform.ANDROID)) {
            try {
                toolchain = toolchain.withAndroidTools(new ExternalToolResolver().resolve("protoc", Paths.get(jdkPath)),
                        new ExternalToolResolver().resolve("bundletool", Paths.get(jdkPath)));
            } catch (IOException unavailable) {
                getLog().warn("Shared Android tools unavailable; selected SDK fallback will be checked: " + unavailable.getMessage());
            }
        }
        List<String> options = new ArrayList<>();
        String deployName = totalcrossLib ? TCZUtils.verifyAndFixLibName(mavenProject.getArtifactId())
                : (name == null || name.isBlank() ? mavenProject.getArtifactId() : name);
        options.add("/p");
        options.add("/n"); options.add(deployName);
        if (activationKey != null && !activationKey.isBlank()) { options.add("/r"); options.add(activationKey); }
        if (certificates != null && !certificates.isBlank()) { options.add("/m"); options.add(certificates); }
        Path input = Paths.get(outputDirectory, finalName + "." + packaging).toAbsolutePath();
        DeployRequest request = new DeployRequest(input, Paths.get(outputDirectory).toAbsolutePath(), Paths.get(totalcrossHome),
                Paths.get(jdkPath), selectedPlatforms(), totalcrossLib, DeployLogLevel.NORMAL, options);
        DeployResult result = new LegacyDeployService(toolchain).deploy(request);
        result.diagnostics().forEach(diagnostic -> getLog().info(diagnostic.message()));
        if (!result.succeeded()) throw new MojoExecutionException("Shared TotalCross deploy failed with exit " + result.exitCode());
    }

    private Path deploySdkJar(Artifact sdk) {
        Path installedJar = Paths.get(totalcrossHome, "dist", "totalcross-sdk.jar");
        return java.nio.file.Files.isRegularFile(installedJar) ? installedJar : sdk.getFile().toPath();
    }

    private List<DeployPlatform> selectedPlatforms() throws MojoExecutionException {
        if (totalcrossLib || platforms == null) return List.of();
        List<DeployPlatform> result = new ArrayList<>();
        for (String value : platforms) {
            String normalized = value.replaceFirst("^-", "").replace('-', '_').toUpperCase(Locale.ROOT);
            try { result.add(DeployPlatform.valueOf(normalized)); }
            catch (IllegalArgumentException error) { throw new MojoExecutionException("Unsupported TotalCross platform: " + value, error); }
        }
        return result;
    }

    private boolean hasPlatform(DeployPlatform target) throws MojoExecutionException {
        return selectedPlatforms().contains(target);
    }
}
