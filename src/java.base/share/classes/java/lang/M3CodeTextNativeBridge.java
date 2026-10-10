/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package java.lang;

import java.util.Arrays;

/**
 * Optional native bridge for the Synexia code-text packed signal lane.
 *
 * <p>The bridge is opt-in through {@code m3.code.text.native.path}. The host-Java implementation
 * remains the reference behavior and is used whenever no library is configured. Native output
 * can be checked against that reference with {@code m3.code.text.native.verify=true}.</p>
 */
final class M3CodeTextNativeBridge {
    private static final Object LOAD_LOCK = new Object();
    private static volatile boolean attempted;
    private static volatile boolean loaded;

    private M3CodeTextNativeBridge() {}

    static boolean loadOptional() {
        if (attempted) {
            return loaded;
        }
        synchronized (LOAD_LOCK) {
            if (attempted) {
                return loaded;
            }
            attempted = true;
            String path = System.getProperty("m3.code.text.native.path");
            if (path == null || path.isBlank()) {
                return false;
            }
            try {
                System.load(path);
                loaded = true;
            } catch (UnsatisfiedLinkError | SecurityException failure) {
                if (Boolean.getBoolean("m3.code.text.native.required")) {
                    throw failure;
                }
            }
            return loaded;
        }
    }

    static boolean isLoaded() {
        return loaded;
    }

    static long[] analyze(
            char[] units,
            int[] offsets,
            int[] lengths,
            M3CodeTextSignalBatch.Limits limits) {
        M3CodeTextSignalBatch.validate(units, offsets, lengths, limits);
        if (!loadOptional()) {
            return M3CodeTextSignalBatch.analyze(units, offsets, lengths, limits);
        }
        try {
            long[] actual = nativeAnalyzeRange(units, offsets, lengths, 0, offsets.length);
            if (actual == null || actual.length != offsets.length) {
                throw new AssertionError("code-text JNI result geometry");
            }
            if (Boolean.getBoolean("m3.code.text.native.verify")) {
                long[] expected = M3CodeTextSignalBatch.analyze(units, offsets, lengths, limits);
                if (!Arrays.equals(actual, expected)) {
                    throw new AssertionError("code-text JNI result differs from host Java");
                }
            }
            return actual;
        } catch (UnsatisfiedLinkError failure) {
            if (Boolean.getBoolean("m3.code.text.native.required")) {
                throw failure;
            }
            loaded = false;
            return M3CodeTextSignalBatch.analyze(units, offsets, lengths, limits);
        }
    }

    private static native long[] nativeAnalyzeRange(
            char[] units, int[] offsets, int[] lengths, int fromRow, int toRow);
}
