// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.deploy;

public enum DeployPlatform {
    WIN32("-win32"), LINUX("-linux"), MACOS("-macos"), ANDROID("-android"), IOS("-ios"),
    LINUX_ARM("-linux_arm");

    private final String argument;

    DeployPlatform(String argument) { this.argument = argument; }

    public String argument() { return argument; }
}
