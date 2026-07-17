// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * Headless surface that can persist the latest rendered frame as PNG.
 */
public class HeadlessPngSurface extends HeadlessPreviewSurface {
  public void writeLatestFrame(Path outputPath) throws IOException {
    BufferedImage frame = getLatestFrame();
    if (frame == null) {
      throw new IOException("No preview frame is available yet");
    }
    Path parent = outputPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    ImageIO.write(frame, "png", outputPath.toFile());
  }
}
