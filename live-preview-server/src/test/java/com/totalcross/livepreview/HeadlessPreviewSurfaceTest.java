// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.awt.image.BufferedImage;
import tc.preview.PreviewFrame;

import org.junit.jupiter.api.Test;

class HeadlessPreviewSurfaceTest {
  @Test
  void presentStoresLatestFrameAndIncrementsFrameNumber() {
    HeadlessPreviewSurface surface = new HeadlessPreviewSurface();
    PreviewFrame frame = new PreviewFrame(2, 2, 2, 1, PreviewFrame.PixelFormat.ARGB_8888,
        new int[] { 0, 0, 0, 0xFF112233 });

    surface.present(frame);

    BufferedImage latest = surface.getLatestFrame();
    assertNotNull(latest);
    assertEquals(1, surface.getFrameNumber());
    assertEquals(0xFF112233, latest.getRGB(1, 1));
  }

  @Test
  void presentAndReadUseDefensiveCopies() {
    HeadlessPreviewSurface surface = new HeadlessPreviewSurface();
    PreviewFrame frame = new PreviewFrame(1, 1, 1, 1, PreviewFrame.PixelFormat.ARGB_8888,
        new int[] { 0xFF000001 });

    surface.present(frame);
    var firstRead = surface.getLatestFrame();
    firstRead.setRGB(0, 0, 0xFF000003);
    BufferedImage secondRead = surface.getLatestFrame();

    assertNotSame(frame, firstRead);
    assertNotSame(firstRead, secondRead);
    assertEquals(0xFF000001, secondRead.getRGB(0, 0));
  }
}
