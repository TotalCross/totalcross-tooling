// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.jdk;

/** Supported immutable catalog archive layouts. */
public enum JdkArchiveType {
    ZIP("zip"),
    TAR_GZ("tar.gz");

    private final String id;

    JdkArchiveType(String id) { this.id = id; }

    public String id() { return id; }

    public static JdkArchiveType parse(String value) {
        for (JdkArchiveType type : values()) if (type.id.equals(value)) return type;
        throw new IllegalArgumentException("Unsupported JDK archive type: " + value);
    }
}
