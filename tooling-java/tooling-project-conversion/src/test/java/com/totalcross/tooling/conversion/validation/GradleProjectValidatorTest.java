/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.validation;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessResult;
import com.totalcross.tooling.process.ProcessRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GradleProjectValidatorTest {
  @TempDir Path project;

  @Test void invokes_the_wrapper_without_a_shell_and_requires_project_model() throws Exception {
    String wrapper = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "gradlew.bat" : "gradlew";
    Files.writeString(project.resolve(wrapper), "ignored");
    ProcessRequest[] request = new ProcessRequest[1];
    var validator = new GradleProjectValidator(new ProcessRunner() {
      @Override public ProcessResult run(ProcessRequest value) { request[0] = value; return new ProcessResult(0, "ok", "", false); }
    });
    assertEquals("ok", validator.validate(project).output());
    assertTrue(request[0].command().contains("totalcrossProjectModel"));
    assertEquals(project, request[0].workingDirectory());
  }

  @Test void reports_a_failed_wrapper_run() throws Exception {
    String wrapper = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "gradlew.bat" : "gradlew";
    Files.writeString(project.resolve(wrapper), "ignored");
    var validator = new GradleProjectValidator(new ProcessRunner() {
      @Override public ProcessResult run(ProcessRequest value) { return new ProcessResult(1, "", "failure", false); }
    });
    assertThrows(java.io.IOException.class, () -> validator.validate(project));
  }
}
