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
      Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
      throw new IllegalStateException("unable to start TotalCross preview runtime: "
          + (cause.getMessage() == null ? cause.getClass().getName() : cause.getMessage()), cause);
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
  public void resize(int width, int height, double density) {
    invokeOptional("resizePreview", new Class<?>[] { int.class, int.class, double.class }, width, height, density);
  }
  public void pointer(int x, int y, int button, boolean pressed) {
    invokeOptional("injectPreviewPointer", new Class<?>[] { int.class, int.class, int.class, boolean.class },
        x, y, button, pressed);
  }
  public void key(int keyCode, boolean pressed, int modifiers) {
    invokeOptional("injectPreviewKey", new Class<?>[] { int.class, boolean.class, int.class }, keyCode, pressed, modifiers);
  }
  public void prepareReload() { invoke("preparePreviewMainWindowReload", new Class<?>[0]); }
  public void replaceMainWindow(String mainClass, String[] args) {
    try {
      Class<?> windowType = Class.forName("totalcross.ui.MainWindow", true, applicationLoader);
      Method create = runtime.getClass().getMethod("createMainWindow", String.class, ClassLoader.class, boolean.class);
      Object window = create.invoke(runtime, mainClass, applicationLoader, false);
      Method replace = runtime.getClass().getMethod("replaceMainWindow", windowType, String.class);
      replace.invoke(runtime, window, String.join(" ", args));
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("preview runtime replacement failed", e);
    }
  }
  public void close() { invoke("close", new Class<?>[0]); }

  private void invokeOptional(String name, Class<?>[] types, Object... args) {
    if (runtime == null) return;
    try {
      runtime.getClass().getMethod(name, types).invoke(runtime, args);
    } catch (NoSuchMethodException ignored) {
      // Older SDKs expose the preview lifecycle but not input injection.
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("preview runtime command failed", e);
    }
  }
}
