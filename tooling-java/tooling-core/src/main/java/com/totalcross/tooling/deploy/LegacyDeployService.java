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
            try (URLClassLoader loader = new URLClassLoader(urls(), null)) {
                Class<?> deploy = Class.forName("tc.Deploy", true, loader);
                var constructor = deploy.getConstructor(String[].class);
                PrintStream previous = System.err;
                try (PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
                    System.setErr(output);
                    constructor.newInstance((Object) arguments.toArray(String[]::new));
                } finally {
                    System.setErr(previous);
                }
            } catch (InvocationTargetException failure) {
                diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.ERROR,
                        message(failure.getCause())));
                return result(1, before, request, diagnostics, captured);
            } catch (Exception failure) {
                diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.ERROR, message(failure)));
                return result(1, before, request, diagnostics, captured);
            }
        }
        if (captured.size() > 0) diagnostics.add(new DeployDiagnostic(DeployDiagnostic.Severity.INFO, captured.toString(StandardCharsets.UTF_8)));
        return result(0, before, request, diagnostics, captured);
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
