// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import com.totalcross.tooling.protocol.FrameData;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Binds the worker to the SDK through the stable {@code tc.preview} contract. */
public final class ReflectiveRuntimeBridge implements WorkerRuntime {
  private final ClassLoader applicationLoader;
  private Object session;

  public ReflectiveRuntimeBridge(ClassLoader applicationLoader) {
    this.applicationLoader = applicationLoader;
  }

  @Override
  public void start(String mainClass, String[] args, FrameSink sink) {
    try {
      Class<?> bootstrapType = Class.forName("tc.preview.PreviewBootstrap", true, applicationLoader);
      Class<?> sinkType = Class.forName("tc.preview.PreviewFrameSink", true, applicationLoader);
      Object frameSink = Proxy.newProxyInstance(applicationLoader, new Class<?>[] { sinkType },
          (proxy, method, values) -> {
            if (method.getName().equals("present")) {
              sink.accept(toFrame(values[0]));
            }
            return null;
          });
      Method start = bootstrapType.getMethod("start", String.class, String[].class, ClassLoader.class, sinkType);
      session = start.invoke(null, mainClass, args == null ? new String[0] : args.clone(), applicationLoader, frameSink);
    } catch (ReflectiveOperationException e) {
      throw commandFailure("unable to start TotalCross preview session", e);
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
    if (session == null) {
      return;
    }
    try {
      session.getClass().getMethod(name, types).invoke(session, args);
    } catch (ReflectiveOperationException e) {
      throw commandFailure("preview session command failed", e);
    }
  }

  public void pump() {
    invoke("pumpEvents", new Class<?>[0]);
  }

  public void resize(int width, int height, double density) {
    invoke("resize", new Class<?>[] { int.class, int.class, double.class }, width, height, density);
  }

  public void pointer(int x, int y, int button, boolean pressed) {
    invoke("pointer", new Class<?>[] { int.class, int.class, int.class, boolean.class }, x, y, button, pressed);
  }

  public void key(int keyCode, boolean pressed, int modifiers) {
    invoke("key", new Class<?>[] { int.class, boolean.class, int.class }, keyCode, pressed, modifiers);
  }

  public void close() {
    invoke("close", new Class<?>[0]);
    session = null;
  }

  private static IllegalStateException commandFailure(String message, ReflectiveOperationException error) {
    Throwable cause = error instanceof InvocationTargetException && error.getCause() != null
        ? error.getCause() : error;
    String detail = cause.getMessage();
    return new IllegalStateException(message + (detail == null || detail.isBlank() ? "" : ": " + detail), cause);
  }
}
