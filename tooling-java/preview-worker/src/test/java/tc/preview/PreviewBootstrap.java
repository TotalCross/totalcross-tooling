// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package tc.preview;

public final class PreviewBootstrap {
  private PreviewBootstrap() { }

  public static Object start(String mainClass, String[] args, ClassLoader loader, PreviewFrameSink sink) {
    return new tc.preview.internal.SimulatorPreviewSession(sink);
  }
}
