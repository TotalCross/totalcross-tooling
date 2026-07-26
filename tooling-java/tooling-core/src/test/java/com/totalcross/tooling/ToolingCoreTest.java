// Copyright (C) 2026 Amalgam Solucoes em TI Ltda.
// SPDX-License-Identifier: Apache-2.0

package com.totalcross.tooling;

import com.totalcross.tooling.jdk.*;
import com.totalcross.tooling.deploy.*;
import com.totalcross.tooling.platform.HostPlatform;
import com.totalcross.tooling.platform.StoreLayout;
import com.totalcross.tooling.process.ProcessRequest;
import com.totalcross.tooling.process.ProcessResult;
import com.totalcross.tooling.process.ProcessRunner;
import com.totalcross.tooling.store.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ToolingCoreTest {
    @Test
    void resolvesNativeRootsAndDoesNotGuessUnknownArchitecture() {
        HostPlatform macArm = HostPlatform.from("Mac OS X", "aarch64");
        StoreLayout mac = StoreLayout.fromSystem(macArm, Map.of(), "/Users/tester");
        assertEquals(Path.of("/Users/tester/Library/Application Support/TotalCross"), mac.dataRoot());
        assertEquals(Path.of("/Users/tester/Library/Caches/TotalCross"), mac.cacheRoot());
        HostPlatform linux = HostPlatform.from("Linux", "amd64");
        StoreLayout xdg = StoreLayout.fromSystem(linux, Map.of("XDG_DATA_HOME", "/tmp/data", "XDG_CACHE_HOME", "/tmp/cache"), "/home/tester");
        assertEquals(Path.of("/tmp/data/TotalCross"), xdg.dataRoot());
        assertThrows(IllegalArgumentException.class, () -> HostPlatform.from("Linux", "sparc"));
    }

    @Test
    void resolvesWindowsNativeRoot() {
        HostPlatform windows = HostPlatform.from("Windows 11", "amd64");
        StoreLayout layout = StoreLayout.fromSystem(windows, Map.of("LOCALAPPDATA", "C:\\Users\\tester\\AppData\\Local"), "C:\\Users\\tester");
        assertEquals(Path.of("C:\\Users\\tester\\AppData\\Local/TotalCross").toAbsolutePath().normalize(), layout.dataRoot());
        assertEquals(Path.of("C:\\Users\\tester\\AppData\\Local/TotalCross/Cache").toAbsolutePath().normalize(), layout.cacheRoot());
    }

    @Test
    void extractsSafelyAndRejectsTraversal() throws Exception {
        Path root = Files.createTempDirectory("extract-test");
        Path safe = root.resolve("safe.zip");
        zip(safe, "app/bin/run", "ok");
        new ArchiveExtractor().extract(safe, root.resolve("out"));
        assertEquals("ok", Files.readString(root.resolve("out/app/bin/run")));
        Path unsafe = root.resolve("unsafe.zip");
        zip(unsafe, "../../outside.txt", "bad");
        assertThrows(IOException.class, () -> new ArchiveExtractor().extract(unsafe, root.resolve("out2")));
    }

    @Test
    void installsWithChecksumMetadataAndAtomicCompletion() throws Exception {
        Path root = Files.createTempDirectory("install-test");
        Path archive = root.resolve("sdk.zip");
        zip(archive, "sdk/bin/tool", "tool");
        String digest = ChecksumVerifier.sha256(archive);
        StoreLayout layout = new StoreLayout(HostPlatform.detect(), root.resolve("data"), root.resolve("cache"));
        ArtifactCoordinate coordinate = new ArtifactCoordinate("sdk", "totalcross", "7.3.0", "build1");
        InstallRequest request = new InstallRequest(coordinate, archive, URI.create("https://example.test/sdk.zip"), digest, HostPlatform.detect());
        InstalledArtifact installed = new ArtifactInstaller(layout).install(request);
        assertTrue(Files.isRegularFile(installed.path().resolve(".totalcross-install-complete")));
        assertEquals("build1", Files.readString(installed.path().resolve("metadata.properties")).contains("build=build1") ? "build1" : "");
        assertEquals(installed.path(), new ArtifactInstaller(layout).install(request).path());
        ArtifactCoordinate secondCoordinate = new ArtifactCoordinate("jdk", "temurin", "17", "build1");
        InstallRequest secondRequest = new InstallRequest(secondCoordinate, archive, request.source(), digest, request.hostPlatform());
        InstalledArtifact second = new ArtifactInstaller(layout).install(secondRequest);
        assertNotEquals(installed.path(), second.path());
        Path incomplete = layout.installationRoot("tooling", "broken-1-b1");
        Files.createDirectories(incomplete);
        assertThrows(IOException.class, () -> new ArtifactInstaller(layout).install(new InstallRequest(
                new ArtifactCoordinate("tooling", "broken", "1", "b1"), archive, request.source(), digest, request.hostPlatform())));
        InstallRequest bad = new InstallRequest(coordinate, archive, request.source(), "0".repeat(64), request.hostPlatform());
        assertThrows(IOException.class, () -> new ArtifactInstaller(new StoreLayout(HostPlatform.detect(), root.resolve("other"), root.resolve("other-cache"))).install(bad));
    }

    @Test
    void serializesConcurrentCoordinateInstallers() throws Exception {
        Path lockPath = Files.createTempDirectory("lock-test").resolve("coordinate.lock");
        FileLockManager manager = new FileLockManager();
        FileLockManager.Lock first = manager.acquire(lockPath);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<FileLockManager.Lock> waiting = executor.submit(() -> manager.acquire(lockPath));
        Thread.sleep(100);
        assertFalse(waiting.isDone());
        first.close();
        try (FileLockManager.Lock second = waiting.get(2, TimeUnit.SECONDS)) {
            assertNotNull(second);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void capturesProcessStreamsAndTimesOut() throws Exception {
        ProcessRunner runner = new ProcessRunner();
        ProcessResult streams = runner.run(new ProcessRequest(List.of("sh", "-c", "printf out; printf err >&2"), null, null, Duration.ofSeconds(5)));
        assertTrue(streams.succeeded());
        assertEquals("out", streams.stdout());
        assertEquals("err", streams.stderr());
        ProcessResult timeout = runner.run(new ProcessRequest(List.of("sh", "-c", "sleep 2"), null, null, Duration.ofMillis(50)));
        assertTrue(timeout.timedOut());
    }

    @Test
    void probesExplicitJdkAndFallsBackFromBrokenAndCracCandidates() throws Exception {
        HostPlatform platform = HostPlatform.detect();
        Path javaHome = Path.of(System.getProperty("java.home"));
        JdkCapabilityProbe probe = new JdkCapabilityProbe(platform);
        JdkInstallation explicit = new JdkInstallation("explicit", "17", "user", javaHome);
        assertTrue(probe.probe(explicit, null).accepted());
        JdkCandidate broken = new JdkCandidate("Broken", "17", "x", javaHome.resolve("missing"), null, "", false);
        JdkCandidate crac = new JdkCandidate("Zulu", "17", "crac", javaHome, null, "", true);
        JdkCandidate working = new JdkCandidate("Temurin", "17", "b1", javaHome, null, "", false);
        JdkInstallation selected = new JdkSelector(probe).select(new JdkRequest("17", null, null), List.of(broken, crac, working));
        assertEquals("Temurin", selected.vendor());
        assertThrows(JdkSelectionException.class, () -> new JdkSelector(probe).select(new JdkRequest("17", null, null), List.of(crac)));
    }

    @Test
    void providersRequireConcreteVersionsAndRejectZuluCrac() {
        HostPlatform platform = HostPlatform.detect();
        assertFalse(new CorrettoProvider().candidate("17.0.12", "build1", platform, Path.of("/jdk")).source().toString().contains("latest"));
        assertThrows(IllegalArgumentException.class, () -> new ZuluProvider().candidate("17", "crac-build", platform, Path.of("/jdk")));
    }

    @Test
    void typedDeployContractReportsIsolatedLegacyFailure() throws Exception {
        Path root = Files.createTempDirectory("deploy-test");
        DeployRequest request = new DeployRequest(root.resolve("app.jar"), root.resolve("out"), root.resolve("sdk"),
                Path.of(System.getProperty("java.home")), List.of(DeployPlatform.LINUX), false,
                DeployLogLevel.NORMAL, List.of());
        DeployResult result = new LegacyDeployService(new DeployToolchain(List.of(root.resolve("missing.jar")))).deploy(request);
        assertFalse(result.succeeded());
        assertFalse(result.diagnostics().isEmpty());
        assertEquals("-linux", DeployPlatform.LINUX.argument());
    }

    private static void zip(Path path, String entry, String content) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new ZipEntry(entry));
            output.write(content.getBytes());
            output.closeEntry();
        }
    }
}
