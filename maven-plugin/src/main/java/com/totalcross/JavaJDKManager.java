package com.totalcross;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class JavaJDKManager extends DownloadManager {
    private static final String JDK_VERSION = "11";
    private static final String BUNDLE_API = "https://api.azul.com/zulu/download/community/v1.0/bundles/latest/binary/?";

    public JavaJDKManager(String localRepositoryDir) {
        super(localRepositoryDir, "zulu_jdk_11");
    }

    public JavaJDKManager() {
        super("zulu_jdk_11");
    }

    public void init() throws IOException {
        if (!verify()) {
            download();
            unzip();
        }
    }

    public void download() throws IOException {
        URLConnection connection = new URL(downloadUrl()).openConnection();
        long fileSize = connection.getContentLength();
        try (InputStream inputStream = connection.getInputStream()) {
            super.download("Download JDK " + JDK_VERSION, inputStream, fileSize);
        }
    }

    public String downloadUrl() {
        return BUNDLE_API + "jdk_version=" + JDK_VERSION + "&ext=zip&os=" + SYSTEM_OS
                + "&arch=x86&hw_bitness=64&crac_supported=false";
    }

    @Override
    protected void setPath(String path) {
        if (isMac) {
            /* 
                java unzip doesn't support symbolic links, 
                but it's easy enough for us to just append 
                the Contents/Home whatever 
            */
            path += "/Contents/Home";
        }
        super.setPath(path);
    }
}
