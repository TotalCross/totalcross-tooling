// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.preview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PreviewProcessTerminatorTest {
  @Test
  void requestsCooperativeShutdownBeforeDeletingTheDescriptor() throws Exception {
    Path directory = Files.createTempDirectory("totalcross-preview-stop-");
    Process process = launch(ControlAwareProcess.class, directory);
    try {
      awaitFile(directory.resolve("ready"));
      Path descriptor = descriptor(directory, process.pid());

      assertTrue(PreviewProcessTerminator.stop(descriptor, Duration.ofSeconds(2), Duration.ofSeconds(1)));

      assertTrue(process.waitFor(2, TimeUnit.SECONDS));
      assertFalse(process.isAlive());
      assertFalse(Files.exists(descriptor));
      assertTrue(Files.readString(directory.resolve("preview-control.txt")).contains("stop"));
    } finally {
      process.destroyForcibly();
      delete(directory);
    }
  }

  @Test
  void forciblyStopsAPreviewThatIgnoresCooperativeAndNormalTermination() throws Exception {
    Path directory = Files.createTempDirectory("totalcross-preview-force-stop-");
    Process process = launch(StubbornProcess.class, directory);
    try {
      awaitFile(directory.resolve("ready"));
      awaitFile(directory.resolve("child-pid"));
      long childPid = Long.parseLong(Files.readString(directory.resolve("child-pid")));
      Path descriptor = descriptor(directory, process.pid());

      assertTrue(PreviewProcessTerminator.stop(descriptor, Duration.ofMillis(50), Duration.ofMillis(500)));

      assertTrue(process.waitFor(2, TimeUnit.SECONDS));
      assertFalse(process.isAlive());
      assertFalse(ProcessHandle.of(childPid).map(ProcessHandle::isAlive).orElse(false));
      assertFalse(Files.exists(descriptor));
    } finally {
      process.destroyForcibly();
      delete(directory);
    }
  }

  private static Process launch(Class<?> mainClass, Path directory) throws Exception {
    String java = Path.of(System.getProperty("java.home"), "bin",
        System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java").toString();
    String classpath = Path.of(PreviewProcessTerminatorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
    return new ProcessBuilder(java, "-cp", classpath, mainClass.getName(), directory.toString())
        .redirectErrorStream(true).start();
  }

  private static Path descriptor(Path directory, long pid) throws Exception {
    Path descriptor = directory.resolve("preview-session.json");
    Files.writeString(descriptor, "{\"pid\":" + pid + "}", StandardCharsets.UTF_8);
    return descriptor;
  }

  private static void awaitFile(Path file) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!Files.isRegularFile(file) && System.nanoTime() < deadline) Thread.sleep(20);
    assertTrue(Files.isRegularFile(file), "child process did not become ready");
  }

  private static void delete(Path path) throws Exception {
    if (!Files.exists(path)) return;
    try (var paths = Files.walk(path)) {
      paths.sorted(java.util.Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
    }
  }

  public static final class ControlAwareProcess {
    public static void main(String[] args) throws Exception {
      Path directory = Path.of(args[0]);
      Path control = directory.resolve("preview-control.txt");
      Files.writeString(directory.resolve("ready"), "ready");
      while (!Files.isRegularFile(control) || !Files.readString(control).contains("stop")) Thread.sleep(20);
    }
  }

  public static final class StubbornProcess {
    public static void main(String[] args) throws Exception {
      String java = Path.of(System.getProperty("java.home"), "bin",
          System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java").toString();
      Process child = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
          SleepingProcess.class.getName()).start();
      Files.writeString(Path.of(args[0]).resolve("child-pid"), Long.toString(child.pid()));
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try { Thread.sleep(TimeUnit.SECONDS.toMillis(30)); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
      }));
      Files.writeString(Path.of(args[0]).resolve("ready"), "ready");
      while (true) Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }
  }

  public static final class SleepingProcess {
    public static void main(String[] args) throws Exception {
      while (true) Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }
  }
}
