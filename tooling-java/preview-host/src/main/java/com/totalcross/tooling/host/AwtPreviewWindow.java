// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.host;

import com.totalcross.tooling.protocol.FrameData;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/** Small AWT host that keeps process/session coordination reusable by other hosts. */
public final class AwtPreviewWindow extends Canvas implements AutoCloseable {
  public interface InputListener {
    void pointer(int x, int y, int button, boolean pressed);
    void key(int keyCode, boolean pressed, int modifiers);
  }

  private final Frame window;
  private final InputListener listener;
  private BufferedImage image;

  public AwtPreviewWindow(String title, InputListener listener) {
    this.listener = listener;
    window = new Frame(title == null ? "TotalCross Preview" : title);
    window.add(this);
    window.setSize(480, 620);
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent event) { if (AwtPreviewWindow.this.listener != null) AwtPreviewWindow.this.listener.pointer(event.getX(), event.getY(), event.getButton(), true); }
      public void mouseReleased(MouseEvent event) { if (AwtPreviewWindow.this.listener != null) AwtPreviewWindow.this.listener.pointer(event.getX(), event.getY(), event.getButton(), false); }
    });
    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent event) { if (AwtPreviewWindow.this.listener != null) AwtPreviewWindow.this.listener.key(event.getKeyCode(), true, event.getModifiersEx()); }
      public void keyReleased(KeyEvent event) { if (AwtPreviewWindow.this.listener != null) AwtPreviewWindow.this.listener.key(event.getKeyCode(), false, event.getModifiersEx()); }
    });
  }

  public void showWindow() { window.setVisible(true); requestFocus(); }

  public void present(FrameData frame) {
    image = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_INT_ARGB);
    int[] target = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    System.arraycopy(frame.pixels(), 0, target, 0, target.length);
    setSize(frame.width(), frame.height());
    repaint();
  }

  @Override public void paint(Graphics graphics) {
    if (image != null) graphics.drawImage(image, 0, 0, getWidth(), getHeight(), null);
  }

  @Override public void close() { window.dispose(); }
}
