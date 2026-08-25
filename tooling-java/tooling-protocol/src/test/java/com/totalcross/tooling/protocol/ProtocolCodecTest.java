// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.protocol;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import org.junit.jupiter.api.Test;

class ProtocolCodecTest {
  @Test
  void appendsSelectionMessagesWithoutRenumberingTheExistingProtocol() {
    assertEquals(12, MessageType.ERROR.ordinal());
    assertEquals(13, MessageType.SHOW.ordinal());
    assertEquals(14, MessageType.SHOW_READY.ordinal());
  }

  @Test
  void roundTripsBoundedMessageAndCopiesPayload() throws Exception {
    byte[] payload = { 1, 2, 3 };
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ProtocolCodec.write(new DataOutputStream(bytes), new ProtocolMessage(MessageType.FRAME, 4, "token", payload));
    payload[0] = 9;
    ProtocolMessage result = ProtocolCodec.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    assertEquals(MessageType.FRAME, result.type());
    assertEquals(4, result.requestId());
    assertArrayEquals(new byte[] { 1, 2, 3 }, result.payload());
  }

  @Test
  void rejectsInvalidTokenAndOversizedPayloadBeforeAllocation() throws Exception {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ProtocolCodec.write(new DataOutputStream(bytes), new ProtocolMessage(MessageType.HELLO, 0, "wrong", new byte[0]));
    assertThrows(ProtocolException.class, () -> SessionAuthenticator.authenticate(
        new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), "expected"));
    assertThrows(ProtocolException.class, () -> ProtocolCodec.write(new DataOutputStream(new ByteArrayOutputStream()),
        new ProtocolMessage(MessageType.FRAME, 0, "t", new byte[ProtocolCodec.MAX_PAYLOAD + 1])));
  }
}
