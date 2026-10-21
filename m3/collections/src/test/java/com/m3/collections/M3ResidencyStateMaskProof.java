// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Standalone seeded contract oracle; also runs with the original M3ObjectResidency owner. */
public final class M3ResidencyStateMaskProof {
    private static long checks;
    private M3ResidencyStateMaskProof() { }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label + " at check " + checks);
    }

    private static void throwsType(Class<? extends Throwable> expected, Runnable code, String label) {
        try {
            code.run();
        } catch (Throwable error) {
            check(expected.isInstance(error), label + ": " + error);
            return;
        }
        throw new AssertionError("expected " + expected.getSimpleName() + ": " + label);
    }

    private static int[] reference(M3ObjectResidency<?> arena, int[] rows,
            int offset, int count, int stride, long required, long forbidden, int allowed) {
        ArrayList<Integer> matches = new ArrayList<>(); // Oracle only; product path never boxes rows.
        for (int i = 0; i < count; i++) {
            int slot = rows[(int) ((long) offset + (long) i * stride)];
            long bits = arena.readParentBits(slot);
            int state = 1 << arena.state(slot).ordinal();
            if ((state & allowed) != 0 && (bits & required) == required
                    && (bits & forbidden) == 0) matches.add(slot);
        }
        return matches.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void equal(int[] expected, int[] actual, String why) {
        check(Arrays.equals(expected, actual), why + " expected=" + Arrays.toString(expected)
                + " actual=" + Arrays.toString(actual));
    }

    private static M3ObjectResidency<Object> newOwner(int slots) {
        return new M3ObjectResidency<>(slots,
                M3ObjectResidency.LAZY_CREATE | M3ObjectResidency.RECREATE
                        | M3ObjectResidency.STRICT_NO_ESCAPE | M3ObjectResidency.LRU,
                1_000_000L, 0L, slot -> new Object(), null, null);
    }

    public static void main(String[] args) {
        boolean requireNative = args.length != 0 && "native".equals(args[0]);
        boolean actualNative = M3ResidencyStateMaskIndex.nativeAvailable();
        check(!requireNative || actualNative, "native binary installed for required proof");
        var absent = M3ObjectResidency.State.ABSENT;
        var live = M3ObjectResidency.State.RESIDENT;
        int absentMask = M3ResidencyStateMaskIndex.states(absent);
        int liveMask = M3ResidencyStateMaskIndex.states(live);
        var arena = newOwner(71);
        int[] rows = {4, 0, 3, 4, 1, 2, 1, 70};
        arena.writeParentBits(4, Long.MIN_VALUE | 3L);
        arena.writeParentBits(3, Long.MIN_VALUE | 2L);
        arena.writeParentBits(0, Long.MIN_VALUE | 1L);
        arena.writeParentBits(1, Long.MIN_VALUE | 5L);
        arena.writeParentBits(2, 0L);
        arena.use(4, 1L, v -> 1);
        arena.use(0, 1L, v -> 1);
        arena.use(1, 1L, v -> 1);
        long req = Long.MIN_VALUE | 1L, notBits = 2L;
        equal(new int[]{0,1,1}, M3ResidencyStateMaskIndex.select(arena, rows, 0,
                rows.length, 1, req, notBits, liveMask, M3ResidencyStateMaskIndex.Backend.JAVA),
                "preserve duplicates and source order");
        equal(new int[]{1,1,0}, M3ResidencyStateMaskIndex.select(arena, rows,
                rows.length - 1, rows.length, -1, req, notBits,
                liveMask, M3ResidencyStateMaskIndex.Backend.JAVA), "reverse signed stride");
        check(M3ResidencyStateMaskIndex.count(arena, rows, 0, rows.length, 1,
                req, notBits, liveMask) == 3, "count matches");
        check(M3ResidencyStateMaskIndex.first(arena, rows, 0, rows.length, 1,
                req, notBits, liveMask) == 0, "first match without return array");
        int[] outputLane = new int[8];
        Arrays.fill(outputLane, -777);
        int filled = M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                req, notBits, liveMask, outputLane, 2);
        check(filled == 3, "in-place selection length");
        equal(new int[]{-777, -777, 0, 1, 1, -777, -777, -777}, outputLane,
                "reuse caller-owned output lane without creating entries");
        int[] shortOutput = {19, 19, 19};
        throwsType(IndexOutOfBoundsException.class,
                () -> M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                        req, notBits, liveMask, shortOutput, 1),
                "reject insufficient capacity before any output write");
        equal(new int[]{19, 19, 19}, shortOutput, "preflight preserves destination bytes");
        throwsType(IllegalArgumentException.class,
                () -> M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                        req, notBits, liveMask, rows, 0),
                "reject output-input aliases to preserve future coordinates");
        throwsType(IndexOutOfBoundsException.class,
                () -> M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                        req, notBits, liveMask, shortOutput, -1),
                "negative destination offset");
        equal(new int[]{19, 19, 19}, shortOutput, "invalid output offset left destination intact");

        check(M3ResidencyStateMaskIndex.first(arena, rows, 0, rows.length, 1,
                req, notBits, absentMask) == -1, "state exclusion");
        equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, rows.length, 0,
                -1, 0L, 0L, 0, M3ResidencyStateMaskIndex.Backend.JAVA), "empty path");
        // Both new empty-output atoms must preserve the full preflight contract.
        // The all-zero state mask and an absent parent bit produce zero matches
        // over a nonempty, duplicate-bearing, stable physical index path.
        int[] unmodified = {88, 88, 88};
        equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, 0,
                rows.length, 1, req, notBits, 0,
                M3ResidencyStateMaskIndex.Backend.JAVA), "zero allowed-state mask");
        equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, 0,
                rows.length, 1, 1L << 60, 0, liveMask,
                M3ResidencyStateMaskIndex.Backend.JAVA), "zero parent-word match");
        check(M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                req, notBits, 0, unmodified, 1) == 0, "zero-hit output count");
        equal(new int[]{88, 88, 88}, unmodified, "zero-hit output not modified");
        throwsType(IndexOutOfBoundsException.class,
                () -> M3ResidencyStateMaskIndex.selectInto(arena, rows, 0, rows.length, 1,
                        req, notBits, 0, unmodified, unmodified.length + 1),
                "zero-hit preflight still rejects invalid output start");
        if (actualNative) {
            equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, 0,
                    rows.length, 1, req, notBits, 0,
                    M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED),
                    "JNI zero allowed-state mask");
            equal(new int[0], M3ResidencyStateMaskIndex.select(arena, rows, 0,
                    rows.length, 1, 1L << 60, 0, liveMask,
                    M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED),
                    "JNI zero parent-word match");
        }
        throwsType(IllegalArgumentException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, rows, 0, 1, 1, 3, 1, liveMask), "overlap masks rejected");
        throwsType(IllegalArgumentException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, rows, 0, 1, 1, 0, 0, 64), "undefined lifecycle state rejected");
        throwsType(IllegalArgumentException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, rows, 0, 1, 0, 0, 0, liveMask), "zero stride rejected");
        throwsType(IndexOutOfBoundsException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, rows, rows.length - 1, 2, 1, 0, 0, liveMask), "overflowing path rejected");
        throwsType(IndexOutOfBoundsException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, new int[]{-1}, 0, 1, 1, 0, 0, liveMask), "bad physical slot rejected");
        throwsType(NullPointerException.class, () -> M3ResidencyStateMaskIndex.count(
                arena, null, 0, 0, 1, 0, 0, liveMask), "null ordered coords");
        throwsType(NullPointerException.class, () -> M3ResidencyStateMaskIndex.states(
                (M3ObjectResidency.State[]) null), "null state mask varargs");
        if (!actualNative) {
            throwsType(UnsatisfiedLinkError.class, () -> M3ResidencyStateMaskIndex.select(arena,
                    rows, 0, 2, 1, 0L, 0L, liveMask,
                    M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED), "no implicit native fallback");
        }

        // A lifecycle transition is distinct from canonical parent state:
        // creating/retiring the child never changes its parent-owned bit flags.
        var lifetime = newOwner(3);
        lifetime.writeParentBits(1, 0x101L);
        int[] lifetimeRows = {1, 2, 1};
        equal(new int[]{1, 1}, M3ResidencyStateMaskIndex.select(lifetime,
                lifetimeRows, 0, lifetimeRows.length, 1, 0x101L, 0L, absentMask,
                M3ResidencyStateMaskIndex.Backend.JAVA), "absent child with canonical state");
        lifetime.use(1, 1L, unused -> 1);
        equal(new int[]{1, 1}, M3ResidencyStateMaskIndex.select(lifetime,
                lifetimeRows, 0, lifetimeRows.length, 1, 0x101L, 0L, liveMask,
                M3ResidencyStateMaskIndex.Backend.JAVA), "materialized child mask");
        check(lifetime.retireUnborrowedProjection(1), "cooperative unborrowed retire");
        check(lifetime.readParentBits(1) == 0x101L,
                "logical state bits remain owned by parent across child recreation");
        equal(new int[]{1, 1}, M3ResidencyStateMaskIndex.select(lifetime,
                lifetimeRows, 0, lifetimeRows.length, 1, 0x101L, 0L, absentMask,
                M3ResidencyStateMaskIndex.Backend.JAVA), "retired child absent again");
        // This selector never returns any child object: it returns slot ints only.

        Random random = new Random(20261017L);
        long fingerprint = 0xcbf29ce484222325L;
        for (int round = 0; round < 1_600; round++) {
            int slots = 1 + random.nextInt(128);
            var owner = newOwner(slots);
            for (int s = 0; s < slots; s++) {
                owner.writeParentBits(s, random.nextLong());
                if (random.nextBoolean()) owner.use(s, 1L, v -> 1);
            }
            int n = round % 270 + (round % 13 == 0 ? 1_024 : 0);
            int[] ordered = new int[n];
            for (int i = 0; i < n; i++) ordered[i] = random.nextInt(slots);
            int stride = random.nextBoolean() ? 1 : -1;
            int offset = stride == 1 || n == 0 ? 0 : n - 1;
            long required = random.nextLong() & (random.nextBoolean() ? 0xFFL : Long.MIN_VALUE);
            long forbidden = random.nextLong() & ~required;
            int stateMask = random.nextInt(64);
            int[] expected = reference(owner, ordered, offset, n, stride,
                    required, forbidden, stateMask);
            int[] actual = M3ResidencyStateMaskIndex.select(owner, ordered, offset, n, stride,
                    required, forbidden, stateMask, M3ResidencyStateMaskIndex.Backend.JAVA);
            equal(expected, actual, "java/reference round=" + round);
            int[] reused = new int[expected.length + 5];
            Arrays.fill(reused, Integer.MIN_VALUE);
            int emitted = M3ResidencyStateMaskIndex.selectInto(owner, ordered, offset,
                    n, stride, required, forbidden, stateMask, reused, 3);
            check(emitted == expected.length, "reused lane output length round=" + round);
            check(reused[0] == Integer.MIN_VALUE && reused[1] == Integer.MIN_VALUE
                    && reused[2] == Integer.MIN_VALUE
                    && reused[reused.length - 1] == Integer.MIN_VALUE,
                    "reused lane left outside segment untouched round=" + round);
            equal(expected, Arrays.copyOfRange(reused, 3, 3 + emitted),
                    "reused lane exact coordinates round=" + round);
            check(M3ResidencyStateMaskIndex.count(owner, ordered, offset, n, stride,
                    required, forbidden, stateMask) == expected.length, "count round=" + round);
            check(M3ResidencyStateMaskIndex.first(owner, ordered, offset, n, stride,
                    required, forbidden, stateMask) == (expected.length == 0 ? -1 : expected[0]),
                    "first round=" + round);
            if (actualNative) equal(actual, M3ResidencyStateMaskIndex.select(owner,
                    ordered, offset, n, stride, required, forbidden, stateMask,
                    M3ResidencyStateMaskIndex.Backend.JNI_REQUIRED), "jni/reference round=" + round);
            fingerprint = (fingerprint ^ actual.length) * 0x100000001b3L;
            for (int k : actual) fingerprint = (fingerprint ^ k) * 0x100000001b3L;
        }
        System.out.println("M3_RESIDENCY_STATE_MASK_PASS checks=" + checks
                + " fingerprint=" + Long.toUnsignedString(fingerprint)
                + " backend=" + (actualNative ? "jni-enabled" : "java-only"));
    }
}
