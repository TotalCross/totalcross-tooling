// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import com.totalcross.tooling.protocol.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Authenticated command loop owned by the disposable worker process. */
public final class PreviewWorkerSession implements AutoCloseable {
  private final Socket socket;
  private final String token;
  private final WorkerRuntime runtime;
  private final DataInputStream input;
  private final DataOutputStream output;

  public PreviewWorkerSession(Socket socket, String token, WorkerRuntime runtime) throws IOException {
    this.socket = socket;
    this.token = token;
    this.runtime = runtime;
    this.input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    this.output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
  }

  public void run() throws IOException {
    SessionAuthenticator.authenticate(input, token);
    send(MessageType.READY, 0, "authenticated".getBytes(StandardCharsets.UTF_8));
    try {
      ProtocolMessage message;
      while ((message = ProtocolCodec.read(input)) != null) {
        dispatch(message);
        if (message.type() == MessageType.STOP) break;
      }
    } finally {
      runtime.close();
      send(MessageType.CLOSED, 0, new byte[0]);
    }
  }

  private void dispatch(ProtocolMessage message) throws IOException {
    try {
      switch (message.type()) {
        case START -> {
          String[] values = text(message).split("\\n", -1);
          send(MessageType.READY, message.requestId(), new byte[0]);
          runtime.start(values[0], java.util.Arrays.copyOfRange(values, 1, values.length), this::sendFrame);
        }
        case RESIZE -> {
          String[] values = text(message).split(",");
          runtime.resize(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Double.parseDouble(values[2]));
        }
        case POINTER -> {
          String[] values = text(message).split(",");
          runtime.pointer(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]),
              Boolean.parseBoolean(values[3]));
        }
        case KEY -> {
          String[] values = text(message).split(",");
          runtime.key(Integer.parseInt(values[0]), Boolean.parseBoolean(values[1]), Integer.parseInt(values[2]));
        }
        case RELOAD -> {
          String value = text(message);
          runtime.prepareReload();
          if (!value.isBlank()) {
            String[] values = value.split("\\n", -1);
            runtime.replaceMainWindow(values[0], java.util.Arrays.copyOfRange(values, 1, values.length));
          }
          send(MessageType.RELOAD_READY, message.requestId(), new byte[0]);
        }
        case STOP -> { }
        default -> throw new ProtocolException("unsupported worker command: " + message.type());
      }
    } catch (Throwable e) {
      String detail = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
      send(MessageType.ERROR, message.requestId(), detail.getBytes(StandardCharsets.UTF_8));
      if (e instanceof Error error) throw error;
      throw new IOException("preview worker command failed", e);
    }
  }

  private void sendFrame(FrameData frame) {
    try { send(MessageType.FRAME, 0, frame.encode()); }
    catch (IOException e) { throw new UncheckedIOException(e); }
  }

  private void send(MessageType type, long requestId, byte[] payload) throws IOException {
    synchronized (output) { ProtocolCodec.write(output, new ProtocolMessage(type, requestId, token, payload)); }
  }

  private static String text(ProtocolMessage message) { return new String(message.payload(), StandardCharsets.UTF_8); }

  @Override public void close() throws IOException { socket.close(); }
}
