// SPDX-License-Identifier: Apache-2.0
package com.m3.precompute;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Optional JNI accelerator; the Java provider remains the semantic reference. */
public final class M3CodeTextSignalBatchNative implements M3CodeTextSignalBatch {
    private static final int ROW_BATCH = 256;
    private static volatile boolean loaded;

    private M3CodeTextSignalBatchNative() {}

    public static M3CodeTextSignalBatchNative loadRequired() {
        if (!loaded) {
            synchronized (M3CodeTextSignalBatchNative.class) {
                if (!loaded) {
                    loadLibrary();
                    loaded = true;
                }
            }
        }
        return new M3CodeTextSignalBatchNative();
    }

    public static boolean isLoaded() {
        return loaded;
    }

    private static void loadLibrary() {
        String explicit = System.getProperty("m3.precompute.native.path");
        if (explicit != null && !explicit.isBlank()) {
            System.load(Path.of(explicit).toAbsolutePath().normalize().toString());
            return;
        }
        String pathFile = System.getProperty("m3.precompute.native.pathFile");
        if (pathFile != null && !pathFile.isBlank()) {
            try {
                String path = Files.readString(Path.of(pathFile)).strip();
                if (path.isEmpty()) throw new IllegalStateException("empty M3 native path file");
                System.load(Path.of(path).toAbsolutePath().normalize().toString());
                return;
            } catch (IOException failure) {
                throw new IllegalStateException("cannot read M3 native path file", failure);
            }
        }
        System.loadLibrary("m3_precompute");
    }

    @Override
    public String name() {
        return "jni-c11";
    }

    @Override
    public boolean accelerated() {
        return true;
    }

    @Override
    public long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            Limits limits,
            M3Progress monitor) {
        if (!loaded) {
            throw new IllegalStateException("M3 precompute JNI library is not loaded");
        }
        M3CodeTextSignalBatchJava.validate(units, offsets, lengths, limits);
        M3Progress progress = monitor == null ? M3Progress.none() : monitor;
        long[] result = new long[offsets.length];
        progress.begin("M3 JNI code-text lexical signals", result.length);
        try {
            for (int from = 0; from < result.length; from += ROW_BATCH) {
                progress.checkCanceled();
                int to = Math.min(result.length, from + ROW_BATCH);
                long[] part = nativeAnalyzeRange(units, offsets, lengths, from, to);
                if (part == null || part.length != to - from) {
                    throw new IllegalStateException("invalid JNI code-text result");
                }
                System.arraycopy(part, 0, result, from, part.length);
                progress.worked(part.length);
            }
            progress.checkCanceled();
            return result;
        } finally {
            progress.done();
        }
    }

    private static native long[] nativeAnalyzeRange(
            char[] units, int[] offsets, int[] lengths, int fromRow, int toRow);
}
