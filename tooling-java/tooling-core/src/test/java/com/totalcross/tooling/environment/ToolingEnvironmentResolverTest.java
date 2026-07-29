// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.environment;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.jdk.*;
import com.totalcross.tooling.platform.HostPlatform;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolingEnvironmentResolverTest {
  @Test
  void provesExplicitJdkAndAppliesSharedCompatibilityPolicy() throws Exception {
    Path sdk = Files.createTempDirectory("totalcross-sdk-home");
    Files.createDirectories(sdk.resolve("dist"));
    Path javaHome = Path.of(System.getProperty("java.home"));
    ToolingEnvironment environment = resolver().resolve(new ToolingEnvironmentRequest("7.3.0", sdk,
        "explicit test SDK", 17, new JdkRequest("17", javaHome, null), List.of()));
    assertEquals(sdk, environment.sdkHome());
    assertEquals(javaHome, environment.toolingJdk().home());
    assertFalse(environment.requiresRetrolambda());
  }

  @Test
  void rejectsAnApplicationTargetUnsupportedByTheSelectedSdk() throws Exception {
    Path sdk = Files.createTempDirectory("totalcross-sdk-home");
    Files.createDirectories(sdk.resolve("dist"));
    Path javaHome = Path.of(System.getProperty("java.home"));
    assertThrows(IllegalArgumentException.class, () -> resolver().resolve(new ToolingEnvironmentRequest("7.2.2", sdk,
        "explicit test SDK", 9, new JdkRequest("11", javaHome, null), List.of())));
  }

  private static ToolingEnvironmentResolver resolver() {
    return new ToolingEnvironmentResolver(new JdkSelector(new JdkCapabilityProbe(HostPlatform.detect())));
  }
}
