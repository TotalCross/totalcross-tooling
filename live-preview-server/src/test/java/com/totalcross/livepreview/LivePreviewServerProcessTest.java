// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LivePreviewServerProcessTest {
  @TempDir
  Path tempDir;

  @Test
  void servesHealthFrameAndShutdownFromTheBuiltRuntime() throws Exception {
    int port = freePort();
    Path configPath = tempDir.resolve(PreviewConfigLoader.DEFAULT_FILE_NAME);
    Path testClasses = Path.of(System.getProperty("user.dir"), "build", "classes", "java", "test");
    Files.writeString(configPath, "{\n"
        + "  \"mainWindow\": \"previewfixture.PreviewMainWindow\",\n"
        + "  \"launcherArgs\": [\"width\", \"120\", \"height\", \"180\"],\n"
        + "  \"classOutputPaths\": [\"" + jsonPath(testClasses) + "\"],\n"
        + "  \"resourcePaths\": [],\n"
        + "  \"dependencyPaths\": [],\n"
        + "  \"width\": 120,\n"
        + "  \"height\": 180\n"
        + "}\n", StandardCharsets.UTF_8);

    Path output = tempDir.resolve("preview-server.log");
    Process process = new ProcessBuilder(javaCommand(), "-cp", System.getProperty("java.class.path"),
        "com.totalcross.livepreview.LivePreviewServer", "--config", configPath.toString(), "--host", "127.0.0.1", "--port",
        String.valueOf(port)).redirectErrorStream(true).redirectOutput(output.toFile()).start();
    String baseUrl = "http://127.0.0.1:" + port;
    try {
      assertTrue(waitForHealth(baseUrl, process), () -> "LivePreviewServer did not start:\n" + readOutput(output));
      assertTrue(get(baseUrl + "/health").contains("\"ok\":true"));
      assertEquals(200, status(baseUrl + "/frame"));
      assertEquals(200, status(baseUrl + "/shutdown", "POST"));
      assertTrue(process.waitFor(10, TimeUnit.SECONDS), () -> "LivePreviewServer did not stop:\n" + readOutput(output));
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
        process.waitFor(10, TimeUnit.SECONDS);
      }
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  private static String javaCommand() {
    return Path.of(System.getProperty("java.home"), "bin", "java").toString();
  }

  private static boolean waitForHealth(String baseUrl, Process process) throws Exception {
    for (int attempt = 0; attempt < 80; attempt++) {
      if (!process.isAlive()) {
        return false;
      }
      try {
        if (status(baseUrl + "/health") == 200) {
          return true;
        }
      } catch (IOException ignored) {
      }
      Thread.sleep(125);
    }
    return false;
  }

  private static String get(String url) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);
    assertEquals(200, connection.getResponseCode());
    return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static int status(String url) throws IOException {
    return status(url, "GET");
  }

  private static int status(String url, String method) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod(method);
    connection.setConnectTimeout(1000);
    connection.setReadTimeout(1000);
    return connection.getResponseCode();
  }

  private static String jsonPath(Path path) {
    return path.toString().replace("\\", "\\\\");
  }

  private static String readOutput(Path output) {
    try {
      return Files.readString(output, StandardCharsets.UTF_8);
    } catch (IOException error) {
      return error.toString();
    }
  }
}
