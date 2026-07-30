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
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/** Thin Gradle adapter for the shared conversion engine. */
public abstract class TotalCrossConvertProjectTask extends DefaultTask {
    public TotalCrossConvertProjectTask() {
        getMode().convention("ANALYZE");
        getProjectDirectory().convention(getProject().getLayout().getProjectDirectory());
        getPlanFile().convention(getProject().getLayout().getBuildDirectory().file("totalcross/conversion-plan.json"));
        getNonInteractive().convention(true);
    }

    @Input public abstract Property<String> getMode();
    @InputDirectory public abstract DirectoryProperty getProjectDirectory();
    @OutputFile public abstract RegularFileProperty getPlanFile();
    @Optional @Input public abstract Property<String> getSelectedMainWindow();
    @Optional @Input public abstract Property<String> getSelectedSdkVersion();
    @Optional @Input public abstract Property<Integer> getSelectedJavaTarget();
    @Input public abstract Property<Boolean> getNonInteractive();

    @TaskAction public void convert() throws Exception {
        Path project = getProjectDirectory().get().getAsFile().toPath().toAbsolutePath().normalize();
        String mode = getMode().get().trim().toUpperCase(java.util.Locale.ROOT);
        switch (mode) {
            case "ANALYZE" -> writePlan(review(analyze(project)));
            case "APPLY" -> {
                ConversionPlan plan = review(analyze(project));
                requireReviewed(plan);
                writePlan(plan);
                ProjectConversionTransaction.Result result = new ProjectConversionTransaction().apply(plan,
                    new GradleProjectRenderer().render(plan, "0.1.0", getSelectedJavaTarget().getOrNull()));
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

    private ConversionPlan review(ConversionPlan plan) {
        return new ConversionPlan(plan.schemaVersion(), plan.project(), plan.inventoryFingerprint(), plan.moves(), plan.generatedFiles(),
            select(plan.mainWindowCandidates(), getSelectedMainWindow().getOrNull(), ConversionPlan.MainWindowCandidate::className), plan.scriptEvidence(),
            selectSdk(plan.sdkCandidates(), getSelectedSdkVersion().getOrNull()),
            plan.launcherArguments(), plan.deployArguments(), plan.warnings());
    }

    private void requireReviewed(ConversionPlan plan) {
        if (!getNonInteractive().get()) return;
        if (plan.mainWindowCandidates().size() != 1) throw new IllegalArgumentException("set selectedMainWindow before non-interactive apply");
        if (plan.sdkCandidates().size() > 1) throw new IllegalArgumentException("set selectedSdkVersion before non-interactive apply");
    }

    private static <T> java.util.List<T> select(java.util.List<T> values, String selected, java.util.function.Function<T, String> name) {
        if (selected == null || selected.isBlank()) return values;
        return values.stream().filter(value -> selected.equals(name.apply(value))).findFirst().map(java.util.List::of)
            .orElseThrow(() -> new IllegalArgumentException("selected conversion value was not found: " + selected));
    }

    private static java.util.List<com.totalcross.tooling.conversion.inference.SdkVersionInference.Candidate> selectSdk(
            java.util.List<com.totalcross.tooling.conversion.inference.SdkVersionInference.Candidate> values, String selected) {
        if (selected == null || selected.isBlank()) return values;
        if (values.isEmpty()) return java.util.List.of(new com.totalcross.tooling.conversion.inference.SdkVersionInference.Candidate(
            selected, Path.of("gradle-task"), 0, "user-selection"));
        return select(values, selected, com.totalcross.tooling.conversion.inference.SdkVersionInference.Candidate::version);
    }

    private void writePlan(ConversionPlan plan) throws IOException {
        Path output = getPlanFile().get().getAsFile().toPath();
        Files.createDirectories(output.getParent());
        Files.writeString(output, new ConversionPlanCodec().toJson(plan) + System.lineSeparator());
        getLogger().lifecycle("TotalCross conversion plan: {}", output);
    }
}
