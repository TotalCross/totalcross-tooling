// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import static org.junit.jupiter.api.Assertions.*;
import com.totalcross.tooling.protocol.*;
import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessWorkerCandidateTest {
  @Test
  void promotesRealWorkersAndPreservesTheActiveProcessAfterCandidateFailure() throws Exception {
    try (PreviewReloadCoordinator coordinator = new PreviewReloadCoordinator(Duration.ofSeconds(3))) {
      long previous = -1;
      for (int i = 0; i < 20; i++) {
        assertTrue(coordinator.reload(() -> candidate("frame")));
        long current = coordinator.activeProcessId();
        assertTrue(current > 0);
        if (previous > 0) awaitStopped(previous);
        previous = current;
      }
      assertNotNull(coordinator.nextFrame(Duration.ofSeconds(1)));
      long active = coordinator.activeProcessId();
      assertFalse(coordinator.reload(() -> candidate("no-frame")));
      assertEquals(active, coordinator.activeProcessId());
      assertTrue(ProcessHandle.of(active).orElseThrow().isAlive());
      assertTrue(coordinator.lastFailure().contains("TimeoutException"));
    }
  }

  @Test
  void promotesAWorkerOnlyAfterTheSelectedClassProducesAFrame() throws Exception {
    try (PreviewReloadCoordinator coordinator = new PreviewReloadCoordinator(Duration.ofSeconds(3))) {
      ProcessWorkerCandidate candidate = new ProcessWorkerCandidate(
          List.of(javaExecutable(), "-cp", System.getProperty("java.class.path"),
              FakeWorkerMain.class.getName(), "selection"),
          "", "example.MainWindow", "example.Screen", new String[0]);

      assertTrue(coordinator.reload(() -> candidate));
      FrameData frame = coordinator.nextFrame(Duration.ofSeconds(1));
      assertNotNull(frame);
      assertArrayEquals(new int[] { 0xffff0000 }, frame.pixels());
    }
  }

  private static ProcessWorkerCandidate candidate(String mode) throws IOException {
    return new ProcessWorkerCandidate(List.of(javaExecutable(), "-cp", System.getProperty("java.class.path"),
        FakeWorkerMain.class.getName(), mode), "", "example.MainWindow");
  }

  private static String javaExecutable() {
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java";
    return java.nio.file.Path.of(System.getProperty("java.home"), "bin", name).toString();
  }

  private static void awaitStopped(long pid) throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(3).toNanos();
    while (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false) && System.nanoTime() < deadline) {
      Thread.sleep(20);
    }
    assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false));
  }

  public static final class FakeWorkerMain {
    public static void main(String[] args) throws Exception {
      String mode = args[0];
      int port = Integer.parseInt(args[args.length - 2]);
      String token = args[args.length - 1];
      try (Socket socket = new Socket("127.0.0.1", port);
           DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
           DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {
        SessionAuthenticator.authenticate(input, token);
        send(output, token, MessageType.READY, 0, new byte[0]);
        ProtocolMessage message;
        while ((message = ProtocolCodec.read(input)) != null) {
          if (message.type() == MessageType.START) {
            send(output, token, MessageType.READY, message.requestId(), new byte[0]);
            if ("frame".equals(mode) || "selection".equals(mode)) {
              FrameData frame = new FrameData(1, 1, 1, 1, new int[] { 0xff00ff00 });
              send(output, token, MessageType.FRAME, 0, frame.encode());
            }
          }
          if (message.type() == MessageType.SHOW && "selection".equals(mode)) {
            FrameData frame = new FrameData(1, 1, 1, 1, new int[] { 0xffff0000 });
            send(output, token, MessageType.FRAME, 0, frame.encode());
            send(output, token, MessageType.SHOW_READY, message.requestId(), new byte[0]);
          }
          if (message.type() == MessageType.STOP) break;
        }
        send(output, token, MessageType.CLOSED, 0, new byte[0]);
      }
    }

    private static void send(DataOutputStream output, String token, MessageType type, long requestId, byte[] payload)
        throws IOException {
      ProtocolCodec.write(output, new ProtocolMessage(type, requestId, token, payload));
    }
  }
}
