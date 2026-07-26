/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
public final class TotalCrossRunMojo extends TotalCrossPreviewMojo { }
