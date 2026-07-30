/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.validation;

import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessResult;
import com.totalcross.tooling.process.ProcessRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Validates a generated Gradle project through its wrapper without invoking a shell. */
public final class GradleProjectValidator {
  private final ProcessRunner runner;

  public GradleProjectValidator() { this(new ProcessRunner()); }
  GradleProjectValidator(ProcessRunner runner) { this.runner = runner; }

  public Result validate(Path project) throws IOException, InterruptedException {
    Path root = project.toAbsolutePath().normalize();
    String wrapper = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "gradlew.bat" : "./gradlew";
    if (!Files.isRegularFile(root.resolve(wrapper.replace("./", "")))) {
      throw new IOException("Gradle Wrapper is required for conversion validation: " + root);
    }
    ProcessResult process = runner.run(new ProcessRequest(List.of(wrapper, "classes", "totalcrossProjectModel", "--console=plain"),
        root, Map.of(), Duration.ofMinutes(2)));
    if (!process.succeeded()) {
      String output = (process.stdout() + "\n" + process.stderr()).trim();
      throw new IOException(process.timedOut() ? "Gradle validation timed out" : "Gradle validation failed: " + output);
    }
    return new Result(process.stdout());
  }

  public record Result(String output) { }
}
