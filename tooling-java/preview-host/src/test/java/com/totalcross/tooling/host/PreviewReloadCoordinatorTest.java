// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PreviewReloadCoordinatorTest {
  @Test
  void promotesTwentyCandidatesAndKeepsActiveCandidateAfterFailure() {
    PreviewReloadCoordinator coordinator = new PreviewReloadCoordinator(Duration.ofMillis(10));
    AtomicInteger closed = new AtomicInteger();
    PreviewReloadCoordinator.CandidateFactory factory = () -> candidate(closed);
    for (int i = 0; i < 20; i++) assertTrue(coordinator.reload(factory));
    assertEquals(PreviewSessionState.ACTIVE, coordinator.state());
    assertFalse(coordinator.reload(() -> { throw new IllegalStateException("broken"); }));
    assertEquals(PreviewSessionState.FAILED_CANDIDATE, coordinator.state());
    coordinator.close();
    assertEquals(PreviewSessionState.CLOSED, coordinator.state());
    assertEquals(20, closed.get());
  }

  private static PreviewReloadCoordinator.Candidate candidate(AtomicInteger closed) {
    return new PreviewReloadCoordinator.Candidate() {
      public void awaitReady(Duration timeout) {}
      public void awaitFirstFrame(Duration timeout) {}
      public void show(String className) {}
      public void close() { closed.incrementAndGet(); }
    };
  }
}
