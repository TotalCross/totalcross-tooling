// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling.download;

import java.io.IOException;

public interface Downloader {
    void download(DownloadRequest request) throws IOException;
}
