// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import java.time.*;
import java.util.List;

/** Removes only records whose matching process is dead and whose age is stale. */
public final class StaleSessionCleaner {
  public record Record(String token, long pid, Instant started, Runnable remove) {}
  public int clean(List<Record> records, Duration maxAge) {
    int removed = 0;
    for (Record record : records) {
      boolean old = record.started().plus(maxAge).isBefore(Instant.now());
      boolean dead = ProcessHandle.of(record.pid()).map(handle -> !handle.isAlive()).orElse(true);
      if (old && dead) { record.remove().run(); removed++; }
    }
    return removed;
  }
}
