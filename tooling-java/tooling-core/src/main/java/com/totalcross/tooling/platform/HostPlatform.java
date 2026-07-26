// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.platform;

import java.util.Locale;

/** Normalized operating system and CPU architecture for installation metadata. */
public record HostPlatform(OperatingSystem operatingSystem, Architecture architecture) {
    public enum OperatingSystem { MACOS, WINDOWS, LINUX }
    public enum Architecture { X86_64, ARM64 }

    public static HostPlatform detect() {
        return from(System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    public static HostPlatform from(String osName, String architecture) {
        String os = osName.toLowerCase(Locale.ROOT);
        OperatingSystem normalizedOs;
        if (os.startsWith("mac")) normalizedOs = OperatingSystem.MACOS;
        else if (os.startsWith("windows")) normalizedOs = OperatingSystem.WINDOWS;
        else if (os.startsWith("linux")) normalizedOs = OperatingSystem.LINUX;
        else throw new IllegalArgumentException("Unsupported operating system: " + osName);

        String cpu = architecture.toLowerCase(Locale.ROOT);
        Architecture normalizedCpu;
        if (cpu.equals("aarch64") || cpu.equals("arm64")) normalizedCpu = Architecture.ARM64;
        else if (cpu.equals("x86_64") || cpu.equals("amd64") || cpu.equals("x64")) normalizedCpu = Architecture.X86_64;
        else throw new IllegalArgumentException("Unsupported CPU architecture: " + architecture);
        return new HostPlatform(normalizedOs, normalizedCpu);
    }

    public String id() {
        return operatingSystem.name().toLowerCase(Locale.ROOT) + "-" + architecture.name().toLowerCase(Locale.ROOT);
    }
}
