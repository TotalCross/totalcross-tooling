/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.bundling.Jar;

/** Registers the TotalCross application packaging task on Java projects. */
public class TotalCrossPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        boolean library = isLibraryPlugin();
        project.getPluginManager().apply(JavaPlugin.class);
        TotalCrossExtension extension = new TotalCrossExtension(project, project.getObjects());
        project.getExtensions().add("totalcross", extension);
        extension.getApplicationName().convention(project.getName());
        extension.getTotalcrossLib().convention(library);

        Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
        Configuration retrolambda = project.getConfigurations().maybeCreate("totalcrossRetrolambda");
        retrolambda.setCanBeConsumed(false);
        retrolambda.setCanBeResolved(true);
        project.getDependencies().add(retrolambda.getName(), "net.orfjackal.retrolambda:retrolambda:2.5.7");
        var jar = project.getTasks().named("jar", Jar.class);
        var packageTask = project.getTasks().register("totalcrossPackage", TotalCrossPackageTask.class, task -> {
            task.setGroup("build");
            task.setDescription(library ? "Packages the library with tc.Deploy." : "Packages the application with tc.Deploy.");
            task.dependsOn(jar);
            task.getApplicationJar().set(jar.flatMap(Jar::getArchiveFile));
            task.getRuntimeClasspath().from(runtimeClasspath);
            task.getRetrolambdaClasspath().from(retrolambda);
            task.setRuntimeConfiguration(runtimeClasspath);
            task.getApplicationName().convention(extension.getApplicationName());
            task.getSdkVersion().convention(extension.getSdkVersion());
            task.getPlatforms().convention(extension.getPlatforms());
            task.getActivationKey().convention(extension.getActivationKey());
            task.getTotalcrossHome().convention(extension.getTotalcrossHome());
            task.getDeploySdkJar().convention(extension.getDeploySdkJar());
            task.getJdkPath().convention(extension.getJdkPath());
            task.getTotalcrossLib().convention(extension.getTotalcrossLib());
            task.getLogLevel().convention(extension.getLogLevel());
            task.getCertificates().convention(extension.getCertificates());
            task.getDeployArguments().convention(extension.getDeployArguments());
            task.getExternalResources().from(extension.getExternalResources());
            task.getOutputDirectory().convention(project.getLayout().getBuildDirectory().dir("totalcross"));
        });
        project.getTasks().named("assemble").configure(task -> task.dependsOn(packageTask));
    }

    protected boolean isLibraryPlugin() {
        return false;
    }
}
