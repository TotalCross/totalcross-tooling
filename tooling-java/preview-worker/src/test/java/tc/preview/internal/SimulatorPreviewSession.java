// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package tc.preview.internal;

import tc.preview.PreviewFrameSink;
import tc.simulator.Launcher;

public final class SimulatorPreviewSession {
  private final Launcher launcher;

  public SimulatorPreviewSession(PreviewFrameSink sink) {
    launcher = new Launcher(sink);
  }

  public void pumpEvents() { launcher.pumpEvents(); }
  public void resize(int width, int height, double density) { }
  public void pointer(int x, int y, int button, boolean pressed) { }
  public void key(int keyCode, boolean pressed, int modifiers) { }
  public void close() { }
}
