// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Child-first loader for application bytecode used by preview reloads.
 */
public class DisposableAppClassLoader extends URLClassLoader {
  private static final String[] PARENT_FIRST_EXACT_CLASSES = {
      "totalcross.MainClass",
      "totalcross.Launcher",
      "totalcross.LauncherApplet",
      "tc.preview.PreviewBootstrap",
      "tc.preview.PreviewFrame",
      "tc.preview.PreviewFrameSink",
      "tc.preview.PreviewSession",
      "tc.simulator.Launcher",
      "com.totalcross.livepreview.PreviewRunner",
      "totalcross.TCEventThread",
      "totalcross.TotalCrossApplication",
  };

  private static final String[] PARENT_FIRST_PREFIXES = {
      "java.",
      "javax.",
      "sun.",
      "com.sun.",
      "jdk.",
      "jdkcompat.",
      "jdkcompatx.",
      "tc.",
      "tc.preview.",
      "tc.simulator.",
      "totalcross.barcode.",
      "totalcross.crypto.",
      "totalcross.db.",
      "totalcross.firebase.",
      "totalcross.game.",
      "totalcross.io.",
      "totalcross.json.",
      "totalcross.lang.",
      "totalcross.map.",
      "totalcross.money.",
      "totalcross.net.",
      "totalcross.notification.",
      "totalcross.phone.",
      "com.totalcross.livepreview.",
      "totalcross.profiling.",
      "totalcross.qrcode.",
      "totalcross.res.",
      "totalcross.sql.",
      "totalcross.sys.",
      "totalcross.telephony.",
      "totalcross.ui.",
      "totalcross.unit.",
      "totalcross.util.",
      "totalcross.xml.",
  };

  public DisposableAppClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
  }

  public static DisposableAppClassLoader fromConfig(Path workspaceRoot, PreviewConfig config) throws IOException {
    return new DisposableAppClassLoader(toUrls(workspaceRoot, config), DisposableAppClassLoader.class.getClassLoader());
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (mustDelegateToParent(name)) {
      return super.loadClass(name, resolve);
    }
    synchronized (getClassLoadingLock(name)) {
      Class<?> loaded = findLoadedClass(name);
      if (loaded == null) {
        try {
          loaded = findClass(name);
        } catch (ClassNotFoundException e) {
          loaded = super.loadClass(name, false);
        }
      }
      if (resolve) {
        resolveClass(loaded);
      }
      return loaded;
    }
  }

  private static URL[] toUrls(Path workspaceRoot, PreviewConfig config) throws IOException {
    List<URL> urls = new ArrayList<>();
    for (String entry : config.appClasspathRoots()) {
      Path path = workspaceRoot.resolve(entry).normalize();
      addPath(urls, path);
    }
    return urls.toArray(new URL[0]);
  }

  private static void addPath(List<URL> urls, Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }
    urls.add(path.toUri().toURL());
    File file = path.toFile();
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (File child : children) {
          if (child.isFile() && child.getName().endsWith(".jar")) {
            urls.add(child.toURI().toURL());
          }
        }
      }
    }
  }

  private static boolean mustDelegateToParent(String name) {
    for (int i = 0; i < PARENT_FIRST_EXACT_CLASSES.length; i++) {
      if (PARENT_FIRST_EXACT_CLASSES[i].equals(name)) {
        return true;
      }
    }
    for (int i = 0; i < PARENT_FIRST_PREFIXES.length; i++) {
      if (name.startsWith(PARENT_FIRST_PREFIXES[i])) {
        return true;
      }
    }
    return false;
  }
}
