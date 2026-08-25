// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package tc.preview;

public final class PreviewFrame {
  public int getWidth() { return 1; }
  public int getHeight() { return 1; }
  public int getStride() { return 1; }
  public double getDensity() { return 1; }
  public int[] copyPixels() { return new int[] { 0xff00ff00 }; }
}
