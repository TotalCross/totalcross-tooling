// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import com.totalcross.tooling.protocol.FrameData;
import java.lang.reflect.*;
import java.util.Arrays;

/** Binds the worker to the SDK without linking tooling APIs to SDK classes. */
public final class ReflectiveRuntimeBridge implements WorkerRuntime {
  private final ClassLoader applicationLoader;
  private Object runtime;

  public ReflectiveRuntimeBridge(ClassLoader applicationLoader) { this.applicationLoader = applicationLoader; }

  @Override
  public void start(String mainClass, String[] args, FrameSink sink) {
    try {
      Class<?> runtimeType = Class.forName("totalcross.LauncherRuntime", true, applicationLoader);
      Class<?> consumerType = Class.forName("totalcross.preview.PreviewFrameConsumer", true, applicationLoader);
      Object consumer = Proxy.newProxyInstance(applicationLoader, new Class<?>[] { consumerType }, (proxy, method, values) -> {
        if (method.getName().equals("present")) sink.accept(toFrame(values[0]));
        return null;
      });
      Method start = runtimeType.getMethod("startPreviewFrames", String.class, consumerType, ClassLoader.class,
          String[].class);
      runtime = start.invoke(null, mainClass, consumer, applicationLoader, args);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("unable to start TotalCross preview runtime", e);
    }
  }

  private static FrameData toFrame(Object value) throws ReflectiveOperationException {
    Class<?> type = value.getClass();
    int width = (int) type.getMethod("getWidth").invoke(value);
    int height = (int) type.getMethod("getHeight").invoke(value);
    int stride = (int) type.getMethod("getStride").invoke(value);
    double density = (double) type.getMethod("getDensity").invoke(value);
    int[] pixels = (int[]) type.getMethod("copyPixels").invoke(value);
    return new FrameData(width, height, stride, density, pixels);
  }

  private void invoke(String name, Class<?>[] types, Object... args) {
    if (runtime == null) return;
    try { runtime.getClass().getMethod(name, types).invoke(runtime, args); }
    catch (ReflectiveOperationException e) { throw new IllegalStateException("preview runtime command failed", e); }
  }

  public void pump() { invoke("pumpEvents", new Class<?>[0]); }
  public void resize(int width, int height, double density) { }
  public void pointer(int x, int y, int button, boolean pressed) { }
  public void key(int keyCode, boolean pressed, int modifiers) { }
  public void prepareReload() { invoke("preparePreviewMainWindowReload", new Class<?>[0]); }
  public void replaceMainWindow(String mainClass, String[] args) { }
  public void close() { invoke("close", new Class<?>[0]); }
}
