// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;

/**
 * Qualification driver (not a test): seeded inputs through the four restored lane kernels, one SHA-256 per
 * kernel over every output lane, return value, mutated input and exception class. Mode "java" requests the
 * Java paths (nativeRequired=false, Backend.JAVA); mode "native" requests the JNI paths (nativeRequired=true,
 * Backend.NATIVE_REQUIRED). The printed lines carry no mode, so the two runs must be byte-identical.
 */
//FLLDCIM: TOP - qualification/lane-kernel-native-20261106 differential driver; native vs Java byte identity
public final class M3LaneKernelDigest {
    private M3LaneKernelDigest() { }

    public static void main(String[] args) throws Exception {
        boolean nativeMode = "native".equals(args[0]);
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261106L;
        int rounds = args.length > 2 ? Integer.parseInt(args[2]) : 400;
        if (nativeMode && !(M3LongAddressKernel.nativeAvailable()
                && M3LaneAlgorithms.slidingMaximumNativeAvailable()
                && M3LaneAlgorithms.partition3NativeAvailable())) {
            System.err.println("native mode requested but a lane JNI export is unavailable");
            System.exit(3);
        }
        Random random = new Random(seed);
        MessageDigest[] digests = new MessageDigest[4];
        for (int i = 0; i < 4; i++) digests[i] = MessageDigest.getInstance("SHA-256");
        int[] cases = new int[4];
        for (int round = 0; round < rounds; round++) {
            int size = round % 7 == 0 ? random.nextInt(5) : 1 + random.nextInt(round % 3 == 0 ? 4100 : 600);
            gather(random, size, nativeMode, digests[0]);
            cases[0]++;
            if (size > 0) {
                sliding(random, size, nativeMode, digests[1]);
                cases[1]++;
            }
            partitionLong(random, size, nativeMode, digests[2]);
            cases[2]++;
            partitionInt(random, size, nativeMode, digests[3]);
            cases[3]++;
        }
        String[] names = {"gatherSelected", "slidingMaximumIndices", "partition3Long", "partition3Int"};
        for (int i = 0; i < 4; i++) {
            System.out.println("KERNEL " + names[i] + " seed=" + seed + " cases=" + cases[i]
                    + " sha256=" + HexFormat.of().formatHex(digests[i].digest()));
        }
    }

    private static void gather(Random random, int size, boolean nativeMode, MessageDigest digest) {
        long[] payload = new long[1 + random.nextInt(size + 8)];
        for (int i = 0; i < payload.length; i++) payload[i] = random.nextLong();
        int from = random.nextInt(4);
        int[] addresses = new int[from + size + 3];
        byte[] states = new byte[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = random.nextInt(8) == 0 ? -1 - random.nextInt(9) : random.nextInt(payload.length);
            states[i] = (byte) random.nextInt(256);
        }
        int required = random.nextInt(4) == 0 ? 0 : 1 << random.nextInt(8);
        int forbidden = (random.nextInt(3) == 0 ? 0 : 1 << random.nextInt(8)) & ~required;
        long[] output = new long[size + 5];
        java.util.Arrays.fill(output, 0x5eedL);
        M3IndexMaskCompactor.Backend backend = nativeMode
                ? M3IndexMaskCompactor.Backend.NATIVE_REQUIRED : M3IndexMaskCompactor.Backend.JAVA;
        String outcome;
        try {
            outcome = "n=" + M3LongAddressKernel.gatherSelectedInto(payload, addresses, states, from, size,
                    output, 2, required, forbidden, backend);
        } catch (IndexOutOfBoundsException | IllegalArgumentException refused) {
            outcome = refused.getClass().getName() + ":" + refused.getMessage();
        }
        update(digest, outcome);
        update(digest, output);
        update(digest, payload);
    }

    private static void sliding(Random random, int size, boolean nativeMode, MessageDigest digest) {
        long[] values = new long[size];
        for (int i = 0; i < size; i++) {
            int pick = random.nextInt(10);
            values[i] = pick == 0 ? Long.MIN_VALUE : pick == 1 ? Long.MAX_VALUE : random.nextInt(21) - 10L;
        }
        int from = random.nextInt(size);
        int to = from + 1 + random.nextInt(size - from);
        int window = 1 + random.nextInt(to - from);
        int[] output = new int[to - from - window + 1 + 6];
        java.util.Arrays.fill(output, -17);
        M3LaneAlgorithms.slidingMaximumIndicesInto(values, from, to, window, output, 3, nativeMode);
        update(digest, "w=" + window);
        update(digest, output);
        update(digest, values);
    }

    private static void partitionLong(Random random, int size, boolean nativeMode, MessageDigest digest) {
        long[] lane = new long[size + 3];
        for (int i = 0; i < lane.length; i++) {
            int pick = random.nextInt(9);
            lane[i] = pick == 0 ? Long.MIN_VALUE : pick == 1 ? Long.MAX_VALUE
                    : pick == 2 ? random.nextLong() : random.nextInt(31) - 15L;
        }
        int from = random.nextInt(3);
        long pivot = random.nextInt(5) == 0 ? lane[random.nextInt(lane.length)] : random.nextInt(31) - 15L;
        long bounds = M3LaneAlgorithms.partition3Prepared(lane, from, from + size, pivot, nativeMode);
        update(digest, "b=" + bounds);
        update(digest, lane);
    }

    private static void partitionInt(Random random, int size, boolean nativeMode, MessageDigest digest) {
        int[] lane = new int[size + 3];
        for (int i = 0; i < lane.length; i++) {
            int pick = random.nextInt(9);
            lane[i] = pick == 0 ? Integer.MIN_VALUE : pick == 1 ? Integer.MAX_VALUE
                    : pick == 2 ? random.nextInt() : random.nextInt(31) - 15;
        }
        int from = random.nextInt(3);
        int pivot = random.nextInt(5) == 0 ? lane[random.nextInt(lane.length)] : random.nextInt(31) - 15;
        long bounds = M3LaneAlgorithms.partition3Prepared(lane, from, from + size, pivot, nativeMode);
        update(digest, "b=" + bounds);
        update(digest, lane);
    }

    private static void update(MessageDigest digest, String text) {
        digest.update(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        digest.update((byte) '\n');
    }

    private static void update(MessageDigest digest, long[] values) {
        update(digest, java.util.Arrays.toString(values));
    }

    private static void update(MessageDigest digest, int[] values) {
        update(digest, java.util.Arrays.toString(values));
    }
}
