/*
 * Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross.tooling.conversion.java;

import com.totalcross.tooling.conversion.inventory.ProjectInventory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative Java source and resource classification for a conversion dry run. */
public final class JavaSourceClassifier {
  private static final Pattern PACKAGE = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*)\\s*;");
  private static final Pattern MAIN_WINDOW = Pattern.compile(
      "(?m)\\bpublic\\s+(?!abstract\\b)class\\s+([A-Za-z_$][\\w$]*)\\s+extends\\s+(?:[\\w$.]*\\.)?MainWindow\\b");

  public Classification classify(ProjectInventory inventory) throws IOException {
    List<Source> main = new ArrayList<>(), tests = new ArrayList<>(), resources = new ArrayList<>();
    List<MainWindowCandidate> mainWindows = new ArrayList<>(), warnings = new ArrayList<>();
    for (ProjectInventory.Entry entry : inventory.entries()) {
      Path relative = entry.relativePath();
      String name = relative.getFileName().toString();
      if (!name.endsWith(".java")) {
        if (resourceLike(relative)) resources.add(new Source(relative, relative, false));
        continue;
      }
      String source = Files.readString(inventory.root().resolve(relative));
      boolean test = isTest(relative, name, source);
      String packageName = packageName(source);
      Path destination = destination(relative, name, packageName, test);
      Source classified = new Source(relative, destination, test);
      (test ? tests : main).add(classified);
      if (packageName == null) warnings.add(new MainWindowCandidate(relative, name.substring(0, name.length() - 5), false,
          "default-package source; it will be preserved and may not be portable"));
      Matcher candidates = MAIN_WINDOW.matcher(source);
      while (candidates.find()) mainWindows.add(new MainWindowCandidate(relative, qualified(packageName, candidates.group(1)), true,
          "concrete class directly extends totalcross.ui.MainWindow"));
    }
    return new Classification(main, tests, resources, mainWindows, warnings);
  }

  private static boolean resourceLike(Path relative) {
    String normalized = relative.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("resource") || normalized.startsWith("assets/") || normalized.startsWith("res/");
  }

  private static boolean isTest(Path relative, String name, String source) {
    String normalized = relative.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("/test/") || normalized.startsWith("test/") || normalized.startsWith("tests/")
        || name.endsWith("Test.java") || name.endsWith("Tests.java") || source.contains("org.junit.")
        || source.contains("org.testng.") || source.contains("@Test");
  }

  private static String packageName(String source) {
    Matcher matcher = PACKAGE.matcher(source);
    return matcher.find() ? matcher.group(1) : null;
  }

  private static Path destination(Path source, String name, String packageName, boolean test) {
    Path root = Path.of("src", test ? "test" : "main", "java");
    return packageName == null ? root.resolve(name) : root.resolve(packageName.replace('.', '/')).resolve(name);
  }

  private static String qualified(String packageName, String className) {
    return packageName == null ? className : packageName + "." + className;
  }

  public record Source(Path source, Path destination, boolean test) { }
  public record MainWindowCandidate(Path source, String className, boolean selectable, String evidence) { }
  public record Classification(List<Source> mainSources, List<Source> testSources, List<Source> resources,
      List<MainWindowCandidate> mainWindowCandidates, List<MainWindowCandidate> warnings) {
    public Classification { mainSources = List.copyOf(mainSources); testSources = List.copyOf(testSources);
      resources = List.copyOf(resources); mainWindowCandidates = List.copyOf(mainWindowCandidates); warnings = List.copyOf(warnings); }
  }
}
