// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

/** Dependency-free Java 21 parity/refusal and collision-corpus witness. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzFourWayCandidateSelfTest.java blob 6a6d9fcd324e; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzFourWayCandidateSelfTest {
    private static long checks;
    private M3LzFourWayCandidateSelfTest() { }

    public static void main(String[] args) throws Exception {
        runAll();
    }

    public static void runAll() throws Exception {
        reject(() -> new M3LzFourWayCandidateIndex(7));
        reject(() -> new M3LzFourWayCandidateIndex(21));
        M3LzFourWayCandidateIndex index = new M3LzFourWayCandidateIndex(8);
        rejectState(() -> index.findAndRecord(0, 1));
        byte[] repeated = new byte[32];
        Arrays.fill(repeated, (byte) 'A');
        index.begin(repeated);
        check(index.findAndRecord(0, 65535) == -1, "first reference");
        for (int i = 1; i <= repeated.length - 4; i++) {
            check(index.findAndRecord(i, 1) == i - 1, "immediate predecessor");
        }
        index.begin(repeated);
        check(index.findAndRecord(0, 65535) == -1, "epoch isolation");
        index.clear();
        rejectState(() -> index.findAndRecord(0, 1));
        index.begin(new byte[] {1, 2, 3});
        reject(() -> index.findAndRecord(0, 1));
        index.begin(repeated);
        reject(() -> index.findAndRecord(-1, 1));
        reject(() -> index.findAndRecord(repeated.length - 3, 1));
        reject(() -> index.findAndRecord(0, 0));

        byte[] mutable = new byte[8];
        Arrays.fill(mutable, (byte) 'X');
        index.begin(mutable);
        check(index.findAndRecord(0, 8) == -1, "mutable initial");
        mutable[0] = (byte) 'Y';
        check(index.findAndRecord(1, 8) == -1, "mutable original invalidated");
        index.clear();

        // Exercise the generation wrap, including previously active slots.
        index.begin(repeated);
        check(index.findAndRecord(0, 16) == -1, "wrap precondition");
        Field generation = M3LzFourWayCandidateIndex.class.getDeclaredField("epoch");
        generation.setAccessible(true);
        generation.setInt(index, -1);
        index.begin(repeated);
        check(index.findAndRecord(1, 16) == -1, "wrapped epoch clears slots");

        Random random = new Random(0x4d33465741594c5aL);
        M3LzFourWayCandidateIndex a = new M3LzFourWayCandidateIndex(9);
        M3LzFourWayCandidateIndex b = new M3LzFourWayCandidateIndex(9);
        for (int block = 0; block < 320; block++) {
            byte[] data = new byte[4 + random.nextInt(1200)];
            random.nextBytes(data);
            if (block % 3 == 0) Arrays.fill(data, (byte) block);
            if (block % 5 == 0) {
                for (int i = 0; i < data.length; i++) data[i] = (byte) (i & 15);
            }
            byte[] unchanged = data.clone();
            int maxDistance = 1 + random.nextInt(2048);
            a.begin(data);
            b.begin(data);
            for (int position = 0; position <= data.length - 4; position++) {
                int first = a.findAndRecord(position, maxDistance);
                int second = b.findAndRecord(position, maxDistance);
                check(first == second, "stable replay");
                if (first >= 0) {
                    check(first < position && position - first <= maxDistance,
                            "bounded backward candidate");
                    for (int offset = 0; offset < 4; offset++) {
                        check(data[first + offset] == data[position + offset],
                                "byte exact, not hash equal");
                    }
                }
            }
            check(Arrays.equals(data, unchanged), "read-only input");
            a.clear();
            b.clear();
        }

        // Four distinct keys share a direct-mapped 8-bit hash slot. At the
        // same 256-entry memory budget, four-way retention avoids evictions.
        int[] keys = fourKeysForOneDirectSlot();
        byte[] corpus = new byte[4 * 4 * 40];
        for (int word = 0; word < corpus.length / 4; word++) {
            int value = keys[word & 3];
            for (int j = 0; j < 4; j++) corpus[4 * word + j] = (byte) (value >>> (8 * j));
        }
        M3LzMatchCandidateIndex direct = new M3LzMatchCandidateIndex(8);
        M3LzFourWayCandidateIndex four = new M3LzFourWayCandidateIndex(8);
        direct.begin(corpus);
        four.begin(corpus);
        int directMatches = 0;
        int fourMatches = 0;
        for (int offset = 0; offset < corpus.length; offset += 4) {
            if (direct.findAndRecord(offset, 256) >= 0) directMatches++;
            if (four.findAndRecord(offset, 256) >= 0) fourMatches++;
        }
        check(directMatches == 0, "direct mapped collision baseline");
        check(fourMatches == corpus.length / 4 - 4, "four-way collision retention");
        System.out.println("M3 four-way LZ checks=" + checks
                + " direct-collision-hits=" + directMatches
                + " four-way-collision-hits=" + fourMatches);
    }

    private static int[] fourKeysForOneDirectSlot() {
        int[] count = new int[256];
        int[][] candidates = new int[256][4];
        for (int key = 1; key <= 20000; key++) {
            int slot = mix(key) & 255;
            if (count[slot] < 4) candidates[slot][count[slot]++] = key;
            if (count[slot] == 4) return candidates[slot].clone();
        }
        throw new AssertionError("could not construct collision witness");
    }

    private static int mix(int word) {
        word ^= word >>> 16;
        word *= 0x7feb352d;
        word ^= word >>> 15;
        word *= 0x846ca68b;
        return word ^ (word >>> 16);
    }

    private static void reject(Runnable work) {
        try { work.run(); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("invalid source range accepted");
    }

    private static void rejectState(Runnable work) {
        try { work.run(); }
        catch (IllegalStateException expected) { checks++; return; }
        throw new AssertionError("inactive source accepted");
    }

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason + " at check " + checks);
    }
}
