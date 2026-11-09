// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.stream.Stream;

/**
 * Reference model of the relocated M3PagedArray (recipe residency-paged-array-20261107): seeded episodes of add, get,
 * set, pin/lease, trimTo and stats against java.util.ArrayList, under an injected pressure Probe whose seeded samples
 * drive headroom admission, physical-pressure eviction and the 1-in-1024 resident-access trim. Every op prints one
 * line (outcome, model agreement, resident/spill/page-in counts); a disagreement with the model prints MISMATCH. A
 * pressure refusal (IllegalStateException "memory pressure: no evictable pages") leaves array and model unchanged.
 * differential.sh compiles the same source a second time under the donor package and runs both: the two outputs must
 * be byte-identical. usage: M3PagedArrayReferenceDifferential OUT_FILE SCRATCH_DIR EPISODES OPS_PER_EPISODE
 */
public final class M3PagedArrayReferenceDifferential {
    static final long SEED = 0x5041474541525241L; // named seed "PAGEARRA"
    static final long MAX_HEAP = 1_000_000L;

    private M3PagedArrayReferenceDifferential() { }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args[1]);
        int episodes = Integer.parseInt(args[2]);
        int ops = Integer.parseInt(args[3]);
        long mismatches = 0;
        try (BufferedWriter out = Files.newBufferedWriter(Path.of(args[0]), StandardCharsets.UTF_8)) {
            for (int e = 0; e < episodes; e++) mismatches += episode(out, root.resolve("e" + e), e, ops);
            out.write("TOTAL episodes=" + episodes + " ops=" + (long) episodes * ops + " mismatches=" + mismatches + "\n");
        }
        if (mismatches != 0) throw new AssertionError("reference model mismatches: " + mismatches);
    }

    private static long episode(BufferedWriter out, Path scratch, int e, int ops) throws IOException {
        SplittableRandom rnd = new SplittableRandom(SEED ^ (e * 0x9E3779B97F4A7C15L));
        SplittableRandom pressure = new SplittableRandom(~SEED ^ (e * 0xC2B2AE3D27D4EB4FL));
        int pageSize = 1 + rnd.nextInt(8);
        int residentPages = 2 + rnd.nextInt(3);
        double hot = e % 3 == 0 ? 0.0 : e % 3 == 1 ? 0.05 : 0.25;
        long[] samples = {0};
        boolean[] calm = {false};
        M3MemoryPressure.Probe probe = () -> {
            samples[0]++;
            if (calm[0]) return new M3MemoryPressure.Snapshot(MAX_HEAP, MAX_HEAP / 10, MAX_HEAP / 10, 1_000_000L, 4_000_000L);
            double draw = pressure.nextDouble();
            long used = draw < hot ? MAX_HEAP - 1_500 : draw < 2 * hot ? MAX_HEAP * 8 / 10 : MAX_HEAP / 10;
            long physical = pressure.nextInt(10) == 0 ? 100L : 1_000_000L;
            return new M3MemoryPressure.Snapshot(MAX_HEAP, used, used, physical, 4_000_000L);
        };
        ArrayList<String> model = new ArrayList<>();
        ArrayDeque<M3PagedArray<String>.Lease> leases = new ArrayDeque<>();
        ArrayDeque<Integer> leased = new ArrayDeque<>();
        long mismatches = 0;
        M3PagedArray<String> array = new M3PagedArray<>(scratch, pageSize, 600, 1_600, 1_000, 1 << 20,
                M3PagedArray.strings(64), probe, residentPages);
        for (int i = 0; i < ops; i++) {
            int op = rnd.nextInt(20);
            int size = model.size();
            int index = size == 0 ? 0 : rnd.nextInt(size);
            String value = rnd.nextInt(10) == 0 ? null : "v" + e + "-" + i + "-" + "x".repeat(rnd.nextInt(5));
            String outcome;
            boolean agree = true;
            try {
                if (op < 8 || size == 0) {
                    array.add(value);
                    model.add(value);
                    outcome = "add " + value;
                } else if (op < 13) {
                    String got = array.get(index);
                    agree = Objects.equals(got, model.get(index));
                    outcome = "get " + index + "=" + got;
                } else if (op < 16) {
                    String old = array.set(index, value);
                    agree = Objects.equals(old, model.set(index, value));
                    outcome = "set " + index + "=" + value + " old=" + old;
                } else if (op < 18) {
                    if (leases.size() < residentPages - 1) { // one resident page stays unpinned
                        M3PagedArray<String>.Lease lease = array.pin(index);
                        leases.add(lease);
                        leased.add(index);
                        agree = Objects.equals(lease.value(), model.get(index));
                        outcome = "pin " + index + "=" + lease.value();
                    } else {
                        int at = leased.poll();
                        M3PagedArray<String>.Lease lease = leases.poll();
                        agree = Objects.equals(lease.value(), model.get(at));
                        lease.close();
                        outcome = "unpin " + at;
                    }
                } else if (op == 18) {
                    outcome = "trim " + array.trimTo(rnd.nextInt(3));
                } else {
                    M3PagedArray.Stats s = array.stats();
                    agree = s.size() == model.size() && s.totalPages() == (model.size() + pageSize - 1) / pageSize;
                    outcome = "stats size=" + s.size() + " pages=" + s.totalPages();
                }
            } catch (RuntimeException failure) {
                outcome = "op" + op + " threw " + failure.getClass().getSimpleName() + ":" + failure.getMessage();
            }
            if (!agree) mismatches++;
            M3PagedArray.Stats s = array.stats();
            out.write(e + ":" + i + " " + outcome + (agree ? "" : " MISMATCH") + " size=" + array.size()
                    + " model=" + model.size() + " resident=" + s.residentPages() + " spills=" + s.spillCount()
                    + " pageIns=" + s.pageInCount() + " deferred=" + array.deferredPressureFailures()
                    + " samples=" + samples[0] + "\n");
        }
        while (!leases.isEmpty()) leases.poll().close();
        calm[0] = true; // the final read-back runs without injected pressure and without leases
        for (int i = 0; i < model.size(); i++) {
            if (!Objects.equals(array.get(i), model.get(i))) mismatches++;
        }
        array.close();
        long left;
        try (Stream<Path> files = Files.list(scratch)) {
            left = files.count();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
        out.write(e + ":end size=" + model.size() + " leftFiles=" + left + " mismatches=" + mismatches + "\n");
        Files.deleteIfExists(scratch);
        return mismatches + left;
    }
}
