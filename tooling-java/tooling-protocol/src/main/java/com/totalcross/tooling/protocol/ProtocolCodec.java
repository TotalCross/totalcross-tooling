// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Length-prefixed, bounded framing for authenticated loopback messages. */
public final class ProtocolCodec {
  public static final int VERSION = 1;
  public static final int MAX_PAYLOAD = 4 * 1024 * 1024;
  private static final int MAGIC = 0x54435650;
  private static final int MAX_TOKEN = 256;

  private ProtocolCodec() {}

  public static void write(DataOutputStream output, ProtocolMessage message) throws IOException {
    byte[] token = message.token().getBytes(StandardCharsets.UTF_8);
    byte[] payload = message.payload();
    if (token.length > MAX_TOKEN || payload.length > MAX_PAYLOAD) {
      throw new ProtocolException("message exceeds protocol limits");
    }
    output.writeInt(MAGIC);
    output.writeShort(VERSION);
    output.writeByte(message.type().ordinal());
    output.writeLong(message.requestId());
    output.writeInt(token.length);
    output.writeInt(payload.length);
    output.write(token);
    output.write(payload);
    output.flush();
  }

  public static ProtocolMessage read(DataInputStream input) throws IOException {
    final int magic;
    try {
      magic = input.readInt();
    } catch (EOFException eof) {
      return null;
    }
    if (magic != MAGIC) {
      throw new ProtocolException("invalid protocol magic");
    }
    int version = input.readUnsignedShort();
    if (version != VERSION) {
      throw new ProtocolException("unsupported protocol version: " + version);
    }
    int ordinal = input.readUnsignedByte();
    MessageType[] types = MessageType.values();
    if (ordinal >= types.length) {
      throw new ProtocolException("unknown message type");
    }
    long requestId = input.readLong();
    int tokenLength = input.readInt();
    int payloadLength = input.readInt();
    if (tokenLength < 0 || tokenLength > MAX_TOKEN || payloadLength < 0 || payloadLength > MAX_PAYLOAD) {
      throw new ProtocolException("message exceeds protocol limits");
    }
    byte[] token = input.readNBytes(tokenLength);
    byte[] payload = input.readNBytes(payloadLength);
    if (token.length != tokenLength || payload.length != payloadLength) {
      throw new EOFException("truncated protocol message");
    }
    return new ProtocolMessage(types[ordinal], requestId, new String(token, StandardCharsets.UTF_8), payload);
  }
}
