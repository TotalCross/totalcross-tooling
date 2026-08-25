// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import com.totalcross.tooling.protocol.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** One disposable authenticated worker process that becomes active after its first frame. */
public final class ProcessWorkerCandidate implements PreviewReloadCoordinator.Candidate {
  private final PreviewHost host;
  private final List<String> command;
  private final String applicationClasspath;
  private final String mainClass;
  private final String selectedClass;
  private final String[] arguments;
  private final BlockingQueue<FrameData> frames = new LinkedBlockingQueue<>();
  private final CompletableFuture<FrameData> firstFrame = new CompletableFuture<>();
  private final Object showMonitor = new Object();
  private CompletableFuture<Void> pendingShow;
  private final AtomicReference<Throwable> failure = new AtomicReference<>();
  private final AtomicBoolean started = new AtomicBoolean();
  private final AtomicBoolean closed = new AtomicBoolean();
  private Thread receiver;

  public ProcessWorkerCandidate(List<String> command, String applicationClasspath, String mainClass, String... arguments)
      throws IOException {
    this(command, applicationClasspath, mainClass, null, arguments);
  }

  public ProcessWorkerCandidate(List<String> command, String applicationClasspath, String mainClass,
      String selectedClass, String[] arguments) throws IOException {
    this.host = new PreviewHost();
    this.command = List.copyOf(command);
    this.applicationClasspath = applicationClasspath;
    this.mainClass = mainClass;
    this.selectedClass = selectedClass == null || selectedClass.isBlank() ? null : selectedClass;
    this.arguments = arguments == null ? new String[0] : arguments.clone();
  }

  @Override public void awaitReady(Duration timeout) throws Exception {
    if (!started.compareAndSet(false, true)) return;
    host.launchWorker(command, applicationClasspath);
    host.sendStart(mainClass, arguments);
    ProtocolMessage message = host.session().receive();
    if (message == null || message.type() != MessageType.READY) {
      throw new IOException("worker did not acknowledge preview start");
    }
    receiver = new Thread(this::receiveFrames, "totalcross-preview-frame-receiver-" + host.workerProcessId());
    receiver.setDaemon(true);
    receiver.start();
  }

  @Override public void awaitFirstFrame(Duration timeout) throws Exception {
    try {
      firstFrame.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (selectedClass != null) {
        frames.clear();
        show(selectedClass, timeout);
      }
    } catch (ExecutionException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof Exception exception) throw exception;
      throw new IOException(selectedClass == null ? "worker failed before its first frame"
          : "worker failed before its selected frame", cause);
    }
  }

  @Override public FrameData nextFrame(Duration timeout) throws Exception {
    FrameData frame = frames.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    if (frame != null) return frame;
    Throwable problem = failure.get();
    if (problem instanceof Exception exception) throw exception;
    if (problem != null) throw new IOException("preview worker failed", problem);
    return null;
  }

  @Override public void resize(int width, int height, double density) throws IOException { host.sendResize(width, height, density); }
  @Override public void pointer(int x, int y, int button, boolean pressed) throws IOException { host.sendPointer(x, y, button, pressed); }
  @Override public void key(int keyCode, boolean pressed, int modifiers) throws IOException { host.sendKey(keyCode, pressed, modifiers); }
  @Override public void show(String className) throws Exception { show(className, Duration.ofSeconds(15)); }
  @Override public long processId() { return host.workerProcessId(); }

  private void show(String className, Duration timeout) throws Exception {
    CompletableFuture<Void> ready = new CompletableFuture<>();
    synchronized (showMonitor) {
      if (pendingShow != null) throw new IOException("preview selection is already in progress");
      pendingShow = ready;
      try {
        host.sendShow(className);
      } catch (IOException failure) {
        pendingShow = null;
        throw failure;
      }
    }
    try {
      ready.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } finally {
      synchronized (showMonitor) {
        if (pendingShow == ready) pendingShow = null;
      }
    }
  }

  @Override public void close() {
    if (!closed.compareAndSet(false, true)) return;
    fail(new IOException("preview worker was closed before its first frame"));
    try { host.close(); } catch (IOException ignored) { }
    if (receiver != null) receiver.interrupt();
  }

  private void receiveFrames() {
    try {
      ProtocolMessage message;
      while ((message = host.session().receive()) != null) {
        if (message.type() == MessageType.FRAME) {
          FrameData frame = FrameData.decode(message.payload());
          frames.offer(frame);
          firstFrame.complete(frame);
        } else if (message.type() == MessageType.SHOW_READY) {
          FrameData latest = null;
          FrameData queued;
          while ((queued = frames.poll()) != null) latest = queued;
          if (latest == null) throw new IOException("worker acknowledged selection without a frame");
          frames.offer(latest);
          CompletableFuture<Void> ready;
          synchronized (showMonitor) { ready = pendingShow; }
          if (ready != null) ready.complete(null);
        } else if (message.type() == MessageType.ERROR) {
          throw new IOException(new String(message.payload(), StandardCharsets.UTF_8));
        } else if (message.type() == MessageType.CLOSED) {
          break;
        }
      }
      if (!closed.get()) fail(new IOException("preview worker closed"));
    } catch (Throwable problem) {
      if (!closed.get()) fail(problem);
    }
  }

  private void fail(Throwable problem) {
    if (failure.compareAndSet(null, problem)) {
      firstFrame.completeExceptionally(problem);
      synchronized (showMonitor) {
        if (pendingShow != null) pendingShow.completeExceptionally(problem);
      }
    }
  }
}
