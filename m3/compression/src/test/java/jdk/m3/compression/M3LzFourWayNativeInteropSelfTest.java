// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compression;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * Bounded, deterministic Java/C17-JNI differential for the opt-in four-way
 * candidate index.  Regex- and Java-looking bytes are opaque input: no source
 * transformation, regex execution or codec routing occurs in this oracle.
 */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/test/java/com/synexia/algorithms/compression/M3LzFourWayNativeInteropSelfTest.java blob 9fa22af28708; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzFourWayNativeInteropSelfTest {
    private static final int BLOCKS = 128;
    private static final int MAX_LANES = 4096;
    private static long checks;

    private M3LzFourWayNativeInteropSelfTest() { }

    public static void main(String[] args) throws Exception {
        runAll();
    }

    public static void runAll() throws Exception {
        String configured = System.getProperty("m3.lz.library", "");
        M3LzNative backend = configured.isBlank() ? null : M3LzNative.load(Path.of(configured));
        Random random = new Random(0x4d334c5a5f4a4e49L);
        long totalLanes = 0;
        long directHits = 0;
        long fourWayHits = 0;
        for (int block = 0; block < BLOCKS; block++) {
            byte[] input = block(block, random);
            byte[] unchanged = input.clone();
            M3LzMatchCandidateIndex direct = new M3LzMatchCandidateIndex(9);
            M3LzFourWayCandidateIndex four = new M3LzFourWayCandidateIndex(9);
            direct.begin(input);
            four.begin(input);
            int[] left = new int[MAX_LANES];
            int[] right = new int[MAX_LANES];
            int[] limit = new int[MAX_LANES];
            int count = 0;
            int maximumDistance = block % 7 == 0 ? 1 : 1 + random.nextInt(65535);
            for (int position = 0; position <= input.length - 4; position++) {
                int original = direct.findAndRecord(position, maximumDistance);
                int candidate = four.findAndRecord(position, maximumDistance);
                if (original >= 0) directHits++;
                if (candidate < 0) continue;
                fourWayHits++;
                check(candidate < position && position - candidate <= maximumDistance,
                        "candidate distance");
                for (int j = 0; j < 4; j++) {
                    check(input[candidate + j] == input[position + j], "full-key check");
                }
                int maximum = input.length - position;
                int scalar = 0;
                while (scalar < maximum && input[candidate + scalar] == input[position + scalar]) {
                    scalar++;
                }
                check(M3LzByteKernels.commonPrefix(input, candidate, position, maximum)
                        == scalar, "Java JDK-intrinsic/scalar prefix parity");
                if (count == left.length) {
                    compare(backend, input, left, right, limit, count);
                    totalLanes += count;
                    count = 0;
                }
                left[count] = candidate;
                right[count] = position;
                limit[count] = maximum;
                count++;
            }
            if (count != 0) {
                compare(backend, input, left, right, limit, count);
                totalLanes += count;
            }
            four.clear();
            direct.clear();
            check(Arrays.equals(input, unchanged), "read-only binary input");
        }
        // Empty JNI lanes, repeated offsets, maximum zero and explicit short windows.
        byte[] exact = "class R { String r=\"(?s)a.*b\"; /* (?<x>\\\\w+) */ }"
                .getBytes(StandardCharsets.UTF_8);
        compare(backend, exact, new int[] {0, 5, exact.length, 3},
                new int[] {0, 5, exact.length, 3}, new int[] {0, 0, 0, 0}, 4);
        compare(backend, new byte[0], new int[0], new int[0], new int[0], 0);
        System.out.println("M3 four-way Java/JNI checks=" + checks + "; lanes=" + totalLanes
                + "; direct-hits=" + directHits + "; four-way-hits=" + fourWayHits
                + "; JNI=" + (backend != null ? "executed" : "not requested"));
    }

    private static byte[] block(int index, Random random) {
        byte[] bytes;
        switch (index % 8) {
            case 0 -> {
                bytes = new byte[4 + random.nextInt(1536)];
                random.nextBytes(bytes);
            }
            case 1 -> {
                bytes = new byte[4 + random.nextInt(1536)];
                Arrays.fill(bytes, (byte) ('a' + (index % 26)));
            }
            case 2 -> {
                bytes = new byte[4 + random.nextInt(1536)];
                for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i & 15);
            }
            case 3 -> bytes = (
                    "class Example { String x = \"return false; // (?s).*\"; "
                    + "// if (true) yield 12;\\n return \"(?<name>[\\\\p{L}]+)\"; }")
                    .repeat(1 + index % 7).getBytes(StandardCharsets.UTF_8);
            case 4 -> bytes = (
                    "(?m)^(?:public|private).*\\\\bclass\\\\s+\\\\w+ // (?<nested>.*)\\u0000"
                    + "\ud800\udc00\udfff")
                    .repeat(1 + index % 8).getBytes(StandardCharsets.UTF_8);
            case 5 -> {
                bytes = new byte[4 + random.nextInt(1536)];
                for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i % 37 == 0 ? 0 : -1);
            }
            case 6 -> bytes = ("switch (a) { case 0 -> 1; default -> 2; }\n"
                    + "// regexp: (?=x)(?!y)\\\\1\\\\p{Lu}\n").repeat(15)
                    .getBytes(StandardCharsets.UTF_8);
            default -> {
                bytes = new byte[4 + random.nextInt(1536)];
                random.nextBytes(bytes);
                for (int i = 4; i < bytes.length; i += 31) {
                    bytes[i] = bytes[i - 4];
                }
            }
        }
        return bytes;
    }

    private static void compare(M3LzNative backend, byte[] bytes,
            int[] left, int[] right, int[] maximum, int length) {
        int[] a = Arrays.copyOf(left, length);
        int[] b = Arrays.copyOf(right, length);
        int[] m = Arrays.copyOf(maximum, length);
        int[] expected = M3LzByteKernels.commonPrefixes(bytes, a, b, m);
        check(expected.length == length, "Java lane shape");
        if (backend != null) {
            int[] nativeResult = backend.commonPrefixes(bytes, a, b, m);
            check(Arrays.equals(expected, nativeResult), "Java/C17 JNI full prefix parity");
        }
    }

    private static void check(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description + " at check=" + checks);
    }
}
