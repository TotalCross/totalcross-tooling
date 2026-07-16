/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

/** Configuration exposed through the {@code totalcross { }} block. */
public class TotalCrossExtension {
    private final Project project;
    private final ConfigurableFileCollection externalResources;
    private final Property<String> applicationName;
    private final Property<String> sdkVersion;
    private final ListProperty<String> platforms;
    private final Property<String> activationKey;
    private final DirectoryProperty totalcrossHome;
    private final RegularFileProperty deploySdkJar;
    private final DirectoryProperty jdkPath;
    private final Property<Boolean> totalcrossLib;
    private final Property<String> logLevel;
    private final DirectoryProperty certificates;
    private final ListProperty<String> deployArguments;

    @Inject
    public TotalCrossExtension(Project project, ObjectFactory objects) {
        this.project = project;
        this.externalResources = objects.fileCollection();
        this.applicationName = objects.property(String.class);
        this.sdkVersion = objects.property(String.class);
        this.platforms = objects.listProperty(String.class);
        this.activationKey = objects.property(String.class);
        this.totalcrossHome = objects.directoryProperty();
        this.deploySdkJar = objects.fileProperty();
        this.jdkPath = objects.directoryProperty();
        this.totalcrossLib = objects.property(Boolean.class);
        this.logLevel = objects.property(String.class);
        this.certificates = objects.directoryProperty();
        this.deployArguments = objects.listProperty(String.class);
        platforms.convention(new ArrayList<>());
        totalcrossLib.convention(false);
        deployArguments.convention(new ArrayList<>());
    }

    public Property<String> getApplicationName() { return applicationName; }
    public Property<String> getSdkVersion() { return sdkVersion; }
    public ListProperty<String> getPlatforms() { return platforms; }
    public Property<String> getActivationKey() { return activationKey; }
    public DirectoryProperty getTotalcrossHome() { return totalcrossHome; }
    public RegularFileProperty getDeploySdkJar() { return deploySdkJar; }
    public DirectoryProperty getJdkPath() { return jdkPath; }
    public Property<Boolean> getTotalcrossLib() { return totalcrossLib; }
    public Property<String> getLogLevel() { return logLevel; }
    public DirectoryProperty getCertificates() { return certificates; }
    public ListProperty<String> getDeployArguments() { return deployArguments; }

    public ConfigurableFileCollection getExternalResources() {
        return externalResources;
    }

    // Assignment-friendly setters for Groovy build scripts.
    public void setApplicationName(String value) { getApplicationName().set(value); }
    public void setSdkVersion(String value) { getSdkVersion().set(value); }
    public void setPlatforms(Iterable<String> value) { getPlatforms().set(value); }
    public void setPlatforms(String... value) { getPlatforms().set(Arrays.asList(value)); }
    public void setActivationKey(String value) { getActivationKey().set(value); }
    public void setTotalcrossHome(Object value) { getTotalcrossHome().fileValue(project.file(value)); }
    public void setDeploySdkJar(Object value) { getDeploySdkJar().fileValue(project.file(value)); }
    public void setJdkPath(Object value) { getJdkPath().fileValue(project.file(value)); }
    public void setTotalcrossLib(boolean value) { getTotalcrossLib().set(value); }
    public void setLogLevel(String value) { getLogLevel().set(value); }
    public void setCertificates(Object value) { getCertificates().fileValue(project.file(value)); }
    public void setDeployArguments(Iterable<String> value) { getDeployArguments().set(value); }
    public void setDeployArguments(String... value) { getDeployArguments().set(Arrays.asList(value)); }
}
