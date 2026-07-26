// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.cli;

import com.totalcross.tooling.host.PreviewHost;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Minimal standalone entry point; project/build discovery is added by Plan 06. */
public final class ToolingCli {
  private ToolingCli() {}

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || "--help".equals(args[0])) {
      usage();
      return;
    }
    String command = args[0];
    if (!command.equals("preview") && !command.equals("run")) {
      throw new IllegalArgumentException("unknown command: " + command);
    }
    Path project = project(args);
    if (!Files.exists(project)) throw new IllegalArgumentException("project does not exist: " + project);
    System.out.println("TotalCross " + command + " project=" + project.toAbsolutePath());
    if (command.equals("preview")) {
      try (PreviewHost host = new PreviewHost()) {
        System.out.println("preview host listening on loopback port " + host.session().port());
        // A worker command is intentionally supplied by the fixture until Plan 06 adds discovery.
      }
    }
  }

  private static Path project(String[] args) {
    for (int i = 1; i + 1 < args.length; i++) if ("--project".equals(args[i])) return Path.of(args[i + 1]);
    throw new IllegalArgumentException("missing --project <path>");
  }

  private static void usage() {
    System.out.println("usage: totalcross-tooling preview|run --project <path>");
  }
}
