// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.platform;

import java.nio.file.Path;
import java.util.Map;

/** Resolves native data and cache roots without requiring a vendor-specific SDK path. */
public record StoreLayout(HostPlatform platform, Path dataRoot, Path cacheRoot) {
    public StoreLayout {
        dataRoot = dataRoot.toAbsolutePath().normalize();
        cacheRoot = cacheRoot.toAbsolutePath().normalize();
    }

    public static StoreLayout detect() {
        HostPlatform platform = HostPlatform.detect();
        return fromSystem(platform, System.getenv(), System.getProperty("user.home"));
    }

    public static StoreLayout fromSystem(HostPlatform platform, Map<String, String> environment, String userHome) {
        Path home = Path.of(userHome);
        return switch (platform.operatingSystem()) {
            case MACOS -> new StoreLayout(platform,
                    home.resolve("Library/Application Support/TotalCross"),
                    home.resolve("Library/Caches/TotalCross"));
            case WINDOWS -> {
                Path local = Path.of(environment.getOrDefault("LOCALAPPDATA", home.resolve("AppData/Local").toString()));
                yield new StoreLayout(platform, local.resolve("TotalCross"), local.resolve("TotalCross/Cache"));
            }
            case LINUX -> {
                Path data = Path.of(environment.getOrDefault("XDG_DATA_HOME", home.resolve(".local/share").toString()));
                Path cache = Path.of(environment.getOrDefault("XDG_CACHE_HOME", home.resolve(".cache").toString()));
                yield new StoreLayout(platform, data.resolve("TotalCross"), cache.resolve("TotalCross"));
            }
        };
    }

    public Path installationRoot(String kind, String coordinate) {
        if (kind.isBlank() || coordinate.isBlank() || kind.contains("/") || coordinate.contains("/")) {
            throw new IllegalArgumentException("Installation names must be non-empty path components");
        }
        return dataRoot.resolve(kind).resolve(coordinate);
    }

    public Path externalToolRoot(String name, String version, String build) {
        if (name.isBlank() || version.isBlank() || build.isBlank()
                || name.contains("/") || version.contains("/") || build.contains("/")) {
            throw new IllegalArgumentException("External tool coordinates must be safe path components");
        }
        return dataRoot.resolve("tools").resolve(name).resolve(version).resolve(build);
    }

    public Path stagingRoot() {
        return cacheRoot.resolve("staging");
    }
}
