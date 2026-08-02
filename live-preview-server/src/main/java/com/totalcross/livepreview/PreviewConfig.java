// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Project-level live preview configuration loaded from totalcross-preview.json.
 */
public class PreviewConfig {
  public String mainWindow = "";
  public List<String> launcherArgs = new ArrayList<>();
  public String buildCommand = "";
  public List<String> classOutputPaths = new ArrayList<>();
  public List<String> resourcePaths = new ArrayList<>();
  public List<String> dependencyPaths = new ArrayList<>();
  public String previewMode = "windowed";
  public String reloadMode = "fast";
  public int width = 500;
  public int height = 600;
  public int scale = 1;
  public String platform = "android";
  public String headlessOutput = "build/totalcross-preview/preview.png";

  public static PreviewConfig defaults() {
    PreviewConfig config = new PreviewConfig();
    config.launcherArgs.add("width");
    config.launcherArgs.add("500");
    config.launcherArgs.add("height");
    config.launcherArgs.add("600");
    config.classOutputPaths.add("build/classes/java/main");
    config.resourcePaths.add("src/main/resources");
    config.dependencyPaths.add("build/libs");
    config.dependencyPaths.add("lib");
    return config;
  }

  public String[] toLauncherArgs() {
    List<String> args = new ArrayList<>();
    boolean hasScreen = false;
    boolean hasScale = false;
    int effectiveWidth = width;
    int effectiveHeight = height;

    for (int i = 0; i < launcherArgs.size(); i++) {
      String arg = launcherArgs.get(i);
      if ("/scr".equalsIgnoreCase(arg)) {
        hasScreen = true;
        args.add(arg);
        if (i + 1 < launcherArgs.size()) {
          args.add(launcherArgs.get(++i));
        }
      } else if ("width".equalsIgnoreCase(arg) && i + 1 < launcherArgs.size()) {
        effectiveWidth = parseInt(launcherArgs.get(++i), effectiveWidth);
      } else if ("height".equalsIgnoreCase(arg) && i + 1 < launcherArgs.size()) {
        effectiveHeight = parseInt(launcherArgs.get(++i), effectiveHeight);
      } else if ("/scale".equalsIgnoreCase(arg)) {
        hasScale = true;
        args.add(arg);
        if (i + 1 < launcherArgs.size()) {
          args.add(launcherArgs.get(++i));
        }
      } else {
        args.add(arg);
      }
    }

    if (!hasScreen) {
      args.add(0, effectiveWidth + "x" + effectiveHeight + "x32");
      args.add(0, "/scr");
    }
    if (!hasScale && scale != 1) {
      args.add("/scale");
      args.add(String.valueOf(scale));
    }
    return args.toArray(new String[0]);
  }

  public List<String> appClasspathRoots() {
    List<String> paths = new ArrayList<>();
    paths.addAll(nullToEmpty(classOutputPaths));
    paths.addAll(nullToEmpty(resourcePaths));
    paths.addAll(nullToEmpty(dependencyPaths));
    return paths;
  }

  private List<String> nullToEmpty(List<String> values) {
    return values == null ? Collections.<String>emptyList() : values;
  }

  private static int parseInt(String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (RuntimeException e) {
      return defaultValue;
    }
  }
}
