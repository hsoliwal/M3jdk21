// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Deterministic heap-pressure and pin-safety simulation; runs without JNI or a forced OOME. */
//FLLDCIM: RELOCATE - donor blob 25149a9daf48 at 165ceb5a0202; recipe m3/collections/recipe/residency-paged-array-20261107/relocate.py v4
public final class M3PressurePolicyTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3-pressure-policy-");
        AtomicReference<M3MemoryPressure.Snapshot> snapshot = new AtomicReference<>(
                new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
        try (M3PagedArray<String> array = new M3PagedArray<>(root, 1, 600,
                1_600, 0, 1 << 20, M3PagedArray.strings(128), snapshot::get, 3)) {
            array.add("alpha");
            array.add("beta");
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 99_000, 99_000, -1, -1));
            array.add("gamma"); // requires evicting both old pages to reach required headroom
            if (array.stats().residentPages() != 1 || array.stats().spillCount() != 2)
                throw new AssertionError("headroom pressure did not reclaim cold pages");
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
            if (!"alpha".equals(array.get(0)) || !"beta".equals(array.get(1))
                    || !"gamma".equals(array.get(2)))
                throw new AssertionError("logical data changed under pressure");
        }
        try (var files = Files.list(root)) {
            if (files.findAny().isPresent()) throw new AssertionError("leaked spill image");
        }
        Files.delete(root);

        Path scratch = Files.createTempDirectory("m3-pin-policy-");
        AtomicReference<M3MemoryPressure.Snapshot> physical = new AtomicReference<>(
                new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, 200_000, 300_000));
        try (var array = new M3PagedArray<>(scratch, 1, 600, 0, 20_000,
                1 << 20, M3PagedArray.strings(32), physical::get, 2)) {
            array.add("one");
            try (var lease = array.pin(0)) {
                physical.set(new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, 100, 300_000));
                boolean refused = false;
                try { array.add("two"); }
                catch (IllegalStateException expected) { refused = true; }
                if (!refused || array.size() != 1 || !"one".equals(lease.value()))
                    throw new AssertionError("pressure must fail without corrupting pins or size");
            }
            physical.set(new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, 200_000, 300_000));
            array.add("two");
            if (!"two".equals(array.get(1))) throw new AssertionError("recovery after pressure");
        }
        Files.delete(scratch);
        testSmallContainerProportionalReserve();
        testObservedReleaseCredit(true);
        testObservedReleaseCredit(false);
        testHotTailAutomaticallyReclaimsColdPages();
        testPassiveTrimFailurePreservesHotValues();
        System.out.println("PASS M3PressurePolicyTest: heap, cgroup-sized physical reserve and safe recovery");
    }



    /**
     * Two pages exist, but the caller only appends to the already-hot second page.
     * External heap pressure must eventually trigger internal LRU eviction without
     * forcing a new page growth, an explicit poll, or a user-side memory knob.
     */
    private static void testHotTailAutomaticallyReclaimsColdPages() throws Exception {
        Path root = Files.createTempDirectory("m3-hot-autopressure-");
        AtomicReference<M3MemoryPressure.Snapshot> snapshot = new AtomicReference<>(
                new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
        int[] samples = {0};
        M3MemoryPressure.Probe probe = () -> {
            samples[0]++;
            return snapshot.get();
        };
        try (M3PagedArray<String> array = new M3PagedArray<>(root, 2_048,
                32_768, 0, 0, 1 << 20, M3PagedArray.strings(32), probe, 4)) {
            for (int i = 0; i < 2_049; i++) array.add(Integer.toString(i));
            int before = samples[0];
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 99_000, 95_000, -1, -1));
            // Never crosses the second page's capacity: only ordinary hot add(value).
            for (int i = 0; i < 1_024; i++) array.add("hot");
            if (samples[0] != before + 1)
                throw new AssertionError("one internal sample per 1024 hot adds: "
                        + (samples[0] - before));
            M3PagedArray.Stats after = array.stats();
            if (after.residentPages() != 1 || after.spillCount() != 1
                    || array.size() != 3_073 || array.deferredPressureFailures() != 0)
                throw new AssertionError("hot append did not shed optional cold pages");
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
            if (!"0".equals(array.get(0)) || !"hot".equals(array.get(3_072)))
                throw new AssertionError("automatic spill/reload lost stable values");
            if (array.stats().pageInCount() == 0)
                throw new AssertionError("automatic cold page was not restored");
        }
        try (var files = Files.list(root)) {
            if (files.findAny().isPresent()) throw new AssertionError("hot pressure leaked files");
        }
        Files.delete(root);
    }

    /**
     * Opportunistic eviction must never turn a valid resident get into an I/O
     * exception if a codec or disk write is temporarily unavailable.
     */
    private static void testPassiveTrimFailurePreservesHotValues() throws Exception {
        Path root = Files.createTempDirectory("m3-hot-trim-refusal-");
        AtomicReference<M3MemoryPressure.Snapshot> snapshot = new AtomicReference<>(
                new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
        var delegate = M3PagedArray.strings(32);
        M3PagedArray.Codec<String> refusing = new M3PagedArray.Codec<>() {
            @Override public void validate(String v) { delegate.validate(v); }
            @Override public long maximumEncodedBytes() { return delegate.maximumEncodedBytes(); }
            @Override public void write(java.io.DataOutput out, String value) throws java.io.IOException {
                if ("locked".equals(value)) throw new java.io.IOException("injected spill refusal");
                delegate.write(out, value);
            }
            @Override public String read(java.io.DataInput in) throws java.io.IOException {
                return delegate.read(in);
            }
        };
        try (M3PagedArray<String> array = new M3PagedArray<>(root, 8, 600,
                0, 0, 1 << 20, refusing, snapshot::get, 4)) {
            array.add("locked");
            for (int i = 1; i < 8; i++) array.add("old");
            array.add("active");
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 99_000, 95_000, -1, -1));
            for (int i = 0; i < 1_024; i++)
                if (!"active".equals(array.get(8)))
                    throw new AssertionError("resident read failed under eviction refusal");
            if (array.deferredPressureFailures() != 1 || array.stats().residentPages() != 2)
                throw new AssertionError("failed optional spill must retain both resident pages");
            snapshot.set(new M3MemoryPressure.Snapshot(100_000, 20_000, 10_000, -1, -1));
            if (!"locked".equals(array.get(0)) || array.size() != 9)
                throw new AssertionError("failed optional spill damaged logical contents");
        }
        try (var files = Files.list(root)) {
            if (files.findAny().isPresent()) throw new AssertionError("failed trim leaked files");
        }
        Files.delete(root);
    }

    /** A positive fresh observation must retire its corresponding pending release credit. */
    private static void testObservedReleaseCredit(boolean budgetEvictionFirst) throws Exception {
        Path scratch = Files.createTempDirectory("m3-observed-release-");
        long[] headrooms = budgetEvictionFirst
                ? new long[] {90_000, 90_000, 90_000, 1_900}
                : new long[] {90_000, 90_000, 90_000, 1_300, 1_900};
        int[] samples = {0};
        boolean[] recovered = {false};
        M3MemoryPressure.Probe probe = () -> {
            long headroom = recovered[0] ? 90_000
                    : headrooms[Math.min(samples[0]++, headrooms.length - 1)];
            return new M3MemoryPressure.Snapshot(100_000, 100_000,
                    100_000 - headroom, -1, -1);
        };
        String held = new String("held");
        try (var array = new M3PagedArray<>(scratch, 1, 600, 1_600, 0, 1 << 20,
                M3PagedArray.strings(128), probe, budgetEvictionFirst ? 2 : 3)) {
            array.add("first");
            array.add(held);
            try (var lease = array.pin(1)) {
                boolean refused = false;
                try { array.add("incoming"); }
                catch (IllegalStateException expected) { refused = true; }
                // Independent oracle: 1,900 current bytes - 600 incoming < 1,600 reserve.
                // The other page is pinned; the already-observed release is not extra capacity.
                if (!refused || array.size() != 2 || lease.value() != held)
                    throw new AssertionError("observed recovery credited twice; beforeBudget="
                            + budgetEvictionFirst);
            }
            recovered[0] = true;
            if (!"first".equals(array.get(0)) || !"held".equals(array.get(1)))
                throw new AssertionError("observed-pressure refusal lost logical data");
            array.add("recovered");
            if (!"recovered".equals(array.get(2)))
                throw new AssertionError("observed-pressure recovery did not restore admission");
        }
        try (var files = Files.list(scratch)) {
            if (files.findAny().isPresent()) throw new AssertionError("observed-pressure spill leak");
        }
        Files.delete(scratch);
    }

    private static void testSmallContainerProportionalReserve() throws Exception {
        Path scratch = Files.createTempDirectory("m3-small-cgroup-");
        AtomicReference<M3MemoryPressure.Snapshot> small = new AtomicReference<>(
                new M3MemoryPressure.Snapshot(32L << 20, 8L << 20, 4L << 20,
                        4L << 20, 16L << 20));
        try (var array = new M3PagedArray<>(scratch, 1, 1024L, 0L, 128L << 20,
                1 << 20, M3PagedArray.strings(30), small::get, 2)) {
            array.add("allowed"); // must not require 128 MiB available in a 16 MiB cgroup
            small.set(new M3MemoryPressure.Snapshot(32L << 20, 8L << 20, 4L << 20,
                    1024L, 16L << 20));
            boolean refused = false;
            try { array.add("rejected"); }
            catch (IllegalStateException expected) { refused = true; }
            if (!refused || array.size() != 1)
                throw new AssertionError("small-container pressure must fail without changing size");
            small.set(new M3MemoryPressure.Snapshot(32L << 20, 8L << 20, 4L << 20,
                    4L << 20, 16L << 20));
            if (!"allowed".equals(array.get(0)))
                throw new AssertionError("old contents must restore when pressure recovers");
            array.add("recovered");
            if (!"recovered".equals(array.get(1)))
                throw new AssertionError("recovery after container memory pressure");
        }
        Files.delete(scratch);
    }
}
