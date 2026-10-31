// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class M3FileScopeTest {
    @TempDir Path directory;

    @Test void boundedIdentityGrowthAndClose() throws Exception {
        var scope = new M3FileScope(4096);
        var admitted = new M3File[4096];
        for (int i = 0; i < admitted.length; i++) {
            Path path = directory.resolve(Integer.toString(i));
            admitted[i] = scope.file(path);
            assertSame(path, admitted[i].path());
        }
        for (int i = admitted.length - 1; i >= 0; i--) {
            assertSame(admitted[i], scope.file(directory.resolve(Integer.toString(i))));
        }
        assertEquals(4096, scope.size());
        assertThrows(IllegalStateException.class, () -> scope.file(directory.resolve("overflow")));
        M3File retained = admitted[0];
        scope.close();
        scope.close();
        assertThrows(IllegalStateException.class, scope::size);
        assertThrows(IllegalStateException.class, () -> scope.file(retained.path()));
        Files.writeString(retained.path(), "still alive");
        assertArrayEquals("still alive".getBytes(java.nio.charset.StandardCharsets.UTF_8), retained.readAllBytes(11));
    }

    @Test void noNormalizationOrGlobalInterning() {
        Path path = directory.resolve("missing");
        try (var scope = new M3FileScope(3); var second = new M3FileScope(3)) {
            assertNotSame(M3File.of(path), M3File.of(path));
            assertNotSame(scope.file(path), second.file(path));
            assertNotSame(scope.file(path), scope.file(directory.resolve(".").resolve("missing")));
            assertFalse(Files.exists(path));
            assertThrows(NullPointerException.class, () -> scope.file(null));
        }
        assertThrows(IllegalArgumentException.class, () -> new M3FileScope(-1));
        assertThrows(IllegalArgumentException.class, () -> new M3FileScope(Integer.MAX_VALUE));
        try (var empty = new M3FileScope(0)) {
            assertThrows(IllegalStateException.class, () -> empty.file(path));
        }
    }

    @Test void providerInstancesRemainSeparateAndUnowned() throws Exception {
        var a = FileSystems.newFileSystem(directory.resolve("a.zip"), Map.of("create", "true"));
        var b = FileSystems.newFileSystem(directory.resolve("b.zip"), Map.of("create", "true"));
        try (a; b) {
            try (var scope = new M3FileScope(2)) {
                assertNotSame(scope.file(a.getPath("/same")), scope.file(b.getPath("/same")));
            }
            assertTrue(a.isOpen());
            assertTrue(b.isOpen());
        }
    }

    @Test void concurrentAdmissionsKeepSingleIdentity() throws Exception {
        try (var scope = new M3FileScope(64); var executor = Executors.newFixedThreadPool(4)) {
            var calls = new ArrayList<java.util.concurrent.Callable<M3File>>();
            for (int i = 0; i < 2048; i++) {
                Path path = directory.resolve(Integer.toString(i % 64));
                calls.add(() -> scope.file(path));
            }
            var results = executor.invokeAll(calls);
            for (int i = 0; i < results.size(); i++) {
                assertSame(scope.file(directory.resolve(Integer.toString(i % 64))), results.get(i).get());
            }
            assertEquals(64, scope.size());
        }
    }
}
