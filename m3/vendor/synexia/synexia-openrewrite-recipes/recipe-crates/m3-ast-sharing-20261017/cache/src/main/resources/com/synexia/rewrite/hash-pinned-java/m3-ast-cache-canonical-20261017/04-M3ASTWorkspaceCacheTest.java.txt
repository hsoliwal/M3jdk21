// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.mindex.compiler.ast;

import static com.synexia.mindex.compiler.ast.M3ASTCacheTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import com.synexia.mindex.swap.SwapBudgetPlane;
import com.synexia.mindex.swap.TieredSwapCache;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class M3ASTWorkspaceCacheTest {
    @Test void deferredAccessIsLazyAndColdReloadKeepsExactIdentity() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger calls = new AtomicInteger();
        M3FileSourceFacts expected = key(0);
        try (var cache = cache(store)) {
            var ref = cache.deferred(0, expected, id -> { calls.incrementAndGet(); return "syntax-0"; });
            assertEquals(0, store.reads);
            assertEquals(0, calls.get());
            try (var lease = ref.acquire()) {
                assertEquals(0, lease.id());
                assertEquals(expected, lease.key());
                assertEquals("syntax-0", lease.value());
            }
            load(cache, 1);
            load(cache, 2);
            assertFalse(cache.residentIdsByHeat(10).contains(0L));
            try (var reloaded = ref.acquire()) {
                assertEquals(expected, reloaded.key());
                assertEquals("syntax-0", reloaded.value());
            }
            assertEquals(1, calls.get());
        }
        try (var reopened = cache(store); var held = reopened.acquire(0, expected)) {
            assertEquals(expected, held.key());
            assertEquals("syntax-0", held.value());
        }
    }

    @Test void lruAndPressureKeepActiveLeasesAndRefillOnlyOnDemand() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = cache(store)) {
            load(cache, 0);
            load(cache, 1);
            try (var hit = cache.acquire(0, key(0))) { assertEquals("syntax-0", hit.value()); }
            load(cache, 2);
            assertEquals(List.of(2L, 0L), cache.residentIdsByHeat(2));
            var active = cache.acquire(0, key(0));
            SwapBudgetPlane plane = new SwapBudgetPlane(BUDGET, 1, .65, .90);
            var pressure = cache.readjust(plane.snapshot(0));
            assertFalse(pressure.satisfied());
            assertEquals(1, pressure.pinnedEntries());
            assertEquals("syntax-0", active.value());
            try (var transientLease = cache.acquire(3, key(3), ignored -> "syntax-3")) {
                assertFalse(transientLease.resident());
                assertEquals(1, cache.dynamicStats().transientPins());
                assertEquals("syntax-3", transientLease.value());
            }
            assertThrows(IOException.class, cache::close);
            active.close();
            active.close();
            assertThrows(IllegalStateException.class, active::value);
            assertEquals(0, cache.stats().entries());
            assertEquals(0, cache.stats().pins());
            long loads = cache.stats().loads();
            cache.readjust(plane.snapshot(1));
            assertEquals(0, cache.stats().entries());
            assertEquals(loads, cache.stats().loads());
            try (var restored = cache.acquire(3, key(3))) { assertTrue(restored.resident()); }
        }
    }

    @Test void hardCapacityRefusesWithoutInvalidatingPinnedValue() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = new M3ASTWorkspaceCache<>(CONTEXT, store, TEXT,
                new TieredSwapCache.Budget(4_096, 1, 0), false);
                var active = cache.acquire(0, key(0), ignored -> "syntax-0")) {
            assertThrows(TieredSwapCache.CapacityException.class,
                    () -> cache.acquire(1, key(1), ignored -> "syntax-1"));
            assertEquals("syntax-0", active.value());
            assertFalse(store.records.containsKey(1L));
            assertEquals(1, cache.stats().pins());
        }
    }

    @Test void exactSourceProviderSchemaPhaseUriAndContextCannotAlias() throws Exception {
        MemoryStore store = new MemoryStore();
        M3FileSourceFacts original = key(0);
        try (var cache = cache(store)) { load(cache, 0); }
        List<M3FileSourceFacts> mismatches = List.of(
                new M3FileSourceFacts("memory:///Other.java", original.sourceSha256(), original.utf16Length(), CONTEXT, original.providerId(), 1, original.phase()),
                new M3FileSourceFacts(original.sourceUri(), "f".repeat(64), original.utf16Length(), CONTEXT, original.providerId(), 1, original.phase()),
                new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(), original.utf16Length() + 1, CONTEXT, original.providerId(), 1, original.phase()),
                new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(), original.utf16Length(), CONTEXT, "other-provider", 1, original.phase()),
                new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(), original.utf16Length(), CONTEXT, original.providerId(), 2, original.phase()),
                new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(), original.utf16Length(), CONTEXT, original.providerId(), 1, "ANALYZE"));
        for (M3FileSourceFacts mismatch : mismatches) {
            try (var cache = cache(store)) {
                assertThrows(IOException.class, () -> cache.acquire(0, mismatch));
                assertEquals(0, cache.stats().entries());
                assertEquals(0, cache.stats().loads());
                try (var valid = cache.acquire(0, original)) { assertEquals("syntax-0", valid.value()); }
                assertThrows(IOException.class, () -> cache.acquire(0, mismatch));
                assertEquals(0, cache.stats().pins());
            }
        }
        try (var cache = new M3ASTWorkspaceCache<>("b".repeat(64), store, TEXT, BUDGET, false)) {
            assertThrows(IllegalArgumentException.class, () -> cache.acquire(0, original));
            var otherContext = new M3FileSourceFacts(original.sourceUri(), original.sourceSha256(),
                    original.utf16Length(), "b".repeat(64), original.providerId(), 1, original.phase());
            assertThrows(IOException.class, () -> cache.acquire(0, otherContext));
        }
    }

    @Test void corruptedAndMalformedFramesRefuseBeforeResidency() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = cache(store)) { load(cache, 0); }
        byte[] original = store.records.get(0L).clone();
        List<byte[]> bad = new ArrayList<>();
        byte[] checksum = original.clone();
        checksum[checksum.length - 1] ^= 1;
        bad.add(checksum);
        bad.add(Arrays.copyOf(original, 12));
        for (int offset : List.of(0, 4, 8, 16)) {
            byte[] changed = original.clone();
            ByteBuffer.wrap(changed).putInt(offset, Integer.MAX_VALUE);
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.copyOf(changed, changed.length - 32));
            System.arraycopy(hash, 0, changed, changed.length - 32, 32);
            bad.add(changed);
        }
        for (byte[] malformed : bad) {
            store.records.put(0L, malformed);
            try (var cache = cache(store)) {
                assertThrows(IOException.class, () -> cache.acquire(0, key(0)));
                assertEquals(0, cache.stats().entries());
                assertEquals(0, cache.stats().pins());
            }
        }
        store.records.put(0L, original);
        try (var cache = cache(store)) {
            store.records.put(1L, original);
            assertThrows(IOException.class, () -> cache.acquire(1, key(0)));
        }
    }

    @Test void storeAndLoaderFailuresKeepPreviouslyResidentStateAndRemainRetryable() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = cache(store)) {
            load(cache, 0);
            load(cache, 1);
            List<Long> before = cache.residentIdsByHeat(2);
            long loads = cache.stats().loads();
            store.failWrite = true;
            assertThrows(IOException.class, () -> cache.acquire(2, key(2), ignored -> "syntax-2"));
            assertEquals(before, cache.residentIdsByHeat(2));
            assertEquals(loads, cache.stats().loads());
            assertThrows(IOException.class, () -> cache.acquire(2, key(2), ignored -> { throw new IOException("loader failed"); }));
            assertEquals(before, cache.residentIdsByHeat(2));
            assertThrows(NullPointerException.class, () -> cache.acquire(2, key(2), ignored -> null));
            store.failWrite = false;
            load(cache, 2);
            assertEquals(2, cache.stats().entries());
        }
    }

    @Test void concurrentFirstDemandRunsOneLoaderAndEveryLeaseIsReleased() throws Exception {
        MemoryStore store = new MemoryStore();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var cache = cache(store); var workers = Executors.newFixedThreadPool(8)) {
            var ref = cache.deferred(0, key(0), ignored -> {
                calls.incrementAndGet();
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test release timed out");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException(interrupted);
                }
                return "syntax-0";
            });
            List<Future<String>> jobs = new ArrayList<>();
            try {
                for (int i = 0; i < 24; i++) jobs.add(workers.submit(() -> {
                    try (var lease = ref.acquire()) { return lease.value(); }
                }));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
            } finally {
                release.countDown();
            }
            for (Future<String> job : jobs) assertEquals("syntax-0", job.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            assertEquals(1, store.writes);
            assertEquals(0, cache.stats().pins());
        }
    }

    @Test void nestedLoadRestoresRequestIdentityAndSameIdRecursionRefuses() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = cache(store)) {
            try (var outer = cache.acquire(0, key(0), ignored -> {
                try (var inner = cache.acquire(1, key(1), child -> "syntax-1")) {
                    return "parent(" + inner.value() + ")";
                }
            })) {
                assertEquals("parent(syntax-1)", outer.value());
            }
            assertThrows(IOException.class, () -> cache.acquire(2, key(2), ignored -> {
                try (var recursive = cache.acquire(2, key(2))) { return recursive.value(); }
            }));
            load(cache, 2);
            assertEquals(0, cache.stats().pins());
        }
    }

    @Test void manyColdRecordsDoNotExpandResidentEntryPlane() throws Exception {
        MemoryStore store = new MemoryStore();
        try (var cache = cache(store)) {
            for (int i = 0; i < 2_048; i++) {
                load(cache, i);
                assertTrue(cache.stats().entries() <= BUDGET.entries());
                assertTrue(cache.stats().bytes() <= BUDGET.bytes());
                assertEquals(0, cache.stats().pins());
            }
            assertEquals(2_048, store.count());
            assertEquals(2, cache.stats().entries());
            try (var cold = cache.acquire(0, key(0))) { assertEquals("syntax-0", cold.value()); }
        }
    }

    private static void load(M3ASTWorkspaceCache<String> cache, int id) throws IOException {
        try (var lease = cache.acquire(id, key(id), ignored -> "syntax-" + id)) {
            assertEquals("syntax-" + id, lease.value());
        }
    }
}
