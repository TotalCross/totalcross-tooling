// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import totalcross.ui.MainWindow;

class DisposableAppClassLoaderTest {
  @Test
  void loadsApplicationClassesFromConfiguredRoots() throws Exception {
    PreviewConfig config = new PreviewConfig();
    config.classOutputPaths.add("build/classes/java/test");

    try (DisposableAppClassLoader loader = DisposableAppClassLoader.fromConfig(Paths.get(".").toAbsolutePath(),
        config)) {
      Class<?> loaded = Class.forName("previewfixture.AppClass", true, loader);
      Object instance = loaded.getDeclaredConstructor().newInstance();

      assertSame(loader, loaded.getClassLoader());
      assertEquals("loaded", loaded.getMethod("value").invoke(instance));
    }
  }

  @Test
  void loadsTotalCrossSampleApplicationClassesFromConfiguredRoots() throws Exception {
    PreviewConfig config = new PreviewConfig();
    config.classOutputPaths.add("build/classes/java/test");

    try (DisposableAppClassLoader loader = DisposableAppClassLoader.fromConfig(Paths.get(".").toAbsolutePath(),
        config)) {
      Class<?> loaded = Class.forName("totalcross.sample.previewfixture.AppClass", true, loader);
      Object instance = loaded.getDeclaredConstructor().newInstance();

      assertSame(loader, loaded.getClassLoader());
      assertEquals("sample", loaded.getMethod("value").invoke(instance));
    }
  }

  @Test
  void delegatesTotalCrossRuntimeClassesToParent() throws Exception {
    PreviewConfig config = new PreviewConfig();

    try (DisposableAppClassLoader loader = DisposableAppClassLoader.fromConfig(Paths.get(".").toAbsolutePath(),
        config)) {
      Class<?> loaded = Class.forName(MainWindow.class.getName(), true, loader);

      assertSame(MainWindow.class, loaded);
      assertSame(MainWindow.class.getClassLoader(), loaded.getClassLoader());
    }
  }
}
