// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.store;

/** Stable identity for one immutable installed artifact. */
public record ArtifactCoordinate(String kind, String name, String version, String build) {
    public ArtifactCoordinate {
        requireComponent(kind, "kind");
        requireComponent(name, "name");
        requireComponent(version, "version");
        requireComponent(build, "build");
    }

    public String id() {
        return name + "-" + version + "-" + build;
    }

    private static void requireComponent(String value, String field) {
        if (value == null || value.isBlank() || value.contains("/") || value.contains("\\") || value.equals("..")) {
            throw new IllegalArgumentException(field + " must be one safe path component");
        }
    }
}
