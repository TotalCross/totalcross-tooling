// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import com.totalcross.tooling.protocol.FrameData;

public interface WorkerRuntime extends AutoCloseable {
  void start(String mainClass, String[] args, FrameSink sink);
  void pump();
  void resize(int width, int height, double density);
  void pointer(int x, int y, int button, boolean pressed);
  void key(int keyCode, boolean pressed, int modifiers);
  void prepareReload();
  void replaceMainWindow(String mainClass, String[] args);
  @Override void close();

  @FunctionalInterface
  interface FrameSink { void accept(FrameData frame); }
}
