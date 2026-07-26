// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import java.time.Duration;
import java.util.concurrent.*;

/** Coalesces bursts from editors and never runs work on the caller thread. */
public final class ReloadDebouncer implements AutoCloseable {
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread thread = new Thread(r, "totalcross-preview-reload");
    thread.setDaemon(true);
    return thread;
  });
  private ScheduledFuture<?> pending;

  public synchronized void schedule(Duration delay, Runnable reload) {
    if (pending != null) pending.cancel(false);
    pending = executor.schedule(reload, delay.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override public synchronized void close() {
    if (pending != null) pending.cancel(false);
    executor.shutdownNow();
  }
}
