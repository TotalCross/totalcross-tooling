<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: LGPL-2.1-only
-->

# TotalCross Live Preview Server

This LGPL-2.1-only Java 17 project provides the local HTTP service consumed by
TotalCross IDE integrations. It is separate from `totalcross-sdk` and depends
on an SDK version exposing the `totalcross.preview.PreviewRuntime` contract.

Build a local distribution with:

    ./gradlew installDist -PtotalcrossSdkVersion=<sdk-version>

Point `totalcross.livePreview.extraClasspath` at
`build/install/totalcross-live-preview-server/lib`. The server starts on
loopback and prints `TOTALCROSS_PREVIEW_URL` for IDE clients.
