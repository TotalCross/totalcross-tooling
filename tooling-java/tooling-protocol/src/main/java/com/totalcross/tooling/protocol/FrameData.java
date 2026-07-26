// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Bounded ARGB frame payload; intentionally independent of SDK and AWT types. */
public record FrameData(int width, int height, int stride, double density, int[] pixels) {
  public FrameData {
    if (width < 0 || height < 0 || stride < width || pixels == null || pixels.length != stride * height) {
      throw new IllegalArgumentException("invalid frame");
    }
    pixels = pixels.clone();
  }

  public byte[] encode() {
    ByteBuffer buffer = ByteBuffer.allocate(24 + pixels.length * Integer.BYTES).order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(width).putInt(height).putInt(stride).putDouble(density).putInt(pixels.length);
    for (int pixel : pixels) buffer.putInt(pixel);
    return buffer.array();
  }

  public static FrameData decode(byte[] bytes) {
    if (bytes == null || bytes.length < 24) throw new IllegalArgumentException("invalid frame payload");
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
    int width = buffer.getInt();
    int height = buffer.getInt();
    int stride = buffer.getInt();
    double density = buffer.getDouble();
    int length = buffer.getInt();
    if (width < 0 || height < 0 || stride < width || length != stride * height
        || length < 0 || length > (com.totalcross.tooling.protocol.ProtocolCodec.MAX_PAYLOAD / 4)
        || buffer.remaining() != length * 4) {
      throw new IllegalArgumentException("invalid frame dimensions");
    }
    int[] pixels = new int[length];
    for (int i = 0; i < length; i++) pixels[i] = buffer.getInt();
    return new FrameData(width, height, stride, density, pixels);
  }
}
