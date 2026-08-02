// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.Graphics2D;
import tc.preview.PreviewFrame;
import tc.preview.PreviewFrameSink;

/**
 * Preview surface that retains the latest rendered frame without showing UI.
 */
public class HeadlessPreviewSurface implements PreviewFrameSink {
  private BufferedImage latestFrame;
  private long frameNumber;

  @Override
  public synchronized void present(PreviewFrame frame) {
    latestFrame = copy(frame);
    frameNumber++;
  }

  /**
   * Returns a copy of the latest frame, or {@code null} if no frame was rendered yet.
   */
  public synchronized BufferedImage getLatestFrame() {
    return latestFrame == null ? null : copyImage(latestFrame);
  }

  public synchronized long getFrameNumber() {
    return frameNumber;
  }

  public synchronized void clear(int width, int height) {
    latestFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    frameNumber++;
  }

  private BufferedImage copy(PreviewFrame frame) {
    BufferedImage copy = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
    int[] target = ((DataBufferInt) copy.getRaster().getDataBuffer()).getData();
    int[] pixels = frame.copyPixels();
    for (int y = 0; y < frame.getHeight(); y++) {
      System.arraycopy(pixels, y * frame.getStride(), target, y * frame.getWidth(), frame.getWidth());
    }
    return copy;
  }

  private BufferedImage copyImage(BufferedImage image) {
    BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
    Graphics2D graphics = copy.createGraphics();
    graphics.drawImage(image, 0, 0, null);
    graphics.dispose();
    return copy;
  }
}
