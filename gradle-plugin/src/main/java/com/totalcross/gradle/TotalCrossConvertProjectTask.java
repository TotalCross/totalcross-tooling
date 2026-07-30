/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.gradle;

import com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer;
import com.totalcross.tooling.conversion.plan.ConversionPlan;
import com.totalcross.tooling.conversion.plan.ConversionPlanCodec;
import com.totalcross.tooling.conversion.plan.GradleProjectRenderer;
import com.totalcross.tooling.conversion.transaction.ProjectConversionTransaction;
import com.totalcross.tooling.conversion.validation.GradleProjectValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Thin Gradle adapter for the shared conversion engine. */
public abstract class TotalCrossConvertProjectTask extends DefaultTask {
    public TotalCrossConvertProjectTask() {
        getMode().convention("ANALYZE");
        getProjectDirectory().convention(getProject().getLayout().getProjectDirectory());
        getPlanFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/conversion-plan.json"));
    }

    @Input public abstract Property<String> getMode();
    @InputDirectory public abstract DirectoryProperty getProjectDirectory();
    @OutputFile public abstract RegularFileProperty getPlanFile();

    @TaskAction public void convert() throws Exception {
        Path project = getProjectDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();
        String mode = getMode().get().trim().toUpperCase(java.util.Locale.ROOT);
        switch (mode) {
            case "ANALYZE" -> writePlan(analyze(project));
            case "APPLY" -> {
                ConversionPlan plan = analyze(project);
                writePlan(plan);
                ProjectConversionTransaction.Result result = new ProjectConversionTransaction().apply(plan,
                    new GradleProjectRenderer().render(plan, "0.1.0"));
                getLogger().lifecycle("TotalCross conversion applied: {} files; journal {}", result.movedFiles(), result.journal());
            }
            case "VALIDATE" -> new GradleProjectValidator().validate(project);
            case "ROLLBACK" -> {
                int restored = new ProjectConversionTransaction().rollback(getPlanFile().get().getAsFile().toPath());
                getLogger().lifecycle("TotalCross conversion rollback restored {} files", restored);
            }
            default -> throw new IllegalArgumentException("totalcrossConvertProject mode must be ANALYZE, APPLY, VALIDATE, or ROLLBACK");
        }
    }

    private ConversionPlan analyze(Path project) throws IOException { return new ProjectConversionAnalyzer().analyze(project); }

    private void writePlan(ConversionPlan plan) throws IOException {
        Path output = getPlanFile().get().getAsFile().toPath();
        Files.createDirectories(output.getParent());
        Files.writeString(output, new ConversionPlanCodec().toJson(plan) + System.lineSeparator());
        getLogger().lifecycle("TotalCross conversion plan: {}", output);
    }
}
