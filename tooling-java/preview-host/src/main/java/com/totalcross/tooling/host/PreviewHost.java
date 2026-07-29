// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import com.totalcross.tooling.protocol.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Standalone preview host facade; build-tool discovery is deferred to Plan 06. */
public final class PreviewHost implements AutoCloseable {
  private final PreviewHostSession session;
  private Process worker;
  private final StringBuilder workerOutput = new StringBuilder();

  public PreviewHost() throws IOException { session = new PreviewHostSession(); }
  public PreviewHostSession session() { return session; }

  public void launchWorker(List<String> command) throws IOException {
    launchWorker(command, null);
  }

  public void launchWorker(List<String> command, String applicationClasspath) throws IOException {
    java.util.ArrayList<String> args = new java.util.ArrayList<>(command);
    args.add(Integer.toString(session.port()));
    args.add(session.token());
    if (applicationClasspath != null && !applicationClasspath.isBlank()) args.add(applicationClasspath);
    worker = new ProcessBuilder(args).redirectErrorStream(true).start();
    Thread outputReader = new Thread(() -> drain(worker.getInputStream()), "totalcross-preview-worker-output");
    outputReader.setDaemon(true);
    outputReader.start();
    try {
      session.accept(15_000);
    } catch (IOException failure) {
      worker.destroyForcibly();
      throw failure;
    }
  }

  public FrameData receiveFrame() throws IOException {
    ProtocolMessage message = session.receive();
    if (message == null) return null;
    if (message.type() == MessageType.ERROR) throw new IOException(new String(message.payload(), StandardCharsets.UTF_8));
    return message.type() == MessageType.FRAME ? FrameData.decode(message.payload()) : null;
  }

  public void sendStart(String mainClass, String... args) throws IOException {
    StringBuilder value = new StringBuilder(mainClass);
    for (String arg : args) value.append('\n').append(arg);
    session.send(MessageType.START, 1, value.toString().getBytes(StandardCharsets.UTF_8));
  }

  public void sendResize(int width, int height, double density) throws IOException {
    session.send(MessageType.RESIZE, 2, (width + "," + height + "," + density).getBytes(StandardCharsets.UTF_8));
  }

  public void sendPointer(int x, int y, int button, boolean pressed) throws IOException {
    session.send(MessageType.POINTER, 3, (x + "," + y + "," + button + "," + pressed).getBytes(StandardCharsets.UTF_8));
  }

  public void sendKey(int keyCode, boolean pressed, int modifiers) throws IOException {
    session.send(MessageType.KEY, 4, (keyCode + "," + pressed + "," + modifiers).getBytes(StandardCharsets.UTF_8));
  }

  /** Returns worker diagnostics when the child closes before the protocol reports an error. */
  public String workerDiagnostics() {
    if (worker != null && !worker.isAlive()) {
      try { worker.waitFor(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
    synchronized (workerOutput) { return workerOutput.toString().trim(); }
  }

  public void prepareReload() throws IOException { session.send(MessageType.RELOAD, 5, new byte[0]); }

  public void reload(String mainClass, String... args) throws IOException {
    StringBuilder value = new StringBuilder(mainClass);
    for (String arg : args) value.append('\n').append(arg);
    session.send(MessageType.RELOAD, 5, value.toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override public void close() throws IOException {
    if (worker != null && worker.isAlive()) { session.stop(); worker.destroy(); }
    session.close();
  }

  private void drain(InputStream stream) {
    try (stream) {
      byte[] buffer = new byte[1024];
      int count;
      while ((count = stream.read(buffer)) >= 0) {
        synchronized (workerOutput) { workerOutput.append(new String(buffer, 0, count, StandardCharsets.UTF_8)); }
      }
    } catch (IOException ignored) {
      // The worker may be terminated while its diagnostics stream is being drained.
    }
  }
}
