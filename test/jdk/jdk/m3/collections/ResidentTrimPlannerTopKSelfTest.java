// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.internal.m3.collections.LazyElementFlags;

/** Independent comparator-and-sort oracle; usable with original OR candidate class. */
//FLLDCIM: RELOCATE - donor blob e2027e901579 at fd9a233680b4; recipe make/m3/collections/recipe/transient-facade-20261106/relocate.py v4
public final class ResidentTrimPlannerTopKSelfTest {
    private static long checked;
    private static long selected;
    private static final long[] EXTRAS = {Long.MIN_VALUE, -2L, -1L, 0L, 1L, 2L, Long.MAX_VALUE};

    private ResidentTrimPlannerTopKSelfTest() { }

    private static void verify(short[] flags, long[] ages, int count, int request) {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if ((flags[i] & LazyElementFlags.PINNED) == 0) candidates.add(i);
        }
        candidates.sort((a, b) -> {
            int cmp = Long.compare(ages[a], ages[b]);
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        int want = Math.min(request, candidates.size());
        short[] flagsBefore = flags.clone();
        long[] agesBefore = ages.clone();
        int[] got = ResidentTrimPlanner.oldestSlots(flags, ages, count, request);
        if (got.length != want) throw new AssertionError("wrong number of victims");
        for (int i = 0; i < want; i++) {
            if (got[i] != candidates.get(i)) {
                throw new AssertionError("oldest mismatch: request=" + request
                        + " count=" + count + " position=" + i + " expected="
                        + candidates.get(i) + " actual=" + got[i]);
            }
        }
        if (!Arrays.equals(flags, flagsBefore) || !Arrays.equals(ages, agesBefore)) {
            throw new AssertionError("caller array mutated");
        }
        checked++;
        selected += got.length;
    }

    private static void exhaustive() {
        for (int n = 0; n <= 6; n++) {
            int agesCombinations = 1;
            for (int i = 0; i < n; i++) agesCombinations *= 3;
            for (int mask = 0; mask < (1 << n); mask++) {
                short[] flags = new short[n + 2];
                for (int i = 0; i < n; i++) if ((mask & (1 << i)) != 0) flags[i] = LazyElementFlags.PINNED;
                flags[n] = LazyElementFlags.PINNED;
                flags[n + 1] = LazyElementFlags.PINNED;
                for (int ageMode = 0; ageMode < agesCombinations; ageMode++) {
                    long[] ages = new long[n + 2];
                    int digits = ageMode;
                    for (int i = 0; i < n; i++) {
                        ages[i] = (digits % 3) - 1L;
                        digits /= 3;
                    }
                    for (int k = 0; k <= n + 2; k++) verify(flags, ages, n, k);
                }
            }
        }
    }

    private static void randomized() {
        Random random = new Random(0x6d3352515f4c5255L);
        for (int round = 0; round < 7000; round++) {
            int n = random.nextInt(256);
            int extra = random.nextInt(4);
            short[] flags = new short[n + extra];
            long[] ages = new long[n + extra];
            for (int i = 0; i < flags.length; i++) {
                flags[i] = random.nextInt(4) == 0 ? LazyElementFlags.PINNED : 0;
                ages[i] = random.nextBoolean() ? EXTRAS[random.nextInt(EXTRAS.length)] : random.nextLong();
            }
            int request = switch (round % 8) {
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> n / 8;
                case 4 -> n / 4;
                case 5 -> n / 2;
                case 6 -> n;
                default -> n + 32;
            };
            verify(flags, ages, n, request);
        }
        int big = 20000;
        short[] flags = new short[big];
        long[] ages = new long[big];
        for (int i = 0; i < big; i++) {
            flags[i] = i % 7 == 0 ? LazyElementFlags.PINNED : 0;
            ages[i] = (i % 23) - 11L;
        }
        for (int k : new int[] {0, 1, 2, 8, 100, 1000, 5000, 20000, 21000}) {
            verify(flags, ages, big, k);
        }
    }

    private static void bad(Runnable action, Class<? extends Throwable> expected) {
        try { action.run(); }
        catch (Throwable actual) { if (expected.isInstance(actual)) { checked++; return; }
            throw new AssertionError("wrong exception: " + actual, actual); }
        throw new AssertionError("missing expected exception: " + expected.getName());
    }

    private static void rejectsInvalid() {
        bad(() -> ResidentTrimPlanner.oldestSlots(null, new long[0], 0, 0), NullPointerException.class);
        bad(() -> ResidentTrimPlanner.oldestSlots(new short[0], null, 0, 0), NullPointerException.class);
        bad(() -> ResidentTrimPlanner.oldestSlots(new short[0], new long[0], -1, 0), IllegalArgumentException.class);
        bad(() -> ResidentTrimPlanner.oldestSlots(new short[0], new long[0], 1, 0), IllegalArgumentException.class);
        bad(() -> ResidentTrimPlanner.oldestSlots(new short[1], new long[0], 1, 0), IllegalArgumentException.class);
        bad(() -> ResidentTrimPlanner.oldestSlots(new short[0], new long[0], 0, -1), IllegalArgumentException.class);
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        exhaustive();
        randomized();
        rejectsInvalid();
        // Keep the regression output independent of heap implementation and JVM mode.
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((checked + ":" + selected).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("PASS cases=" + checked + " victims=" + selected + " sha256="
                + java.util.HexFormat.of().formatHex(digest.digest()));
    }
}
