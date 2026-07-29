// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Legacy HTTP/Webview adapter retained for compatibility fixtures.
 * New integrations must use tooling-java's authenticated host/worker lifecycle;
 * this server is not a second supported reload owner.
 */
public class LivePreviewServer {
  private static final String DEFAULT_HOST = "127.0.0.1";
  private static final int MAX_REQUEST_BODY_BYTES = 64 * 1024;
  private static final Pattern SHOW_REQUEST = Pattern.compile(
      "\\{\\s*\\\"className\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"\\s*\\}");

  public static void main(String[] args) throws Exception {
    Config cli = Config.parse(args);
    PreviewConfig previewConfig;
    Path workspaceRoot;
    Path configPath = null;
    if (cli.configPath != null) {
      configPath = Paths.get(cli.configPath).toAbsolutePath().normalize();
      previewConfig = PreviewConfigLoader.load(configPath);
      workspaceRoot = configPath.getParent() == null ? Paths.get(".").toAbsolutePath().normalize()
          : configPath.getParent();
    } else {
      previewConfig = PreviewConfig.defaults();
      previewConfig.mainWindow = cli.className;
      previewConfig.launcherArgs = cli.launcherArgs;
      workspaceRoot = Paths.get(".").toAbsolutePath().normalize();
    }

    if (previewConfig.mainWindow == null || previewConfig.mainWindow.length() == 0) {
      printUsage();
      System.exit(2);
    }

    final PreviewRunner runner = PreviewRunner.run(previewConfig, workspaceRoot);
    final Path reloadConfigPath = configPath;
    final HttpServer server = HttpServer.create(new InetSocketAddress(cli.address, cli.port), 0);
    final CountDownLatch stopped = new CountDownLatch(1);
    server.createContext("/health", exchange -> writeJson(exchange, runner));
    server.createContext("/frame", exchange -> writeFrame(exchange, runner));
    server.createContext("/show", exchange -> writeShow(exchange, runner));
    server.createContext("/clear", exchange -> writeClear(exchange, runner));
    server.createContext("/reload", exchange -> writeReload(exchange, runner, reloadConfigPath));
    server.createContext("/shutdown", exchange -> writeShutdown(exchange, runner, server, stopped));
    server.setExecutor(null);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      runner.stop();
      server.stop(0);
    }));
    server.start();

    InetSocketAddress address = server.getAddress();
    System.out.println("TOTALCROSS_PREVIEW_URL=http://" + cli.address.getHostAddress() + ":" + address.getPort());
    System.out.flush();
    stopped.await();
    runner.stop();
    // The desktop launcher leaves AWT worker threads alive after its runtime is closed.
    // This executable owns that process, so terminate only after the shutdown response
    // has been written and all preview resources have been released.
    System.exit(0);
  }

  private static void writeJson(HttpExchange exchange, PreviewRunner runner) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    runner.pumpEvents();
    String json = "{\"ok\":true,\"mainWindow\":\"" + escapeJson(runner.getMainWindowClass()) + "\",\"frameNumber\":"
        + runner.getFrameNumber() + "}";
    writeBytes(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeFrame(HttpExchange exchange, PreviewRunner runner) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    runner.pumpEvents();
    BufferedImage frame = runner.getLatestFrame();
    if (frame == null) {
      writeBytes(exchange, 503, "text/plain; charset=utf-8", "No frame available".getBytes(StandardCharsets.UTF_8));
      return;
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(frame, "png", output);
    Headers headers = exchange.getResponseHeaders();
    headers.set("Cache-Control", "no-store, no-cache, must-revalidate");
    headers.set("Pragma", "no-cache");
    headers.set("X-TotalCross-Frame", String.valueOf(runner.getFrameNumber()));
    writeBytes(exchange, 200, "image/png", output.toByteArray());
  }

  private static void writeReload(HttpExchange exchange, PreviewRunner runner, Path configPath) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    boolean reloaded = configPath == null ? runner.reload() : runner.reload(PreviewConfigLoader.load(configPath));
    String json = "{\"ok\":" + reloaded + ",\"mainWindow\":\"" + escapeJson(runner.getMainWindowClass())
        + "\",\"frameNumber\":" + runner.getFrameNumber() + ",\"error\":\""
        + escapeJson(runner.getLastReloadError()) + "\"}";
    writeBytes(exchange, reloaded ? 200 : 500, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeShow(HttpExchange exchange, PreviewRunner runner) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    String request = readRequestBody(exchange);
    String className = showRequestClassName(request);
    boolean shown = runner.showClass(className);
    String json = "{\"ok\":" + shown + ",\"className\":\"" + escapeJson(className)
        + "\",\"frameNumber\":" + runner.getFrameNumber() + ",\"error\":\""
        + escapeJson(runner.getLastShowError()) + "\"}";
    writeBytes(exchange, shown ? 200 : 422, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeClear(HttpExchange exchange, PreviewRunner runner) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    runner.clearPreview();
    String json = "{\"ok\":true,\"frameNumber\":" + runner.getFrameNumber() + "}";
    writeBytes(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
  }

  private static void writeShutdown(HttpExchange exchange, PreviewRunner runner, HttpServer server, CountDownLatch stopped)
      throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      writeBytes(exchange, 405, "text/plain; charset=utf-8", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
      return;
    }
    writeBytes(exchange, 200, "application/json; charset=utf-8", "{\"ok\":true}".getBytes(StandardCharsets.UTF_8));
    new Thread(() -> {
      server.stop(0);
      stopped.countDown();
      runner.stop();
    }).start();
  }

  private static void writeBytes(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
    Headers headers = exchange.getResponseHeaders();
    headers.set("Content-Type", contentType);
    headers.set("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(status, body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  private static String escapeJson(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static String readRequestBody(HttpExchange exchange) throws IOException {
    InputStream input = exchange.getRequestBody();
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int read;
    while ((read = input.read(buffer)) >= 0) {
      if (output.size() + read > MAX_REQUEST_BODY_BYTES) {
        throw new IOException("Request body exceeds 64 KiB");
      }
      output.write(buffer, 0, read);
    }
    return new String(output.toByteArray(), StandardCharsets.UTF_8);
  }

  private static String showRequestClassName(String json) {
    Matcher match = SHOW_REQUEST.matcher(json);
    if (!match.matches()) {
      return "";
    }
    return match.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
  }

  private static void printUsage() {
    System.err.println(
        "Usage: java com.totalcross.livepreview.LivePreviewServer --config totalcross.preview.json [--host 127.0.0.1] [--port 0]");
    System.err.println(
        "   or: java com.totalcross.livepreview.LivePreviewServer --class <MainWindowClass> [--host 127.0.0.1] [--port 0] [-- <launcher args>]");
  }

  private static class Config {
    private String host = DEFAULT_HOST;
    private InetAddress address;
    private int port;
    private String className;
    private String configPath;
    private List<String> launcherArgs = new ArrayList<>();

    private static Config parse(String[] args) throws IOException {
      Config config = new Config();
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if ("--".equals(arg)) {
          while (++i < args.length) {
            config.launcherArgs.add(args[i]);
          }
          break;
        } else if ("--host".equals(arg)) {
          config.host = args[++i];
        } else if ("--port".equals(arg)) {
          config.port = Integer.parseInt(args[++i]);
        } else if ("--class".equals(arg)) {
          config.className = args[++i];
        } else if ("--config".equals(arg)) {
          config.configPath = args[++i];
        } else if ("--windowed".equals(arg) || "--watch".equals(arg) || "--headless".equals(arg)) {
          // Accepted by PreviewRunner; ignored by the HTTP service.
        } else if ("--output".equals(arg)) {
          i++;
        } else if (config.className == null && config.configPath == null) {
          config.className = arg;
        } else {
          config.launcherArgs.add(arg);
        }
      }
      if (config.port < 0 || config.port > 65535) {
        throw new IllegalArgumentException("port must be between 0 and 65535");
      }
      config.address = InetAddress.getByName(config.host);
      if (!config.address.isLoopbackAddress()) {
        throw new IllegalArgumentException("host must resolve to a loopback address");
      }
      return config;
    }
  }
}
