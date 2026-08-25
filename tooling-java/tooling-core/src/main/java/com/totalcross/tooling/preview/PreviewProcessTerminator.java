// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.preview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stops the coordinator and every worker recorded by a preview session descriptor. */
public final class PreviewProcessTerminator {
  private static final Pattern PID = Pattern.compile("\"pid\"\\s*:\\s*(\\d+)");
  private static final Duration COOPERATIVE_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(2);

  private PreviewProcessTerminator() {}

  /** Returns true when an existing descriptor was consumed. */
  public static boolean stop(Path descriptor) throws IOException {
    return stop(descriptor, COOPERATIVE_TIMEOUT, TERMINATION_TIMEOUT);
  }

  static boolean stop(Path descriptor, Duration cooperativeTimeout, Duration terminationTimeout) throws IOException {
    if (!Files.isRegularFile(descriptor)) return false;
    Matcher matcher = PID.matcher(Files.readString(descriptor, StandardCharsets.UTF_8));
    if (!matcher.find()) {
      Files.deleteIfExists(descriptor);
      return true;
    }

    ProcessHandle coordinator = ProcessHandle.of(Long.parseLong(matcher.group(1))).orElse(null);
    if (coordinator != null && coordinator.isAlive()) {
      Map<Long, ProcessHandle> tree = snapshot(coordinator);
      requestCooperativeStop(descriptor.resolveSibling("preview-control.txt"));
      if (!awaitStopped(tree.values(), cooperativeTimeout)) {
        merge(tree, coordinator);
        signal(tree.values(), false);
        if (!awaitStopped(tree.values(), terminationTimeout)) {
          merge(tree, coordinator);
          signal(tree.values(), true);
          if (!awaitStopped(tree.values(), terminationTimeout)) {
            throw new IOException("Unable to stop TotalCross preview process tree rooted at PID " + coordinator.pid());
          }
        }
      }
    }
    Files.deleteIfExists(descriptor);
    return true;
  }

  private static Map<Long, ProcessHandle> snapshot(ProcessHandle coordinator) {
    Map<Long, ProcessHandle> tree = new LinkedHashMap<>();
    merge(tree, coordinator);
    return tree;
  }

  private static void merge(Map<Long, ProcessHandle> tree, ProcessHandle coordinator) {
    try { coordinator.descendants().forEach(handle -> tree.putIfAbsent(handle.pid(), handle)); }
    catch (RuntimeException ignored) { /* The coordinator may have exited while its tree was being sampled. */ }
    tree.putIfAbsent(coordinator.pid(), coordinator);
  }

  private static void requestCooperativeStop(Path control) {
    try {
      Files.writeString(control, "stop\n", StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException ignored) {
      // A missing or unwritable control file falls back to process termination below.
    }
  }

  private static void signal(Iterable<ProcessHandle> tree, boolean force) {
    List<ProcessHandle> handles = new ArrayList<>();
    tree.forEach(handles::add);
    for (ProcessHandle handle : handles) {
      if (!handle.isAlive()) continue;
      try {
        if (force) handle.destroyForcibly();
        else handle.destroy();
      } catch (RuntimeException ignored) {
        // The bounded wait below decides whether termination actually succeeded.
      }
    }
  }

  private static boolean awaitStopped(Iterable<ProcessHandle> tree, Duration timeout) throws IOException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (true) {
      boolean alive = false;
      for (ProcessHandle handle : tree) alive |= handle.isAlive();
      if (!alive) return true;
      if (System.nanoTime() >= deadline) return false;
      try { Thread.sleep(25); }
      catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while stopping TotalCross preview", interrupted);
      }
    }
  }
}
