/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.inventory;

import java.nio.file.Path;
import java.util.List;

/** Read-only inventory of regular project files, with a deterministic content fingerprint. */
public record ProjectInventory(Path root, String fingerprint, List<Entry> entries) {
  public ProjectInventory {
    root = root.toAbsolutePath().normalize();
    entries = List.copyOf(entries);
  }

  public record Entry(Path relativePath, long size, String sha256) {
    public Entry {
      relativePath = relativePath.normalize();
    }
  }
}
