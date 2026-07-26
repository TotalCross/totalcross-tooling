// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import com.totalcross.tooling.protocol.*;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Base64;

/** Loopback coordinator. Window code is intentionally kept outside this class. */
public final class PreviewHostSession implements AutoCloseable {
  private final ServerSocket server;
  private final String token;
  private Socket socket;
  private DataInputStream input;
  private DataOutputStream output;

  public PreviewHostSession() throws IOException {
    server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24));
  }

  public int port() { return server.getLocalPort(); }
  public String token() { return token; }

  public void accept(long timeoutMillis) throws IOException {
    server.setSoTimeout((int) Math.min(Integer.MAX_VALUE, timeoutMillis));
    socket = server.accept();
    input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    send(MessageType.HELLO, 0, new byte[0]);
    ProtocolMessage ready = ProtocolCodec.read(input);
    if (ready == null || ready.type() != MessageType.READY) throw new com.totalcross.tooling.protocol.ProtocolException("worker did not become ready");
  }

  public void send(MessageType type, long requestId, byte[] payload) throws IOException {
    if (output == null) throw new IllegalStateException("worker is not connected");
    synchronized (this) { ProtocolCodec.write(output, new ProtocolMessage(type, requestId, token, payload)); }
  }

  public ProtocolMessage receive() throws IOException {
    if (input == null) throw new IllegalStateException("worker is not connected");
    return ProtocolCodec.read(input);
  }

  public void stop() throws IOException { send(MessageType.STOP, 0, new byte[0]); }

  private static byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  @Override public void close() throws IOException {
    if (socket != null) socket.close();
    server.close();
  }
}
