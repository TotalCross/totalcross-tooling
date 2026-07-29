// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Child entry point: arguments are port, token, and an optional application classpath. */
public final class PreviewWorkerMain {
  private PreviewWorkerMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 2) throw new IllegalArgumentException("usage: worker <port> <token> [classpath]");
    List<URL> urls = new ArrayList<>();
    if (args.length >= 3 && !args[2].isBlank()) {
      for (String entry : args[2].split(java.util.regex.Pattern.quote(File.pathSeparator))) {
        urls.add(Path.of(entry).toAbsolutePath().normalize().toUri().toURL());
      }
    }
    ClassLoader parent = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader applicationLoader = new URLClassLoader(urls.toArray(URL[]::new), parent);
         Socket socket = new Socket("127.0.0.1", Integer.parseInt(args[0]))) {
      new PreviewWorkerSession(socket, args[1], new ReflectiveRuntimeBridge(applicationLoader)).run();
    }
  }
}
