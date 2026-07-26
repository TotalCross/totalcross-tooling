/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import com.totalcross.tooling.build.BuildNotification;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "run")
public final class TotalCrossRunMojo extends AbstractMojo {
    @Override public void execute() throws MojoExecutionException {
        getLog().info(BuildNotification.reload("TotalCross run delegates to the external preview host").message());
    }
}
