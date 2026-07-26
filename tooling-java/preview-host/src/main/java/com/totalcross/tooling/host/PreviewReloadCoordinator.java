// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import java.time.Duration;
import java.util.concurrent.*;

/** Promotes a candidate only after readiness and a first valid frame. */
public final class PreviewReloadCoordinator implements AutoCloseable {
  public interface Candidate extends AutoCloseable {
    void awaitReady(Duration timeout) throws Exception;
    void awaitFirstFrame(Duration timeout) throws Exception;
    @Override void close();
  }
  @FunctionalInterface public interface CandidateFactory { Candidate start() throws Exception; }

  private final Duration timeout;
  private Candidate active;
  private PreviewSessionState state = PreviewSessionState.IDLE;

  public PreviewReloadCoordinator(Duration timeout) { this.timeout = timeout; }
  public synchronized PreviewSessionState state() { return state; }

  public synchronized boolean reload(CandidateFactory factory) {
    state = PreviewSessionState.STARTING_CANDIDATE;
    Candidate candidate = null;
    try {
      candidate = factory.start();
      candidate.awaitReady(timeout);
      candidate.awaitFirstFrame(timeout);
      Candidate previous = active;
      active = candidate;
      state = PreviewSessionState.ACTIVE;
      if (previous != null) previous.close();
      return true;
    } catch (Exception failure) {
      state = PreviewSessionState.FAILED_CANDIDATE;
      if (candidate != null) candidate.close();
      return false;
    }
  }

  @Override public synchronized void close() {
    state = PreviewSessionState.STOPPING;
    if (active != null) active.close();
    active = null;
    state = PreviewSessionState.CLOSED;
  }
}
