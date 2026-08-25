// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.protocol.*;
import com.totalcross.tooling.worker.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class PreviewHostSessionTest {
  @Test
  void transfersFrameAndInputThroughAuthenticatedWorkerSession() throws Exception {
    try (PreviewHostSession host = new PreviewHostSession()) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      try {
      RecordingRuntime runtime = new RecordingRuntime();
      Future<?> worker = executor.submit(() -> {
        try (Socket socket = new Socket("127.0.0.1", host.port())) {
          new PreviewWorkerSession(socket, host.token(), runtime).run();
        } catch (Exception e) { throw new RuntimeException(e); }
      });
      host.accept(3000);
      host.send(MessageType.START, 1, "example.Main\narg".getBytes());
      ProtocolMessage ready = host.receive();
      assertEquals(MessageType.READY, ready.type());
      ProtocolMessage frameMessage = host.receive();
      assertEquals(MessageType.FRAME, frameMessage.type());
      FrameData frame = FrameData.decode(frameMessage.payload());
      assertEquals(2, frame.width());
      assertArrayEquals(new int[] { 1, 2, 3, 4 }, frame.pixels());
      host.send(MessageType.POINTER, 2, "3,4,1,true".getBytes());
      host.send(MessageType.KEY, 3, "65,true,0".getBytes());
      host.send(MessageType.SHOW, 4, "example.Screen".getBytes());
      assertEquals(MessageType.FRAME, host.receive().type());
      assertEquals(MessageType.SHOW_READY, host.receive().type());
      host.send(MessageType.STOP, 4, new byte[0]);
      worker.get(3, TimeUnit.SECONDS);
      assertEquals(List.of("start", "pointer", "key", "show:example.Screen", "close"), runtime.calls);
      } finally {
        executor.shutdownNow();
      }
    }
  }

  private static final class RecordingRuntime implements WorkerRuntime {
    final List<String> calls = new ArrayList<>();
    public void start(String mainClass, String[] args, FrameSink sink) {
      this.sink = sink;
      calls.add("start");
      sink.accept(new FrameData(2, 2, 2, 1, new int[] { 1, 2, 3, 4 }));
    }
    public void pump() { calls.add("pump"); }
    public void resize(int width, int height, double density) { calls.add("resize"); }
    public void pointer(int x, int y, int button, boolean pressed) { calls.add("pointer"); }
    public void key(int keyCode, boolean pressed, int modifiers) { calls.add("key"); }
    public void show(String className) {
      calls.add("show:" + className);
      sink.accept(new FrameData(2, 2, 2, 1, new int[] { 5, 6, 7, 8 }));
    }
    public void prepareReload() { calls.add("reload"); }
    public void replaceMainWindow(String mainClass, String[] args) { calls.add("replace"); }
    public void close() { calls.add("close"); }
    private FrameSink sink;
  }
}
