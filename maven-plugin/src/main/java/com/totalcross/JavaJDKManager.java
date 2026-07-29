/*
 * Copyright (C) 2020-2021 TotalCross Global Mobile Platform Ltda.
 * Copyright (C) 2022-2026 Amalgam Solucoes em TI Ltda.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.totalcross;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class JavaJDKManager extends DownloadManager {
    private final String jdkVersion;

    public JavaJDKManager(String localRepositoryDir) {
        this(localRepositoryDir, "11");
    }

    public JavaJDKManager() {
        this("11", true);
    }

    public JavaJDKManager(String localRepositoryDir, String jdkVersion) {
        super(localRepositoryDir, "zulu_jdk_" + jdkVersion);
        this.jdkVersion = jdkVersion;
    }

    public static JavaJDKManager forVersion(String jdkVersion) {
        return new JavaJDKManager(jdkVersion, true);
    }

    private JavaJDKManager(String jdkVersion, boolean useDefaultRepository) {
        super("zulu_jdk_" + jdkVersion);
        this.jdkVersion = jdkVersion;
    }

    public void init() throws IOException {
        if (!verify()) {
            File archive = new File(getLocalRepositoryDir(), baseFolderName + ".zip");
            if (!archive.exists()) {
                download();
            }
            unzip();
        }
    }

    public void download() throws IOException {
        URLConnection connection = new URL(
                "https://api.azul.com/zulu/download/community/v1.0/bundles/latest/binary/?jdk_version=" + jdkVersion
                        + "&ext=zip&os=" + SYSTEM_OS + "&arch=x86&hw_bitness=" + SYSTEM_BITNESS).openConnection();
        long fileSize = connection.getContentLength();
        try (InputStream inputStream = connection.getInputStream()) {
            super.download("Download JDK " + jdkVersion, inputStream, fileSize);
        }
    }

    @Override
    protected void setPath(String path) {
        if (isMac) {
            path += "/Contents/Home";
        }
        super.setPath(path);
    }
}
