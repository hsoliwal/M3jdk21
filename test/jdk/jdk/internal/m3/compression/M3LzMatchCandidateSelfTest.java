// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

/** Checks candidate correctness and generation isolation against scalar witnesses. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzMatchCandidateSelfTest.java blob aad6c22eaf27; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzMatchCandidateSelfTest {
    private static int checks;

    private M3LzMatchCandidateSelfTest() {}

    public static void main(String[] args) {
        runAll();
    }

    public static void runAll() {
        reject(() -> new M3LzMatchCandidateIndex(7));
        reject(() -> new M3LzMatchCandidateIndex(21));
        M3LzMatchCandidateIndex index = new M3LzMatchCandidateIndex(8);
        rejectState(() -> index.findAndRecord(0, 1));
        byte[] repeated = "aaaaaaaaaaaa".getBytes(StandardCharsets.US_ASCII);
        index.begin(repeated);
        check(index.findAndRecord(0, 65535) == -1, "first position");
        for (int i = 1; i <= repeated.length - 4; i++) {
            check(index.findAndRecord(i, 1) == i - 1, "bounded immediate predecessor");
        }
        index.begin(repeated);
        check(index.findAndRecord(0, 65535) == -1, "generation isolation");
        index.clear();
        rejectState(() -> index.findAndRecord(0, 2));
        index.begin(new byte[] {1, 2, 3});
        reject(() -> index.findAndRecord(0, 1));
        index.begin(repeated);
        reject(() -> index.findAndRecord(-1, 1));
        reject(() -> index.findAndRecord(repeated.length - 3, 1));
        reject(() -> index.findAndRecord(0, 0));

        Random rng = new Random(0x4d334d41_544348L);
        M3LzMatchCandidateIndex a = new M3LzMatchCandidateIndex(8);
        M3LzMatchCandidateIndex b = new M3LzMatchCandidateIndex(8);
        for (int block = 0; block < 400; block++) {
            byte[] source = new byte[4 + rng.nextInt(2000)];
            rng.nextBytes(source);
            if (block % 3 == 0) Arrays.fill(source, (byte) 0x5a);
            if (block % 5 == 0) {
                for (int i = 0; i < source.length; i++) source[i] = (byte) (i & 15);
            }
            byte[] before = source.clone();
            a.begin(source);
            b.begin(source);
            int maximumDistance = 1 + rng.nextInt(4096);
            for (int pos = 0; pos <= source.length - 4; pos++) {
                int first = a.findAndRecord(pos, maximumDistance);
                int second = b.findAndRecord(pos, maximumDistance);
                check(first == second, "repeated replay");
                if (first >= 0) {
                    check(first < pos && pos - first <= maximumDistance, "distance window");
                    for (int byteIndex = 0; byteIndex < 4; byteIndex++) {
                        check(source[first + byteIndex] == source[pos + byteIndex], "exact key witness");
                    }
                    int extended = M3LzByteKernels.commonPrefix(source, first, pos,
                            Math.min(source.length - first, source.length - pos));
                    check(extended >= 4, "prefix extension");
                }
            }
            check(Arrays.equals(before, source), "read only");
        }
        byte[] mutable = "aaaaaaaa".getBytes(StandardCharsets.US_ASCII);
        a.begin(mutable);
        check(a.findAndRecord(0, 8) == -1, "mutable start");
        mutable[0] = (byte) 'b';
        check(a.findAndRecord(1, 8) == -1, "mutable key invalidation");
        System.out.println("M3 LZ index oracle checks=" + checks);
    }

    private static void reject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException allowed) {
            checks++;
            return;
        }
        throw new AssertionError("invalid index request accepted");
    }

    private static void rejectState(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException allowed) {
            checks++;
            return;
        }
        throw new AssertionError("inactive index request accepted");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message + " at check=" + checks);
    }
}
