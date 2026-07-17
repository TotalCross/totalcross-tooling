// Copyright (C) 2026 Amalgam Solucoes em TI Ltda
//
// SPDX-License-Identifier: LGPL-2.1-only
package com.totalcross.livepreview;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;

import totalcross.preview.PreviewRuntime;
import totalcross.ui.Container;
import totalcross.ui.Control;
import totalcross.ui.MainWindow;

/**
 * Starts a TotalCross application with an IDE-agnostic preview surface.
 * <p>
 * IDE integrations should use this entry point to obtain rendered frames
 * without depending on Applet painting or AWT windows. The runner owns the
 * launcher lifecycle and can rebuild the application side through a disposable
 * ClassLoader between reloads.
 */
public class PreviewRunner {
  private PreviewRuntime runtime;
  private final HeadlessPngSurface surface;
  private PreviewConfig config;
  private final Path workspaceRoot;
  private DisposableAppClassLoader appClassLoader;
  private DisposableAppClassLoader displayedClassLoader;
  private String lastReloadError = "";
  private String lastShowError = "";
  private boolean blank;

  private PreviewRunner(PreviewRuntime runtime, HeadlessPngSurface surface, PreviewConfig config, Path workspaceRoot,
      DisposableAppClassLoader appClassLoader) {
    this.runtime = runtime;
    this.surface = surface;
    this.config = config;
    this.workspaceRoot = workspaceRoot;
    this.appClassLoader = appClassLoader;
  }

  public static PreviewRunner run(Class<? extends MainWindow> clazz, String... args) {
    if (clazz == null) {
      throw new IllegalArgumentException("clazz cannot be null");
    }
    return run(clazz.getCanonicalName(), args);
  }

  public static PreviewRunner run(String className, String... args) {
    if (className == null || className.length() == 0) {
      throw new IllegalArgumentException("className cannot be empty");
    }
    PreviewConfig config = PreviewConfig.defaults();
    config.mainWindow = className;
    config.launcherArgs.clear();
    for (String arg : args) {
      config.launcherArgs.add(arg);
    }
    try {
      return run(config, Paths.get(".").toAbsolutePath().normalize(), false);
    } catch (IOException e) {
      throw new IllegalStateException("Could not start preview", e);
    }
  }

  public static PreviewRunner run(PreviewConfig config, Path workspaceRoot) throws IOException {
    return run(config, workspaceRoot, true);
  }

  private static PreviewRunner run(PreviewConfig config, Path workspaceRoot, boolean useDisposableClassLoader)
      throws IOException {
    if (config.mainWindow == null || config.mainWindow.length() == 0) {
      throw new IllegalArgumentException("mainWindow cannot be empty");
    }
    HeadlessPngSurface surface = new HeadlessPngSurface();
    DisposableAppClassLoader loader = useDisposableClassLoader
        ? DisposableAppClassLoader.fromConfig(workspaceRoot, config)
        : null;
    PreviewRuntime runtime = startRuntime(config, surface, loader);
    return new PreviewRunner(runtime, surface, config, workspaceRoot, loader);
  }

  public synchronized boolean reload() {
    return reload(config);
  }

  public synchronized boolean reload(PreviewConfig config) {
    if (config == null || config.mainWindow == null || config.mainWindow.length() == 0) {
      lastReloadError = "mainWindow cannot be empty";
      return false;
    }
    DisposableAppClassLoader newClassLoader = null;
    try {
      lastReloadError = "";
      blank = false;
      this.config = config;
      newClassLoader = DisposableAppClassLoader.fromConfig(workspaceRoot, this.config);
      closeClassLoader(displayedClassLoader);
      displayedClassLoader = null;
      runtime.preparePreviewMainWindowReload();
      MainWindow mainWindow = runtime.createMainWindow(this.config.mainWindow, newClassLoader, false);
      if (mainWindow == null) {
        throw new IllegalStateException("Preview mainWindow did not create a MainWindow instance");
      }
      DisposableAppClassLoader oldClassLoader = appClassLoader;
      appClassLoader = newClassLoader;
      newClassLoader = null;
      runtime.replaceMainWindow(mainWindow, currentCommandLine());
      closeClassLoader(oldClassLoader);
      pumpEvents();
      return true;
    } catch (Throwable e) {
      lastReloadError = stackTrace(e);
      e.printStackTrace();
      closeClassLoader(newClassLoader);
      return false;
    }
  }

  public String getMainWindowClass() {
    return config == null ? "" : config.mainWindow;
  }

  public String getLastReloadError() {
    return lastReloadError;
  }

  public synchronized boolean showClass(String className) {
    if (className == null || className.length() == 0) {
      lastShowError = "className cannot be empty";
      return false;
    }
    DisposableAppClassLoader newClassLoader = null;
    try {
      lastShowError = "";
      newClassLoader = DisposableAppClassLoader.fromConfig(workspaceRoot, config);
      Class<?> previewClass = Class.forName(className, true, newClassLoader);
      Constructor<?> constructor = previewClass.getDeclaredConstructor();
      if (MainWindow.class.isAssignableFrom(previewClass)) {
        closeClassLoader(displayedClassLoader);
        displayedClassLoader = null;
        runtime.preparePreviewMainWindowReload();
        MainWindow mainWindow = (MainWindow) constructor.newInstance();
        DisposableAppClassLoader oldClassLoader = appClassLoader;
        appClassLoader = newClassLoader;
        newClassLoader = null;
        runtime.replaceMainWindow(mainWindow, currentCommandLine());
        closeClassLoader(oldClassLoader);
      } else if (Container.class.isAssignableFrom(previewClass)) {
        Container container = (Container) constructor.newInstance();
        runtime.showContainer(container);
        DisposableAppClassLoader oldClassLoader = displayedClassLoader;
        displayedClassLoader = newClassLoader;
        newClassLoader = null;
        closeClassLoader(oldClassLoader);
      } else if (Control.class.isAssignableFrom(previewClass)) {
        Control control = (Control) constructor.newInstance();
        runtime.showControl(control);
        DisposableAppClassLoader oldClassLoader = displayedClassLoader;
        displayedClassLoader = newClassLoader;
        newClassLoader = null;
        closeClassLoader(oldClassLoader);
      } else {
        lastShowError = className
            + " does not extend totalcross.ui.MainWindow, totalcross.ui.Container or totalcross.ui.Control";
        closeClassLoader(newClassLoader);
        return false;
      }
      blank = false;
      pumpEvents();
      return true;
    } catch (NoSuchMethodException e) {
      lastShowError = className + " does not have a default constructor";
      closeClassLoader(newClassLoader);
      return false;
    } catch (Throwable e) {
      lastShowError = stackTrace(e);
      e.printStackTrace();
      closeClassLoader(newClassLoader);
      return false;
    }
  }

  public String getLastShowError() {
    return lastShowError;
  }

  public synchronized void clearPreview() {
    blank = true;
    closeClassLoader(displayedClassLoader);
    displayedClassLoader = null;
    int width = config == null || config.width <= 0 ? 1 : config.width;
    int height = config == null || config.height <= 0 ? 1 : config.height;
    BufferedImage latestFrame = surface.getLatestFrame();
    if (latestFrame != null) {
      width = latestFrame.getWidth();
      height = latestFrame.getHeight();
    }
    surface.clear(width, height);
  }

  public HeadlessPreviewSurface getSurface() {
    return surface;
  }

  public BufferedImage getLatestFrame() {
    return surface.getLatestFrame();
  }

  public long getFrameNumber() {
    return surface.getFrameNumber();
  }

  public void writeLatestFrame(Path outputPath) throws IOException {
    surface.writeLatestFrame(outputPath);
  }

  public void pumpEvents() {
    if (!blank && runtime != null) {
      runtime.pumpEvents();
    }
  }

  public synchronized void stop() {
    stopRuntime();
  }

  public static void main(String[] args) throws Exception {
    Cli cli = Cli.parse(args);
    if (cli.configPath == null) {
      printUsage();
      System.exit(2);
      return;
    }
    if (!cli.headless) {
      LivePreviewServer.main(args);
      return;
    }
    Path configPath = Paths.get(cli.configPath).toAbsolutePath().normalize();
    PreviewConfig config = PreviewConfigLoader.load(configPath);
    Path workspaceRoot = configPath.getParent() == null ? Paths.get(".").toAbsolutePath().normalize()
        : configPath.getParent();
    if (cli.outputPath != null) {
      config.headlessOutput = cli.outputPath;
    }
    PreviewRunner runner = PreviewRunner.run(config, workspaceRoot);
    runner.pumpEvents();
    runner.writeLatestFrame(workspaceRoot.resolve(config.headlessOutput).normalize());
    runner.stop();
  }

  private static PreviewRuntime startRuntime(PreviewConfig config, HeadlessPreviewSurface surface,
      ClassLoader appClassLoader) {
    return PreviewRuntime.startPreview(config.mainWindow, surface, appClassLoader, config.toLauncherArgs());
  }

  private void stopRuntime() {
    if (runtime != null) {
      runtime.close();
      runtime = null;
    }
    closeClassLoader(appClassLoader);
    appClassLoader = null;
    closeClassLoader(displayedClassLoader);
    displayedClassLoader = null;
    MainWindow.resetPreviewState();
  }

  private String currentCommandLine() {
    return "";
  }

  private static void closeClassLoader(DisposableAppClassLoader classLoader) {
    if (classLoader != null) {
      try {
        classLoader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static String stackTrace(Throwable throwable) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    throwable.printStackTrace(new PrintStream(output));
    return output.toString();
  }

  private static void printUsage() {
    System.err.println("Usage: java totalcross.PreviewRunner --config totalcross.preview.json --headless [--output path]");
    System.err.println("   or: java totalcross.PreviewRunner --config totalcross.preview.json --windowed --watch");
  }

  private static class Cli {
    private String configPath;
    private String outputPath;
    private boolean headless;

    private static Cli parse(String[] args) {
      Cli cli = new Cli();
      for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        if ("--config".equals(arg)) {
          cli.configPath = args[++i];
        } else if ("--output".equals(arg)) {
          cli.outputPath = args[++i];
        } else if ("--headless".equals(arg)) {
          cli.headless = true;
        } else if ("--windowed".equals(arg) || "--watch".equals(arg)) {
          cli.headless = false;
        }
      }
      return cli;
    }
  }
}
