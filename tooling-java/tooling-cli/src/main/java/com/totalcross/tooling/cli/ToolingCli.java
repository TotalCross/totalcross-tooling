// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.cli;

import com.totalcross.tooling.host.PreviewHost;
import com.totalcross.tooling.protocol.MessageType;
import com.totalcross.tooling.protocol.ProtocolMessage;
import com.totalcross.tooling.worker.PreviewWorkerMain;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Standalone preview entry point using the authenticated host/worker lifecycle. */
public final class ToolingCli {
  private ToolingCli() {}

  /** Returns the jars/directories needed when a build tool forks this CLI. */
  public static String runtimeClasspath() throws Exception {
    List<Class<?>> components = List.of(ToolingCli.class,
        com.totalcross.tooling.host.PreviewHost.class,
        com.totalcross.tooling.worker.PreviewWorkerMain.class,
        com.totalcross.tooling.protocol.ProtocolCodec.class);
    return components.stream().map(type -> {
      try { return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(); }
      catch (Exception error) { throw new IllegalStateException("Unable to locate tooling component", error); }
    }).distinct().reduce((left, right) -> left + File.pathSeparator + right).orElseThrow();
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || "--help".equals(args[0])) { usage(); return; }
    if ("stop".equals(args[0])) { stop(args); return; }
    if (!List.of("preview", "run").contains(args[0])) throw new IllegalArgumentException("unknown command: " + args[0]);
    Path project = optionPath(args, "--project");
    if (!Files.isDirectory(project)) throw new IllegalArgumentException("project does not exist: " + project);
    String mainClass = option(args, "--main", null);
    if (mainClass == null || mainClass.isBlank()) mainClass = discoverMainClass(project);
    String classpath = applicationClasspath(project, option(args, "--classpath", null));
    boolean once = has(args, "--once");
    try (PreviewHost host = new PreviewHost()) {
      List<String> worker = List.of(javaExecutable(), "-cp", System.getProperty("java.class.path"),
          PreviewWorkerMain.class.getName());
      host.launchWorker(worker, classpath);
      host.sendStart(mainClass);
      emit("started", project, mainClass, null);
      boolean frame = false;
      ProtocolMessage message;
      while ((message = host.session().receive()) != null) {
        if (message.type() == MessageType.FRAME) {
          frame = true;
          emit("frame", project, mainClass, null);
          if (once) break;
        } else if (message.type() == MessageType.ERROR) {
          String error = new String(message.payload(), StandardCharsets.UTF_8);
          emit("error", project, mainClass, error);
          throw new IllegalStateException(error);
        } else if (message.type() == MessageType.CLOSED) break;
      }
      if (!frame && once) throw new IllegalStateException("preview worker closed before its first frame");
      emit("stopped", project, mainClass, null);
    }
  }

  private static Path optionPath(String[] args, String name) { return optionPath(args, name, Path.of(".")); }

  private static Path optionPath(String[] args, String name, Path fallback) {
    String value = option(args, name, null);
    return value == null ? (fallback == null ? null : fallback.toAbsolutePath().normalize())
        : Path.of(value).toAbsolutePath().normalize();
  }

  private static String option(String[] args, String name, String fallback) {
    for (int i = 1; i + 1 < args.length; i++) if (name.equals(args[i])) return args[i + 1];
    return fallback;
  }

  private static boolean has(String[] args, String value) {
    for (String arg : args) if (value.equals(arg)) return true;
    return false;
  }

  private static String discoverMainClass(Path project) throws IOException {
    Path[] outputs = {project.resolve("build/classes/java/main"), project.resolve("target/classes")};
    for (Path output : outputs) if (Files.isDirectory(output)) {
      try (Stream<Path> paths = Files.walk(output)) {
        List<String> candidates = paths.filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().equals("MainWindow.class"))
            .map(output::relativize).map(path -> path.toString().replace(File.separatorChar, '.'))
            .map(path -> path.substring(0, path.length() - 6)).toList();
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() > 1) throw new IOException("More than one MainWindow.class was found: " + candidates);
      }
    }
    throw new IOException("Cannot discover MainWindow.class; pass --main <class>");
  }

  private static String applicationClasspath(Path project, String explicit) throws IOException {
    List<Path> entries = new ArrayList<>();
    if (explicit != null && !explicit.isBlank()) {
      for (String value : explicit.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
        if (!value.isBlank()) entries.add(Path.of(value));
      }
    }
    entries.add(project.resolve("build/classes/java/main"));
    entries.add(project.resolve("build/resources/main"));
    entries.add(project.resolve("target/classes"));
    for (Path directory : List.of(project.resolve("build/libs"), project.resolve("target"), project.resolve("lib"))) {
      if (Files.isDirectory(directory)) try (Stream<Path> paths = Files.list(directory)) {
        paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".jar")).forEach(entries::add);
      }
    }
    return entries.stream().filter(Files::exists).map(path -> path.toAbsolutePath().normalize().toString())
        .distinct().reduce((left, right) -> left + File.pathSeparator + right).orElseThrow();
  }

  private static String javaExecutable() {
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java";
    return Path.of(System.getProperty("java.home"), "bin", name).toString();
  }

  private static void stop(String[] args) throws IOException {
    long pid = 0;
    String value = option(args, "--pid", null);
    if (value == null) {
      Path session = optionPath(args, "--session", null);
      if (session != null && Files.isRegularFile(session)) {
        Matcher matcher = Pattern.compile("\\\"pid\\\"\\s*:\\s*(\\d+)").matcher(Files.readString(session));
        if (matcher.find()) pid = Long.parseLong(matcher.group(1));
      }
    } else pid = Long.parseLong(value);
    if (pid <= 0) throw new IllegalArgumentException("stop requires --pid <pid> or --session <descriptor>");
    ProcessHandle.of(pid).ifPresent(handle -> {
      handle.descendants().forEach(child -> { child.destroy(); if (child.isAlive()) child.destroyForcibly(); });
      handle.destroy();
      if (handle.isAlive()) handle.destroyForcibly();
    });
    emit("stopped", Path.of("."), "", null);
  }

  private static void emit(String event, Path project, String mainClass, String error) {
    String value = "{\"event\":\"" + escape(event) + "\",\"project\":\"" + escape(project.toString())
        + "\",\"mainClass\":\"" + escape(mainClass) + "\",\"error\":\"" + escape(error == null ? "" : error) + "\"}";
    System.out.println(value);
  }

  private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

  private static void usage() {
    System.out.println("usage: totalcross-tooling preview|run --project <path> [--main <class>] [--classpath <path>] [--once] | stop --pid <pid> | stop --session <file>");
  }
}
