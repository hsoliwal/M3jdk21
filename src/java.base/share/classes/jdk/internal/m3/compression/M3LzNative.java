// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

/** Explicitly loaded JNI batch matcher; never replaces the Java oracle implicitly. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/6fdee5330b8ab005005785aeff0d1105ba40d12c/synexia-algo/src/main/java/com/synexia/algorithms/compression/M3LzNative.java blob 6b3eae8f9c9a; recipe m3/compression/recipe/compression-20261106/relocate.py v3
public final class M3LzNative {
    private static final int ABI = 1;
    private static final int MAX_BLOCK_BYTES = 16 * 1024 * 1024;
    private static final int MAX_LANES = 65_536;
    private static Path loaded;
    private static boolean qualified;

    private M3LzNative() {}

    public static synchronized M3LzNative load(Path library) throws IOException {
        Path path = Objects.requireNonNull(library, "library")
                .toAbsolutePath().normalize().toRealPath();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("native library must be a regular file");
        }
        if (loaded == null) {
            System.load(path.toString());
            loaded = path;
        } else if (!loaded.equals(path)) {
            throw new IllegalStateException("different M3 LZ JNI library already loaded");
        }
        if (!qualified) {
            if (nativeAbi() != ABI) {
                throw new LinkageError("M3 LZ native ABI mismatch");
            }
            proveParity();
            qualified = true;
        }
        return new M3LzNative();
    }

    public static synchronized boolean available() {
        return qualified;
    }

    public int[] commonPrefixes(byte[] input, int[] left, int[] right, int[] maximum) {
        if (!available()) {
            throw new IllegalStateException("M3 LZ JNI backend is not qualified");
        }
        M3LzByteKernels.checkBatch(input, left, right, maximum);
        if (input.length > MAX_BLOCK_BYTES || left.length > MAX_LANES) {
            throw new IllegalArgumentException("M3 LZ native batch is too large");
        }
        int[] nativeResult = nativeMatches(input, left, right, maximum);
        if (nativeResult == null || nativeResult.length != left.length) {
            throw new LinkageError("M3 LZ JNI returned wrong shape");
        }
        for (int i = 0; i < nativeResult.length; i++) {
            if (nativeResult[i] < 0 || nativeResult[i] > maximum[i]) {
                throw new LinkageError("M3 LZ JNI returned invalid prefix");
            }
        }
        return nativeResult;
    }

    private static void proveParity() {
        byte[] data = new byte[1_025];
        long state = 0x1234_5678_9abc_def0L;
        for (int i = 0; i < data.length; i++) {
            state ^= state << 13;
            state ^= state >>> 7;
            state ^= state << 17;
            data[i] = (byte) state;
        }
        int[] left = new int[513];
        int[] right = new int[513];
        int[] limits = new int[513];
        for (int i = 0; i < left.length; i++) {
            left[i] = i;
            right[i] = i % 3 == 0 ? i : (1024 - i);
            limits[i] = Math.min(1_025 - Math.max(left[i], right[i]), i % 91);
        }
        int[] actual = nativeMatches(data, left, right, limits);
        if (!Arrays.equals(M3LzByteKernels.commonPrefixes(data, left, right, limits), actual)) {
            throw new LinkageError("M3 LZ JNI parity probe failed");
        }
    }

    private static native int nativeAbi();
    private static native int[] nativeMatches(byte[] input, int[] left, int[] right, int[] maximum);
}
