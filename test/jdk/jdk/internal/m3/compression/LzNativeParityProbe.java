// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.SplittableRandom;

/**
 * Native parity probe of the native leaf (test scope): over the format oracle's seeded inputs
 * ({@link M3CompressionFormatOracle#input(int)}) the JNI batch kernel ({@link M3LzNative#commonPrefixes}) and the Java
 * kernel ({@link M3LzByteKernels#commonPrefixes}) must return identical prefix lanes. Each input contributes the lanes
 * an LZ parse over the direct-mapped {@link M3LzMatchCandidateIndex} asks for (candidate, position, bytes to the end)
 * plus seeded random lanes (identical offsets, zero limits, full spans included), at most 65,536 lanes per call, and
 * the input array must be unchanged after the call.
 *
 * <p>The library comes from the first argument or the system property {@code m3.lz.library}; without one the probe
 * prints {@code M3_LZ_NATIVE_PARITY=SKIPPED} (the Java kernels stay the default backend). Prints
 * {@code M3_LZ_NATIVE_PARITY_*} lines and throws {@link AssertionError} on any difference.
 * usage: {@code M3LzNativeParityProbe [library [inputs]]}
 */
public final class LzNativeParityProbe {
    private static final int MAX_LANES = 65_536;
    private static final int RANDOM_LANES = 64;
    private static final int TABLE_BITS = 12;
    private static final long SEED = 0x4d33_4c5a_5041_5249L;
    private static final String PREFIX = "M3_LZ_NATIVE_PARITY_";

    private LzNativeParityProbe() {}

    public static void main(String[] args) throws Exception {
        String library = args.length > 0 ? args[0] : System.getProperty("m3.lz.library", "");
        int inputs = args.length > 1 ? Integer.parseInt(args[1]) : CompressionFormatOracle.DEFAULT_INPUTS;
        System.out.print(run(library, inputs));
    }

    /** The JUnit lane: runs only when {@code m3.lz.library} names a built library. */
    public static String runIfConfigured() throws Exception {
        return run(System.getProperty("m3.lz.library", ""), CompressionFormatOracle.DEFAULT_INPUTS);
    }

    static String run(String library, int inputs) throws Exception {
        if (library.isBlank()) {
            return "M3_LZ_NATIVE_PARITY=SKIPPED (no m3.lz.library; the Java kernels are the backend)\n";
        }
        LzNative backend = LzNative.load(Path.of(library));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        LzMatchCandidateIndex index = new LzMatchCandidateIndex(TABLE_BITS);
        long lanes = 0;
        long matched = 0;
        long calls = 0;
        for (int i = 0; i < inputs; i++) {
            byte[] data = CompressionFormatOracle.input(i);
            int[][] batch = lanes(data, index, new SplittableRandom(SEED ^ i));
            byte[] before = data.clone();
            int[] java = LzByteKernels.commonPrefixes(data, batch[0], batch[1], batch[2]);
            int[] jni = backend.commonPrefixes(data, batch[0], batch[1], batch[2]);
            if (!Arrays.equals(java, jni) || !Arrays.equals(before, data)) {
                throw new AssertionError("native and Java prefix lanes differ at input " + i + " ("
                        + CompressionFormatOracle.category(i) + ")");
            }
            for (int value : java) {
                matched += value;
                digest.update(new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8),
                        (byte) value});
            }
            lanes += java.length;
            calls++;
        }
        return PREFIX + "INPUTS=" + inputs + "\n" + PREFIX + "CALLS=" + calls + "\n" + PREFIX + "LANES=" + lanes + "\n"
                + PREFIX + "PREFIX_BYTES=" + matched + "\n" + PREFIX + "SHA256="
                + HexFormat.of().formatHex(digest.digest()) + "\n" + PREFIX + "RESULT=PASS\n";
    }

    /** Parse lanes (candidate, position, bytes to the end) of one LZ pass, then seeded random lanes. */
    private static int[][] lanes(byte[] data, LzMatchCandidateIndex index, SplittableRandom random) {
        int[] left = new int[MAX_LANES];
        int[] right = new int[MAX_LANES];
        int[] limit = new int[MAX_LANES];
        int count = 0;
        index.begin(data);
        try {
            for (int position = 0; position <= data.length - Integer.BYTES && count < MAX_LANES - RANDOM_LANES;
                    position++) {
                int match = index.findAndRecord(position, 0xffff);
                if (match >= 0) {
                    left[count] = match;
                    right[count] = position;
                    limit[count++] = data.length - position;
                }
            }
        } finally {
            index.clear();
        }
        for (int r = 0; r < RANDOM_LANES; r++, count++) {
            int a = random.nextInt(data.length + 1);
            int b = r % 8 == 0 ? a : random.nextInt(data.length + 1);
            int room = data.length - Math.max(a, b);
            left[count] = a;
            right[count] = b;
            limit[count] = r % 16 == 1 ? 0 : r % 16 == 2 ? room : random.nextInt(room + 1);
        }
        return new int[][] {Arrays.copyOf(left, count), Arrays.copyOf(right, count), Arrays.copyOf(limit, count)};
    }
}
