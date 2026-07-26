// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.protocol;

import java.io.DataInputStream;
import java.io.IOException;

public final class SessionAuthenticator {
  private SessionAuthenticator() {}

  public static ProtocolMessage authenticate(DataInputStream input, String expectedToken) throws IOException {
    ProtocolMessage hello = ProtocolCodec.read(input);
    if (hello == null || hello.type() != MessageType.HELLO || !constantTimeEquals(expectedToken, hello.token())) {
      throw new ProtocolException("invalid preview session authentication");
    }
    return hello;
  }

  private static boolean constantTimeEquals(String left, String right) {
    if (left == null || right == null) return false;
    byte[] a = left.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] b = right.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    return java.security.MessageDigest.isEqual(a, b);
  }
}
