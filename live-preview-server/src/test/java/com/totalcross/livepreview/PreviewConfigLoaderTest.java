// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class PreviewConfigLoaderTest {
  @TempDir
  Path tempDir;

  @Test
  void parsesPreviewConfigJson() {
    PreviewConfig config = PreviewConfigLoader.parse("{\n"
        + "  \"mainWindow\": \"com.example.App\",\n"
        + "  \"launcherArgs\": [\"/scr\", \"320x480x32\"],\n"
        + "  \"buildCommand\": \"./gradlew classes\",\n"
        + "  \"classOutputPaths\": [\"build/classes/java/main\"],\n"
        + "  \"resourcePaths\": [\"src/main/resources\"],\n"
        + "  \"dependencyPaths\": [\"build/libs\", \"lib\"],\n"
        + "  \"previewMode\": \"windowed\",\n"
        + "  \"reloadMode\": \"fast\",\n"
        + "  \"width\": 320,\n"
        + "  \"height\": 480,\n"
        + "  \"scale\": 2,\n"
        + "  \"platform\": \"android\",\n"
        + "  \"headlessOutput\": \"build/preview.png\"\n"
        + "}\n");

    assertEquals("com.example.App", config.mainWindow);
    assertEquals(320, config.width);
    assertEquals(480, config.height);
    assertEquals(2, config.scale);
    assertArrayEquals(new String[] { "/scr", "320x480x32", "/scale", "2" }, config.toLauncherArgs());
  }

  @Test
  void convertsDefaultWidthHeightLauncherArgsToScreenArgument() {
    PreviewConfig config = PreviewConfig.defaults();

    assertArrayEquals(new String[] { "/scr", "500x600x32" }, config.toLauncherArgs());
  }

  @Test
  void createsDefaultConfigFile() throws Exception {
    Path configPath = tempDir.resolve("config").resolve(PreviewConfigLoader.DEFAULT_FILE_NAME);

    PreviewConfig created = PreviewConfigLoader.createDefault(configPath);
    PreviewConfig loaded = PreviewConfigLoader.load(configPath);

    assertEquals(true, Files.exists(configPath));
    assertArrayEquals(created.toLauncherArgs(), loaded.toLauncherArgs());
    assertEquals(created.buildCommand, loaded.buildCommand);
    assertEquals(created.headlessOutput, loaded.headlessOutput);
  }
}
