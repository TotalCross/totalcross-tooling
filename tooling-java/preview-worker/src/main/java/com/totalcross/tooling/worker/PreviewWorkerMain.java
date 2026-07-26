// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.worker;

import java.net.Socket;
import java.nio.file.Path;

/** Child entry point: arguments are port, token, and an optional application classpath. */
public final class PreviewWorkerMain {
  private PreviewWorkerMain() {}

  public static void main(String[] args) throws Exception {
    if (args.length < 2) throw new IllegalArgumentException("usage: worker <port> <token> [classpath]");
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try (Socket socket = new Socket("127.0.0.1", Integer.parseInt(args[0]))) {
      new PreviewWorkerSession(socket, args[1], new ReflectiveRuntimeBridge(loader)).run();
    }
  }
}
