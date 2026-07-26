// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0
package com.totalcross.tooling.build;

public record BuildNotification(String kind, String message, long timestamp) {
  public static BuildNotification reload(String message) { return new BuildNotification("reload", message, System.currentTimeMillis()); }
}
