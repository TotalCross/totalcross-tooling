// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.cli;

import com.totalcross.tooling.host.*;
import com.totalcross.tooling.host.AwtPreviewWindow;
import com.totalcross.tooling.build.ProjectModel;
import com.totalcross.tooling.build.ProjectModelCodec;
import com.totalcross.tooling.preview.PreviewConfiguration;
import com.totalcross.tooling.jdk.JdkCatalogResolver;
import com.totalcross.tooling.jdk.JdkRequest;
import com.totalcross.tooling.jdk.JdkInstallation;
import com.totalcross.tooling.worker.PreviewWorkerMain;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.GraphicsEnvironment;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Standalone preview entry point using the authenticated host/worker lifecycle. */
public final class ToolingCli {
  private ToolingCli() {}

  /** Returns the jars/directories needed when a build tool forks this CLI. */
  public static String runtimeClasspath() throws Exception {
    LinkedHashSet<String> locations = new LinkedHashSet<>();
    for (Class<?> component : List.of(ToolingCli.class,
        ProjectModelCodec.class,
        com.totalcross.tooling.conversion.inference.ProjectConversionAnalyzer.class,
        com.totalcross.tooling.host.PreviewHost.class,
        com.totalcross.tooling.worker.PreviewWorkerMain.class,
        com.totalcross.tooling.protocol.ProtocolCodec.class)) {
      locations.add(codeSource(component));
    }
    if (locations.isEmpty()) throw new IllegalStateException("Unable to resolve the TotalCross tooling runtime classpath");
    return String.join(File.pathSeparator, locations);
  }

  private static String codeSource(Class<?> component) {
    try {
      if (component.getProtectionDomain() == null || component.getProtectionDomain().getCodeSource() == null
          || component.getProtectionDomain().getCodeSource().getLocation() == null) {
        throw new IllegalStateException("code source is unavailable");
      }
      return Path.of(component.getProtectionDomain().getCodeSource().getLocation().toURI())
          .toAbsolutePath().normalize().toString();
    } catch (Exception error) {
      throw new IllegalStateException("Unable to resolve code source for " + component.getName()
          + "; run the CLI with published tooling modules or exploded module outputs", error);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0 || "--help".equals(args[0])) { usage(); return; }
    if ("convert-project".equals(args[0])) { ConvertProjectCommand.execute(args); return; }
    if ("stop".equals(args[0])) { stop(args); return; }
    if (!List.of("preview", "run").contains(args[0])) throw new IllegalArgumentException("unknown command: " + args[0]);
    boolean runWindow = "run".equals(args[0]);
    Path session = optionPath(args, "--session", null);
    Path modelFile = optionPath(args, "--model", session == null ? null : session.resolveSibling("project-model.json"));
    ProjectModel model = readProjectModel(modelFile);
    Path project = optionPath(args, "--project", null);
    if (project == null) project = model == null ? (session == null ? Path.of(".") : session.getParent()) : model.project();
    if (model == null && session != null) project = sessionProject(session, project);
    if (!Files.isDirectory(project)) throw new IllegalArgumentException("project does not exist: " + project);
    Path configFile = optionPath(args, "--config", project.resolve("totalcross-preview.json"));
    PreviewConfiguration configuration = PreviewConfiguration.read(configFile, project);
    String mainClass = option(args, "--main", null);
    if ((mainClass == null || mainClass.isBlank()) && model != null) mainClass = model.mainClass();
    if (configuration.mainWindow() != null && !configuration.mainWindow().isBlank() && option(args, "--main", null) == null) {
      mainClass = configuration.mainWindow();
    }
    if ((mainClass == null || mainClass.isBlank()) && session != null) mainClass = sessionMainClass(session);
    if (mainClass == null || mainClass.isBlank()) mainClass = discoverMainClass(project);
    String explicitClasspath = option(args, "--classpath", null);
    if ((explicitClasspath == null || explicitClasspath.isBlank()) && model != null) explicitClasspath = modelClasspath(model);
    String classpath = applicationClasspath(project, explicitClasspath, configuration.classpath());
    boolean once = has(args, "--once");
    Path frameFile = optionPath(args, "--frame-file", null);
    Path controlFile = optionPath(args, "--control-file", null);
    Path selectionFile = frameFile == null ? null : frameFile.resolveSibling("preview-selection.json");
    if (selectionFile != null) Files.deleteIfExists(selectionFile);
    Path configuredJdk = optionPath(args, "--jdk-path", null);
    JdkInstallation toolingJdk = JdkCatalogResolver.production().resolve(
        new JdkRequest("17", configuredJdk, null));
    Path outputProject = project;
    String outputMainClass = mainClass;
    try (PreviewReloadCoordinator coordinator = new PreviewReloadCoordinator(Duration.ofSeconds(15))) {
      List<String> worker = workerCommand(toolingJdk.home(), !runWindow);
      String[] sessionRoot = {mainClass};
      Object frameAccess = new Object();
      if (!coordinator.reload(() -> candidate(worker, classpath, sessionRoot[0],
          configuration.launcherArgs().toArray(String[]::new)))) {
        throw new IllegalStateException("preview worker did not start: " + coordinator.lastFailure());
      }
      AwtPreviewWindow window = runWindow ? runWindow(coordinator, mainClass) : null;
      try (ControlLoop controls = controlFile == null ? null : new ControlLoop(coordinator, controlFile,
          (nextMainClass, reloadArgs) -> {
            synchronized (frameAccess) {
              boolean reloaded = coordinator.reload(() -> candidate(worker, classpath, nextMainClass,
                  reloadArgs.length == 0 && !configuration.launcherArgs().isEmpty()
                      ? configuration.launcherArgs().toArray(String[]::new) : reloadArgs));
              if (reloaded) sessionRoot[0] = nextMainClass;
              return reloaded;
            }
          },
          selectedClass -> {
            synchronized (frameAccess) {
              boolean selected = coordinator.reload(() -> selectedCandidate(
                  worker, classpath, sessionRoot[0], selectedClass));
              String selectionError = coordinator.lastFailure();
              if (selected && frameFile != null) {
                com.totalcross.tooling.protocol.FrameData selectedFrame = coordinator.nextFrame(Duration.ofSeconds(1));
                if (selectedFrame == null) {
                  selected = false;
                  selectionError = "selected preview did not provide its promoted frame";
                } else {
                  writeFrame(frameFile, selectedFrame);
                }
              }
              writeSelection(selectionFile, selectedClass, selected, selectionError);
              return selected;
            }
          },
          error -> emit("error", outputProject, outputMainClass, error))) {
        if (controls != null) controls.start();
      emitStarted(project, mainClass);
      boolean frame = false;
      while (coordinator.state() != com.totalcross.tooling.host.PreviewSessionState.CLOSED) {
        com.totalcross.tooling.protocol.FrameData next;
        synchronized (frameAccess) {
          next = coordinator.nextFrame(Duration.ofMillis(250));
          if (next != null && frameFile != null) writeFrame(frameFile, next);
        }
        if (next != null) {
          frame = true;
          if (window != null) window.present(next);
          emit("frame", project, mainClass, null, frameFile == null ? null : frameFile.toString());
          if (once) break;
        }
      }
      if (!frame && once) {
        String detail = coordinator.lastFailure().isBlank() ? "preview worker closed before its first frame" :
            "preview worker closed before its first frame: " + coordinator.lastFailure();
        emit("error", project, mainClass, detail);
        throw new IllegalStateException(detail);
      }
      if (window != null) window.close();
      emit("stopped", project, mainClass, null);
      }
    }
  }

  static AwtPreviewWindow runWindow(PreviewReloadCoordinator coordinator, String mainClass) {
    if (GraphicsEnvironment.isHeadless()) throw new IllegalStateException("TotalCross run requires a graphical desktop");
    AwtPreviewWindow window = new AwtPreviewWindow("TotalCross Run — " + mainClass, new AwtPreviewWindow.InputListener() {
      public void pointer(int x, int y, int button, boolean pressed) { try { coordinator.pointer(x, y, button, pressed); } catch (Exception ignored) { } }
      public void key(int keyCode, boolean pressed, int modifiers) { try { coordinator.key(keyCode, pressed, modifiers); } catch (Exception ignored) { } }
    });
    window.showWindow();
    return window;
  }

  private static ProcessWorkerCandidate candidate(List<String> worker, String classpath, String mainClass, String... args)
      throws IOException {
    return new ProcessWorkerCandidate(worker, classpath, mainClass, args);
  }

  private static ProcessWorkerCandidate selectedCandidate(List<String> worker, String classpath, String mainClass,
      String selectedClass) throws IOException {
    return new ProcessWorkerCandidate(worker, classpath, mainClass, selectedClass, new String[0]);
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

  private static String applicationClasspath(Path project, String explicit, List<Path> configured) throws IOException {
    List<Path> entries = new ArrayList<>();
    if (explicit != null && !explicit.isBlank()) {
      for (String value : explicit.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
        if (!value.isBlank()) entries.add(Path.of(value));
      }
    }
    entries.addAll(configured);
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

  static ProjectModel readProjectModel(Path file) throws IOException {
    if (file == null || !Files.isRegularFile(file)) return null;
    try { return new ProjectModelCodec().fromJson(Files.readString(file)); }
    catch (IllegalArgumentException error) { throw new IOException("Invalid project model: " + file + ": " + error.getMessage(), error); }
  }

  static String modelClasspath(ProjectModel model) {
    List<Path> entries = new ArrayList<>();
    entries.add(model.classOutput().path());
    model.resourceRoots().stream().map(root -> root.path()).forEach(entries::add);
    entries.addAll(model.dependencies().entries());
    return entries.stream().map(Path::toString).distinct().reduce((left, right) -> left + File.pathSeparator + right).orElse("");
  }

  static String javaExecutable(Path home) {
    String name = System.getProperty("os.name", "").toLowerCase().startsWith("windows") ? "java.exe" : "java";
    return home.resolve("bin").resolve(name).toString();
  }

  static List<String> workerCommand(Path toolingJdk, boolean headless) throws Exception {
    List<String> command = new ArrayList<>();
    command.add(javaExecutable(toolingJdk));
    if (headless) command.add("-Djava.awt.headless=true");
    command.addAll(List.of("-cp", runtimeClasspath(), PreviewWorkerMain.class.getName()));
    return List.copyOf(command);
  }

  private static Path sessionProject(Path session, Path fallback) throws IOException {
    String json = Files.readString(session);
    Matcher matcher = Pattern.compile("\\\"project\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(json);
    return matcher.find() ? Path.of(matcher.group(1).replace("\\\\", "\\")).toAbsolutePath().normalize() : fallback;
  }

  private static String sessionMainClass(Path session) throws IOException {
    Matcher matcher = Pattern.compile("\\\"mainClass\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").matcher(Files.readString(session));
    return matcher.find() ? matcher.group(1).replace("\\\\", "\\") : null;
  }

  private static void writeFrame(Path file, com.totalcross.tooling.protocol.FrameData frame) throws IOException {
    BufferedImage image = new BufferedImage(frame.width(), frame.height(), BufferedImage.TYPE_INT_ARGB);
    int[] pixels = frame.pixels();
    image.setRGB(0, 0, frame.width(), frame.height(), pixels, 0, frame.stride());
    Path parent = file.toAbsolutePath().normalize().getParent();
    if (parent != null && !Files.isDirectory(parent)) Files.createDirectories(parent);
    ImageIO.write(image, "png", file.toFile());
  }

  private static void writeSelection(Path file, String className, boolean selected, String error) throws IOException {
    if (file == null) return;
    Path parent = file.toAbsolutePath().normalize().getParent();
    if (parent != null && !Files.isDirectory(parent)) Files.createDirectories(parent);
    String value = "{\"className\":\"" + escape(className) + "\",\"selected\":" + selected
        + ",\"error\":\"" + escape(error == null ? "" : error) + "\"}";
    Files.writeString(file, value, StandardCharsets.UTF_8);
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
    emit(event, project, mainClass, error, null);
  }

  private static void emit(String event, Path project, String mainClass, String error, String frameFile) {
    String value = "{\"event\":\"" + escape(event) + "\",\"project\":\"" + escape(project.toString())
        + "\",\"mainClass\":\"" + escape(mainClass) + "\",\"error\":\"" + escape(error == null ? "" : error)
        + "\",\"frameFile\":\"" + escape(frameFile == null ? "" : frameFile) + "\"}";
    System.out.println(value);
  }

  private static void emitStarted(Path project, String mainClass) {
    String value = "{\"event\":\"started\",\"project\":\"" + escape(project.toString())
        + "\",\"mainClass\":\"" + escape(mainClass)
        + "\",\"error\":\"\",\"frameFile\":\"\",\"selectionSupported\":true}";
    System.out.println(value);
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
  }

  private static void usage() {
    System.out.println("usage: totalcross-tooling preview|run [--model <file>] [--project <path>] [--config <file>] [--session <file>] [--main <class>] [--classpath <path>] [--jdk-path <home>] [--frame-file <png>] [--control-file <file>] [--once] | stop --pid <pid> | stop --session <file> | convert-project analyze --project <path> [--plan <file>]");
  }

  private static final class ControlLoop implements AutoCloseable {
    @FunctionalInterface interface ReloadHandler { boolean reload(String mainClass, String... args); }
    @FunctionalInterface interface ShowHandler { boolean show(String className) throws Exception; }
    private final PreviewReloadCoordinator coordinator;
    private final Path file;
    private final ReloadHandler reload;
    private final ShowHandler show;
    private final java.util.function.Consumer<String> errors;
    private volatile boolean running = true;
    private int consumed;
    private Thread thread;

    ControlLoop(PreviewReloadCoordinator coordinator, Path file, ReloadHandler reload, ShowHandler show,
        java.util.function.Consumer<String> errors) throws IOException {
      this.coordinator = coordinator;
      this.file = file.toAbsolutePath().normalize();
      this.reload = reload;
      this.show = show;
      this.errors = errors;
      Path parent = this.file.getParent();
      if (parent != null && !Files.isDirectory(parent)) Files.createDirectories(parent);
      if (!Files.exists(this.file)) Files.createFile(this.file);
      consumed = Files.readAllLines(this.file, StandardCharsets.UTF_8).size();
    }

    void start() {
      thread = new Thread(this::poll, "totalcross-preview-control");
      thread.setDaemon(true);
      thread.start();
    }

    private void poll() {
      while (running) {
        try {
          List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
          while (consumed < lines.size()) dispatch(lines.get(consumed++));
          Thread.sleep(100);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          return;
        } catch (Exception failure) {
          errors.accept(failure.getMessage() == null ? failure.getClass().getName() : failure.getMessage());
        }
      }
    }

    private void dispatch(String line) throws Exception {
      if (line == null || line.isBlank()) return;
      String[] values = line.trim().split(",", -1);
      switch (values[0]) {
        case "resize" -> coordinator.resize(Integer.parseInt(values[1]), Integer.parseInt(values[2]), Double.parseDouble(values[3]));
        case "pointer" -> coordinator.pointer(Integer.parseInt(values[1]), Integer.parseInt(values[2]),
            Integer.parseInt(values[3]), Boolean.parseBoolean(values[4]));
        case "key" -> coordinator.key(Integer.parseInt(values[1]), Boolean.parseBoolean(values[2]), Integer.parseInt(values[3]));
        case "reload" -> {
          if (!reload.reload(values[1], java.util.Arrays.copyOfRange(values, 2, values.length))) {
            throw new IOException("preview reload failed: " + coordinator.lastFailure());
          }
        }
        case "show" -> {
          if (values.length < 2 || values[1].isBlank()) throw new IOException("preview selection requires a class name");
          if (!show.show(values[1])) throw new IOException("preview selection failed: " + coordinator.lastFailure());
        }
        case "stop" -> coordinator.close();
        default -> { }
      }
    }

    @Override public void close() {
      running = false;
      if (thread != null) thread.interrupt();
    }
  }
}
