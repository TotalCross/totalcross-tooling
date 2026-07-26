// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.protocol;

import java.util.Arrays;

public final class ProtocolMessage {
  private final MessageType type;
  private final long requestId;
  private final String token;
  private final byte[] payload;

  public ProtocolMessage(MessageType type, long requestId, String token, byte[] payload) {
    if (type == null || requestId < 0 || token == null || payload == null) {
      throw new IllegalArgumentException("invalid protocol message");
    }
    this.type = type;
    this.requestId = requestId;
    this.token = token;
    this.payload = payload.clone();
  }

  public MessageType type() { return type; }
  public long requestId() { return requestId; }
  public String token() { return token; }
  public byte[] payload() { return Arrays.copyOf(payload, payload.length); }
}
