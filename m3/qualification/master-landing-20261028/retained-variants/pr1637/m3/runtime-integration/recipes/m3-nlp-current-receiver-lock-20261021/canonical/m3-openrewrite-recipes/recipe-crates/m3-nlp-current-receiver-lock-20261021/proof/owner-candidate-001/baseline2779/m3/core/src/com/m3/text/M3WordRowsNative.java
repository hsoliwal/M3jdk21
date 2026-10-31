/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Optional, explicitly loaded JNI flags batch over the *same* validated M3 word image.
 * The Java implementation is always available; JNI is not an automatic runtime dependency.
 */
public final class M3WordRowsNative {
    private static volatile boolean loaded;
    private M3WordRowsNative() {}

    /** Explicit opt-in to an independently built, verified library. */
    public static synchronized void load(Path library) {
        Objects.requireNonNull(library, "library");
        if (!loaded) {
            System.load(library.toAbsolutePath().toString());
            loaded = true;
        }
    }

    public static boolean available() { return loaded; }

    /** Caller-owned result; every ID is checked before JNI or Java execution. */
    public static int[] flagsBatch(M3WordRows rows, int[] localIds) {
        M3WordRows source = Objects.requireNonNull(rows, "rows");
        int[] ids = Objects.requireNonNull(localIds, "localIds");
        int[] result = new int[ids.length];
        for (int id : ids) source.rowByteOffset(id);
        if (loaded && ids.length != 0) {
            bulkFlags0(source.nativeImage(), source.nativeRowsBase(), source.wordCount(),
                    ids, result);
        } else {
            for (int i = 0; i < ids.length; i++) result[i] = source.flags(ids[i]);
        }
        return result;
    }

    private static native void bulkFlags0(ByteBuffer image, int rowsBase,
                                          int wordCount, int[] localIds, int[] output);
}
