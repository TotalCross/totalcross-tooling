// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Calls tc.Deploy in a disposable classloader while keeping legacy static state isolated. */
public final class LegacyDeployService implements DeployService {
    private static final Object INVOCATION_LOCK = new Object();
    private final DeployToolchain toolchain;

    public LegacyDeployService(DeployToolchain toolchain) {
        this.toolchain = toolchain;
    }

    @Override
    public DeployResult deploy(DeployRequest request) {
        List<DeployDiagnostic> diagnostics = new ArrayList<>();
        List<Path> before = files(request.outputDirectory());
        List<String> arguments = arguments(request);
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        synchronized (INVOCATION_LOCK) {
            String previousProtoc = System.getProperty(DeployToolchain.PROTOC_PROPERTY);
            String previousBundletool = System.getProperty(DeployToolchain.BUNDLETOOL_PROPERTY);
            String previousUserDir = System.getProperty("user.dir");
            try (URLClassLoader loader = new URLClassLoader(urls(), null)) {
                configureAndroidTools(request, diagnostics);
                Class<?> deploy = Class.forName("tc.Deploy", true, loader);
                PrintStream previousErr = System.err;
                PrintStream previousOut = System.out;
                try (PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
                    System.setProperty("user.dir", request.sdkInstallation().toAbsolutePath().toString());
                    System.setOut(output);
                    System.setErr(output);
                    String[] deployArguments = arguments.toArray(String[]::new);
                    AtomicReference<Throwable> invocationFailure = new AtomicReference<>();
                    Thread invocation = new Thread(() -> {
                        try {
                            try {
                                deploy.getConstructor(String[].class).newInstance((Object) deployArguments);
                            } catch (NoSuchMethodException legacyMainOnly) {
                                deploy.getMethod("main", String[].class).invoke(null, (Object) deployArguments);
                            }
                        } catch (Throwable failure) {
                            invocationFailure.set(failure);
                        }
                    }, "totalcross-deploy");
                    invocation.setDaemon(true);
                    invocation.start();
                    invocation.join();
                    Throwable failure = invocationFailure.get();
                    if (failure != null) {
                        if (failure instanceof Exception exception) throw exception;
                        if (failure instanceof Error error) throw error;
                        throw new RuntimeException(failure);
                    }
                } finally {
                    System.setOut(previousOut);
                    System.setErr(previousErr);
                    if (previousUserDir == null) System.clearProperty("user.dir");
                    else System.setProperty("user.dir", previousUserDir);
                }
            } catch (InvocationTargetException failure) {
                diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.ERROR,
                        message(failure.getCause())));
                addCaptured(diagnostics, captured);
                return result(1, before, request, diagnostics, captured);
            } catch (Exception failure) {
                diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.ERROR, message(failure)));
                addCaptured(diagnostics, captured);
                return result(1, before, request, diagnostics, captured);
            } finally {
                restore(DeployToolchain.PROTOC_PROPERTY, previousProtoc);
                restore(DeployToolchain.BUNDLETOOL_PROPERTY, previousBundletool);
            }
        }
        addCaptured(diagnostics, captured);
        return result(0, before, request, diagnostics, captured);
    }

    private static void addCaptured(List<DeployDiagnostic> diagnostics, ByteArrayOutputStream captured) {
        if (captured.size() > 0) {
            diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.INFO,
                    captured.toString(StandardCharsets.UTF_8)));
        }
    }

    private void configureAndroidTools(DeployRequest request, List<DeployDiagnostic> diagnostics) throws java.io.IOException {
        if (!request.platforms().contains(DeployPlatform.ANDROID)) return;
        if ((toolchain.protoc() == null) != (toolchain.bundletool() == null)) {
            throw new java.io.IOException("Both shared Android deploy tools must be resolved together");
        }
        Path protoc = toolchain.protoc();
        Path bundletool = toolchain.bundletool();
        if (protoc == null) {
            var fallback = new LegacyAndroidToolFallback().find(request.sdkInstallation(), request.toolingJdk());
            if (fallback.isEmpty()) {
                throw new java.io.IOException("No verified shared Android tools are available and the explicit SDK has no valid legacy fallback");
            }
            protoc = fallback.get().protoc();
            bundletool = fallback.get().bundletool();
            diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.WARNING,
                    "Using Android tools from the selected SDK; this read-only fallback is deprecated. Install the shared tool store.") );
        }
        if (!Files.isRegularFile(protoc) || !Files.isRegularFile(bundletool)) {
            throw new java.io.IOException("Resolved Android deploy tools are missing");
        }
        System.setProperty(DeployToolchain.PROTOC_PROPERTY, protoc.toAbsolutePath().toString());
        System.setProperty(DeployToolchain.BUNDLETOOL_PROPERTY, bundletool.toAbsolutePath().toString());
    }

    private static void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }

    private static List<String> arguments(DeployRequest request) {
        List<String> arguments = new ArrayList<>();
        arguments.add(request.inputArtifact().toAbsolutePath().toString());
        request.platforms().forEach(platform -> arguments.add(platform.argument()));
        arguments.addAll(request.legacyOptions());
        if (request.logLevel() == DeployLogLevel.VERBOSE) arguments.add("/v");
        if (request.logLevel() == DeployLogLevel.DEBUG) arguments.addAll(List.of("/log-level", "debug"));
        return arguments;
    }

    private static DeployResult result(int code, List<Path> before, DeployRequest request,
                                       List<DeployDiagnostic> diagnostics, ByteArrayOutputStream ignored) {
        List<Path> after = files(request.outputDirectory());
        after.removeAll(before);
        return new DeployResult(code, after, diagnostics);
    }

    private static List<Path> files(Path root) {
        if (!Files.isDirectory(root)) return new ArrayList<>();
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).toList().stream().map(Path::toAbsolutePath).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (java.io.IOException ignored) {
            return new ArrayList<>();
        }
    }

    private static String message(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    private java.net.URL[] urls() throws java.io.IOException {
        java.net.URL[] urls = new java.net.URL[toolchain.artifacts().size()];
        for (int i = 0; i < urls.length; i++) urls[i] = toolchain.artifacts().get(i).toAbsolutePath().normalize().toUri().toURL();
        return urls;
    }
}
