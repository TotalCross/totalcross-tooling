// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tc.simulator.Launcher;

class ReflectiveRuntimeBridgeTest {
  @Test
  void presentsEachSupportedTotalCrossUiType() {
    ReflectiveRuntimeBridge bridge = new ReflectiveRuntimeBridge(getClass().getClassLoader());
    List<com.totalcross.tooling.protocol.FrameData> frames = new ArrayList<>();
    bridge.start("previewfixture.PreviewMainWindow", new String[0], frames::add);

    bridge.show("previewfixture.PreviewMainWindow");
    assertEquals("main-window", Launcher.last.action);
    assertTrue(Launcher.last.prepared);

    bridge.show("previewfixture.PreviewContainer");
    assertEquals("container", Launcher.last.action);

    bridge.show("previewfixture.PreviewControl");
    assertEquals("control", Launcher.last.action);
    assertEquals(4, frames.size());
    assertEquals(3, Launcher.last.pumpCount);
  }

  @Test
  void rejectsUnsupportedClassesAndClassesWithoutDefaultConstructors() {
    ReflectiveRuntimeBridge bridge = new ReflectiveRuntimeBridge(getClass().getClassLoader());
    bridge.start("previewfixture.PreviewMainWindow", new String[0], ignored -> { });

    IllegalArgumentException unsupported = assertThrows(IllegalArgumentException.class,
        () -> bridge.show("previewfixture.UnsupportedPreviewType"));
    assertTrue(unsupported.getMessage().contains("MainWindow, Container, or Control"));

    IllegalArgumentException missingConstructor = assertThrows(IllegalArgumentException.class,
        () -> bridge.show("previewfixture.NoDefaultConstructorControl"));
    assertTrue(missingConstructor.getMessage().contains("no-argument constructor"));
  }
}
