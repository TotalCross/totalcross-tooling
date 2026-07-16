import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.totalcross.JavaJDKManager;

import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class JavaJDKManagerTest {
    private static String repoTestDir;
    private static JavaJDKManager javaJDKManager;

    static {
        try {
            repoTestDir = Files.createTempDirectory("TotalCross_TestRepo_").toAbsolutePath().toString();
            javaJDKManager = new JavaJDKManager(repoTestDir);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Failed to create temporary directory for testing");
        }
    }

    @AfterAll
    static void wipeTest() {
        try {
            FileUtils.deleteDirectory(javaJDKManager.getLocalRepositoryDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void usesTheSameAzulBinaryEndpointAsTheGradleResolver() {
        String url = javaJDKManager.downloadUrl();
        assertEquals(true, url.startsWith("https://api.azul.com/zulu/download/community/v1.0/bundles/latest/binary/?"));
        assertEquals(true, url.contains("jdk_version=11"));
        assertEquals(true, url.contains("ext=zip&os=" + JavaJDKManager.SYSTEM_OS));
        assertEquals(true, url.contains("arch=x86&hw_bitness=64"));
        assertEquals(true, url.contains("crac_supported=false"));
    }

    @Test
    void downloadAndUnzip() {
        try {
            javaJDKManager.init();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File jdkDir = javaJDKManager.getPath();

        assertEquals(true, jdkDir.exists(), "JDK dir should exist");
        assertEquals(true, jdkDir.isDirectory(), "JDK dir should be a directory");
        assertEquals(true, new File(jdkDir, "bin").exists(), "bin dir should be a exists");
        assertEquals(true, new File(jdkDir, "lib").exists(), "lib dir should be a exists");
        assertEquals(false, new File(jdkDir, "tempjdk.zip").exists(), "tempjdk.zip should have been deleted");
    }
}
