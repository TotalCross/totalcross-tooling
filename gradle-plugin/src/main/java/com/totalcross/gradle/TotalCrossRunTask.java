/* Copyright (C) 2026 Amalgam Solucoes em TI Ltda. */
/* SPDX-License-Identifier: Apache-2.0 */
package com.totalcross.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public abstract class TotalCrossRunTask extends DefaultTask {
    @TaskAction public void runApplication() { getLogger().lifecycle("TotalCross run uses the external preview host"); }
}
