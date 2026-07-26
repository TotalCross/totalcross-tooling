// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.gradle;

import com.totalcross.tooling.deploy.DeployLogLevel;
import com.totalcross.tooling.deploy.DeployPlatform;
import com.totalcross.tooling.deploy.DeployRequest;
import com.totalcross.tooling.deploy.DeployResult;
import com.totalcross.tooling.deploy.DeployService;
import com.totalcross.tooling.deploy.DeployToolchain;
import com.totalcross.tooling.deploy.LegacyDeployService;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/** Proof task that maps Gradle inputs to the shared typed deploy contract. */
public abstract class TypedDeployTask extends DefaultTask {
    @InputFile @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationJar();
    @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract DirectoryProperty getSdkHome();
    @InputDirectory @PathSensitive(PathSensitivity.ABSOLUTE)
    public abstract DirectoryProperty getJdkHome();
    @OutputDirectory public abstract DirectoryProperty getOutputDirectory();
    @Input public abstract ListProperty<String> getPlatforms();
    @Input public abstract Property<Boolean> getLibrary();
    @Input public abstract Property<String> getLogLevel();
    @Classpath public abstract ConfigurableFileCollection getToolchain();

    @Inject
    public TypedDeployTask() {
        getLibrary().convention(false);
        getLogLevel().convention("normal");
        getPlatforms().convention(List.of());
    }

    @TaskAction
    public void deploy() {
        DeployService service = new LegacyDeployService(new DeployToolchain(getToolchain().getFiles().stream()
                .map(File::toPath).toList()));
        DeployRequest request = new DeployRequest(getApplicationJar().get().getAsFile().toPath(),
                getOutputDirectory().get().getAsFile().toPath(), getSdkHome().get().getAsFile().toPath(),
                getJdkHome().get().getAsFile().toPath(), getPlatforms().get().stream()
                .map(TypedDeployTask::platform).toList(), getLibrary().get(), logLevel(), List.of());
        DeployResult result = service.deploy(request);
        result.diagnostics().forEach(diagnostic -> getLogger().lifecycle(diagnostic.message()));
        if (!result.succeeded()) throw new GradleException("Typed TotalCross deploy failed with exit " + result.exitCode());
    }

    private DeployLogLevel logLevel() {
        return DeployLogLevel.valueOf(getLogLevel().get().toUpperCase(Locale.ROOT));
    }

    private static DeployPlatform platform(String value) {
        String normalized = value.replaceFirst("^-", "").replace('-', '_').toUpperCase(Locale.ROOT);
        try {
            return DeployPlatform.valueOf(normalized);
        } catch (IllegalArgumentException error) {
            throw new GradleException("Unsupported typed deploy platform: " + value, error);
        }
    }
}
