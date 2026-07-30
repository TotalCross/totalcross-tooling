<!--
Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
SPDX-License-Identifier: LGPL-2.1-only
-->

# TotalCross Live Preview Server

This LGPL-2.1-only Java 17 project provides the local HTTP service consumed by
older TotalCross IDE integrations. The supported release lifecycle is the
authenticated host/worker coordinator in `tooling-java`; this server is not a
second supported reload owner. It is separate from `totalcross-sdk` and depends
on an SDK version exposing the `totalcross.preview.PreviewRuntime` contract.
That SDK contract is internal compatibility only. New integrations use the
versioned project model and the coordinator's frame/control protocol instead.

Build a local distribution with:

    ./gradlew installDist -PtotalcrossSdkVersion=<sdk-version>

Point the legacy client classpath at
`build/install/totalcross-live-preview-server/lib`. The server starts on
loopback and prints `TOTALCROSS_PREVIEW_URL` for compatibility clients.
