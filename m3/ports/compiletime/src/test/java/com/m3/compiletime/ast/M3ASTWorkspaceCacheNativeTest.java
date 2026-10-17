// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.compiletime.ast;

import static com.m3.compiletime.ast.M3ASTCacheTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.synexia.mindex.swap.SqliteSwapStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** These are real SQLite JNI tests; an unavailable native library is a failure, not a skipped pass. */
class M3ASTWorkspaceCacheNativeTest {
    @TempDir Path temporary;

    @Test void persistedSourceFramesReopenAfterEvictionAndKeepStableIdentity() throws Exception {
        Path library = Path.of(System.getProperty("m3.cache.native", "missing-native-library")).toAbsolutePath();
        assertTrue(Files.isRegularFile(library), "supply the compiled existing SQLite swap JNI library");
        SqliteSwapStore.loadLibrary(library);
        Path database = temporary.resolve("ast-cache.sqlite");
        try (var store = open(database, CONTEXT); var cache = cache(store)) {
            for (int i = 0; i < 128; i++) {
                int ordinal = i;
                try (var lease = cache.acquire(i, key(i), ignored -> "native-syntax-" + ordinal)) {
                    assertEquals(key(i), lease.key());
                }
                assertTrue(cache.stats().entries() <= BUDGET.entries());
                assertTrue(cache.stats().bytes() <= BUDGET.bytes());
            }
            assertEquals(128, store.count());
        }
        assertTrue(Files.size(database) > 0);
        try (var store = open(database, CONTEXT); var cache = cache(store)) {
            assertEquals(0, cache.stats().loads());
            assertEquals(0, cache.stats().entries());
            try (var lease = cache.acquire(0, key(0))) {
                assertEquals("native-syntax-0", lease.value());
                assertEquals(key(0), lease.key());
            }
            try (var lease = cache.acquire(127, key(127))) {
                assertEquals("native-syntax-127", lease.value());
            }
        }
        try (var store = open(database, "b".repeat(64)); var cache = cache(store)) {
            assertThrows(IOException.class, () -> cache.acquire(0, key(0)));
        }
    }

    @Test void nativeContextAndImmutableRecordConflictsAreRefused() throws Exception {
        Path library = Path.of(System.getProperty("m3.cache.native", "missing-native-library")).toAbsolutePath();
        assertTrue(Files.isRegularFile(library));
        SqliteSwapStore.loadLibrary(library);
        Path database = temporary.resolve("ast-identity.sqlite");
        try (var store = open(database, CONTEXT); var cache = cache(store)) {
            try (var lease = cache.acquire(0, key(0), ignored -> "original")) {
                assertThrows(IOException.class, cache::close);
                assertEquals("original", lease.value());
            }
            assertThrows(IOException.class, () -> store.write(java.util.Map.of(0L, new byte[] {1, 2, 3})));
        }
        M3FileSourceFacts original = key(0);
        String other = "c".repeat(64);
        M3FileSourceFacts changed = new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(),
                original.utf16Length(), other, original.providerId(), original.schemaVersion(), original.phase());
        try (var store = open(database, CONTEXT);
                var cache = new M3ASTWorkspaceCache<>(other, store, TEXT, BUDGET, false)) {
            assertThrows(IOException.class, () -> cache.acquire(0, changed));
            assertEquals(0, cache.stats().entries());
        }
    }

    private static SqliteSwapStore open(Path database, String namespace) throws IOException {
        return SqliteSwapStore.open(database, namespace, "m3-ast-shared-v1", SqliteSwapStore.Options.defaults());
    }
}
