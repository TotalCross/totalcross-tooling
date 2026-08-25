// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import com.totalcross.tooling.protocol.FrameData;
import java.time.Duration;

/** Promotes a candidate only after readiness and a first valid frame. */
public final class PreviewReloadCoordinator implements AutoCloseable {
  public interface Candidate extends AutoCloseable {
    void awaitReady(Duration timeout) throws Exception;
    void awaitFirstFrame(Duration timeout) throws Exception;
    default FrameData nextFrame(Duration timeout) throws Exception { return null; }
    default void resize(int width, int height, double density) throws Exception { }
    default void pointer(int x, int y, int button, boolean pressed) throws Exception { }
    default void key(int keyCode, boolean pressed, int modifiers) throws Exception { }
    void show(String className) throws Exception;
    default long processId() { return -1; }
    @Override void close();
  }
  @FunctionalInterface public interface CandidateFactory { Candidate start() throws Exception; }

  private final Duration timeout;
  private Candidate active;
  private PreviewSessionState state = PreviewSessionState.IDLE;
  private String lastFailure = "";

  public PreviewReloadCoordinator(Duration timeout) { this.timeout = timeout; }
  public synchronized PreviewSessionState state() { return state; }
  public synchronized String lastFailure() { return lastFailure; }
  public synchronized long activeProcessId() { return active == null ? -1 : active.processId(); }

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
      lastFailure = "";
      if (previous != null) previous.close();
      return true;
    } catch (Exception failure) {
      state = PreviewSessionState.FAILED_CANDIDATE;
      lastFailure = failure.getMessage() == null ? failure.getClass().getName() : failure.getMessage();
      if (candidate != null) candidate.close();
      return false;
    }
  }

  public FrameData nextFrame(Duration timeout) throws Exception {
    Candidate candidate;
    synchronized (this) { candidate = active; }
    return candidate == null ? null : candidate.nextFrame(timeout);
  }

  public void resize(int width, int height, double density) throws Exception { active().resize(width, height, density); }
  public void pointer(int x, int y, int button, boolean pressed) throws Exception { active().pointer(x, y, button, pressed); }
  public void key(int keyCode, boolean pressed, int modifiers) throws Exception { active().key(keyCode, pressed, modifiers); }
  public void show(String className) throws Exception { active().show(className); }

  private synchronized Candidate active() {
    if (active == null) throw new IllegalStateException("preview has no promoted worker");
    return active;
  }

  @Override public synchronized void close() {
    state = PreviewSessionState.STOPPING;
    if (active != null) active.close();
    active = null;
    state = PreviewSessionState.CLOSED;
  }
}
