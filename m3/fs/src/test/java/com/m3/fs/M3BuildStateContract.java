// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

// Modified 2026-10-21: preserve explicit legacy timestamp knowledge and v1 byte round trips.

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

/** Runnable without JUnit or JNI; the M3FS Maven suite invokes the same oracle. */
public final class M3BuildStateContract {
    private static final int MAX_BYTES = 16_000_000;
    private static int checks;

    private M3BuildStateContract() { }
    private static byte[] sha(String data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void invalid(Runnable task) {
        checks++;
        try { task.run(); throw new AssertionError("accepted invalid input"); }
        catch (IllegalArgumentException expected) { /* expected */ }
    }
    private static void badImage(byte[] bytes) {
        checks++;
        try { M3BuildState.decode(bytes, MAX_BYTES, 1_000, 10_000); throw new AssertionError("accepted corrupt image"); }
        catch (IOException expected) { /* expected */ }
    }

    private static M3BuildState sample(String a, String abiA, String context) {
        String[] keys = {"a/A.java", "b/B.class", "c/prop.txt"};
        byte[] role = {M3BuildState.SOURCE, M3BuildState.CLASS, M3BuildState.RESOURCE};
        long[] sizes = {a.length(), 8L, 4L};
        byte[] digests = new byte[3 * 32];
        byte[] abis = new byte[3 * 32];
        put(digests, 0, sha(a)); put(digests, 1, sha("class")); put(digests, 2, sha("resource"));
        put(abis, 0, sha(abiA));
        return new M3BuildState(keys, role, sizes, digests, abis, new long[]{1L},
                new int[]{0, 0, 1, 2}, new int[]{0, 1}, sha(context));
    }

    private static M3BuildState overlappingChanges(boolean now) {
        byte[] contents = new byte[96], abi = new byte[96];
        put(contents, 0, sha(now ? "a new" : "a old"));
        put(contents, 1, sha(now ? "b new" : "b old"));
        put(contents, 2, sha("c unchanged"));
        put(abi, 0, sha(now ? "A NEW ABI" : "A OLD ABI"));
        put(abi, 1, sha("B identical ABI"));
        return new M3BuildState(new String[]{"a", "b", "c"},
                fill(3, M3BuildState.SOURCE), new long[]{5, 5, 11}, contents,
                abi, new long[]{3}, new int[]{0, 0, 1, 2}, new int[]{0, 1}, sha("ctx"));
    }

    private static void put(byte[] lane, int row, byte[] value) {
        System.arraycopy(value, 0, lane, row * 32, 32);
    }

    public static int verify() throws IOException {
        checks = 0;
        M3BuildState baseline = sample("old", "sameABI", "ctx");
        byte[] encoded = baseline.encode(MAX_BYTES);
        M3BuildState decoded = M3BuildState.decode(encoded, MAX_BYTES, 3, 2);
        check(Arrays.equals(encoded, decoded.encode(MAX_BYTES)), "canonical round-trip");
        check(decoded.size() == 3 && decoded.dependencyCount() == 2, "shape");
        check(decoded.dependency(2, 0) == 1 && decoded.dependency(1, 0) == 0, "edge identity");
        check(decoded.find("b/B.class", M3BuildState.CLASS) == 1 && decoded.find("absent", M3BuildState.CLASS) == -1, "binary search");
        check(decoded.hasAbi(0) && !decoded.hasAbi(1), "ABI bitmap");
        check(java.nio.ByteBuffer.wrap(encoded).getInt(4) == 1, "legacy constructor preserves v1 schema");
        check(!decoded.completeInventory() && decoded.modifiedTimeNanos(0).isEmpty(), "v1 timestamp remains unknown");
        check(!decoded.hasModifiedTime(1), "unknown timestamp is not epoch zero");
        check(decoded.changedFrom(decoded).affectedCount() == 0L, "unchanged exact state");
        M3BuildState changed = sample("new body", "sameABI", "ctx");
        M3BuildState.Plan plan = changed.changedFrom(baseline);
        check(!plan.fullRebuild() && plan.changed(0) && plan.affected(0), "modified source");
        check(!plan.affected(1) && !plan.affected(2), "compiler-proven ABI stops propagation");
        check(plan.affectedCount() == 1L, "ABI-limited count");
        M3BuildState apiChanged = sample("new body", "DIFFERENT ABI", "ctx");
        plan = apiChanged.changedFrom(baseline);
        check(plan.affectedCount() == 3L && plan.affected(2), "reverse dependency closure");
        // A changed exported ABI forces B to rebuild, even though B's independently
        // changed source has an equal ABI. B must not block the propagation to C.
        M3BuildState oldChain = overlappingChanges(false);
        M3BuildState newChain = overlappingChanges(true);
        M3BuildState.Plan overlapping = newChain.changedFrom(oldChain);
        check(overlapping.changed(0) && overlapping.changed(1), "overlapping direct changes");
        check(overlapping.affectedCount() == 3L && overlapping.affected(2),
                "independently dirty middle vertex cannot suppress ancestor invalidation");
        check(Arrays.equals(plan.changedMask(), new long[]{1L}), "only source changed");
        long[] hacked = plan.affectedMask(); hacked[0] = 0L;
        check(plan.affectedCount() == 3L, "defensive bitmap projection");
        check(apiChanged.changedFrom(sample("old", "sameABI", "new ctx")).fullRebuild(), "context invalidation");
        check(apiChanged.changedFrom(null).fullRebuild(), "cold first build");
        check(apiChanged.changedFrom(baseline).affectedCount() == 3L, "repeatable planning");
        M3BuildState removed = new M3BuildState(new String[]{"a/A.java"},
                new byte[]{M3BuildState.SOURCE}, new long[]{3}, sha("old"), sha("sameABI"),
                new long[]{1L}, new int[]{0, 0}, new int[0], sha("ctx"));
        check(removed.changedFrom(baseline).fullRebuild(), "removed input fail-closed");
        check(removed.changedFrom(baseline).affectedCount() == 1L, "removed input surviving rows");
        byte[] corrupt = encoded.clone(); corrupt[10] ^= 0x01; badImage(corrupt);
        badImage(Arrays.copyOf(encoded, encoded.length - 1));
        badImage(Arrays.copyOf(encoded, MAX_BYTES + 1));
        // Counts that fit the caller's row limit but not the image budget must
        // be refused before allocating any large column arrays.
        byte[] impossible = encoded.clone();
        java.nio.ByteBuffer.wrap(impossible).putInt(8, 30_000);
        putSeal(impossible);
        checks++;
        try { M3BuildState.decode(impossible, MAX_BYTES, 40_000, 10_000);
              throw new AssertionError("accepted impossible image geometry"); }
        catch (IOException expected) { /* expected */ }
        checks++;
        try { baseline.encode(encoded.length - 1);
              throw new AssertionError("admitted output exceeding exact bound"); }
        catch (IOException expected) { /* expected */ }
        invalid(() -> sampleInvalid(new String[]{"same", "same"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"../bad", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a//b", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a/./b", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a/", "safe"}, new int[]{0, 0, 0}, new int[0]));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 1}, new int[]{2}));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 2}, new int[]{0, 0}));
        invalid(() -> sampleInvalid(new String[]{"a", "b"}, new int[]{0, 0, 0}, new int[0], new long[]{4L}));
        fuzzReverseClosure();
        return checks;
    }

    private static void putSeal(byte[] encoded) {
        byte[] data = Arrays.copyOf(encoded, encoded.length - 32);
        byte[] hash;
        try { hash = MessageDigest.getInstance("SHA-256").digest(data); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        System.arraycopy(hash, 0, encoded, data.length, 32);
    }

    private static byte[] fill(int n, byte value) { byte[] values = new byte[n]; Arrays.fill(values, value); return values; }

    private static void sampleInvalid(String[] keys, int[] offsets, int[] edges) {
        sampleInvalid(keys, offsets, edges, new long[]{0L});
    }
    private static void sampleInvalid(String[] keys, int[] offsets, int[] edges, long[] mask) {
        byte[] ids = new byte[keys.length * 32];
        new M3BuildState(keys, fill(keys.length, M3BuildState.SOURCE), new long[keys.length], ids, ids,
                mask, offsets, edges, sha("ctx"));
    }

    private static void fuzzReverseClosure() throws IOException {
        Random random = new Random(210_2026L);
        for (int trial = 0; trial < 180; trial++) {
            int n = 1 + random.nextInt(160), changedRow = random.nextInt(n);
            String[] keys = new String[n]; byte[] roles = new byte[n]; long[] length = new long[n];
            byte[] oldHashes = new byte[n * 32], newHashes = new byte[n * 32], unknown = new byte[n * 32];
            int[] offsets = new int[n + 1], pool = new int[n * 7]; int count = 0;
            boolean[][] deps = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                keys[i] = String.format("unit/%05d.java", i); roles[i] = M3BuildState.SOURCE; length[i] = 7L;
                put(oldHashes, i, sha("unit:" + i)); put(newHashes, i, sha(i == changedRow ? "CHANGED:" + i : "unit:" + i));
                offsets[i] = count;
                for (int j = 0; j < n; j++) {
                    if (i != j && random.nextInt(150) < 2) {
                        if (count == pool.length) pool = Arrays.copyOf(pool, pool.length * 2 + 1);
                        deps[i][j] = true; pool[count++] = j;
                    }
                }
            }
            offsets[n] = count;
            int[] edges = Arrays.copyOf(pool, count);
            long[] noAbi = new long[(n + 63) >>> 6];
            M3BuildState old = new M3BuildState(keys, roles, length, oldHashes, unknown,
                    noAbi, offsets, edges, sha("ctx"));
            M3BuildState next = new M3BuildState(keys, roles, length, newHashes, unknown,
                    noAbi, offsets, edges, sha("ctx"));
            M3BuildState.Plan plan = next.changedFrom(old);
            boolean[] reference = new boolean[n]; reference[changedRow] = true;
            boolean updated;
            do {
                updated = false;
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    if (deps[i][j] && reference[j] && !reference[i]) { reference[i] = true; updated = true; }
                }
            } while (updated);
            for (int i = 0; i < n; i++) check(plan.affected(i) == reference[i], "differential reverse closure row " + i);
            byte[] image = next.encode(MAX_BYTES);
            check(Arrays.equals(image, M3BuildState.decode(image, MAX_BYTES, n, edges.length).encode(MAX_BYTES)), "random roundtrip");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("M3_BUILD_STATE_JAVA21_PASS checks=" + verify());
    }
}
