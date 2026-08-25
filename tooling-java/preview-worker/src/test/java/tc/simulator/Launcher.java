// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package tc.simulator;

import tc.preview.PreviewFrame;
import tc.preview.PreviewFrameSink;
import totalcross.ui.Container;
import totalcross.ui.Control;
import totalcross.ui.MainWindow;

public final class Launcher {
  public static Launcher last;
  public String action = "";
  public boolean prepared;
  public int pumpCount;
  private final PreviewFrameSink sink;

  public Launcher(PreviewFrameSink sink) {
    this.sink = sink;
    last = this;
    present();
  }

  public void preparePreviewMainWindowReload() { prepared = true; }
  public void replaceMainWindow(MainWindow mainWindow, String args) { action = "main-window"; present(); }
  public void showContainer(Container container) { action = "container"; present(); }
  public void showControl(Control control) { action = "control"; present(); }
  public void pumpEvents() { pumpCount++; }

  private void present() { sink.present(new PreviewFrame()); }
}
