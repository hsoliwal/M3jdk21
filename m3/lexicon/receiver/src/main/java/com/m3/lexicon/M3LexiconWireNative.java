// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.lexicon;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Objects;

/** Optional, explicit JNI bulk expansion over already-verified M3 CWID lanes. */
public final class M3LexiconWireNative {
    private M3LexiconWireNative() { }

    public static void load(Path library) {
        System.load(Objects.requireNonNull(library, "library").toAbsolutePath().toString());
    }

    /**
     * A byte-copy accelerator, not dictionary authentication. Requires distinct,
     * nonoverlapping direct buffers. All row bounds are checked before mutation.
     */
    public static native int expand(ByteBuffer arena, int[] offsets, int[] lengths,
            int[] cwids, ByteBuffer output);

    /** Java reference; caller guarantees no overlapping direct-buffer views. */
    public static int expandJava(ByteBuffer arena, int[] offsets, int[] lengths,
            int[] cwids, ByteBuffer output) {
        Objects.requireNonNull(arena, "arena");
        Objects.requireNonNull(offsets, "offsets");
        Objects.requireNonNull(lengths, "lengths");
        Objects.requireNonNull(cwids, "cwids");
        Objects.requireNonNull(output, "output");
        if (!arena.isDirect() || !output.isDirect() || output.isReadOnly() || arena == output
                || offsets.length != lengths.length) {
            throw new IllegalArgumentException("nonoverlapping direct buffers and equal row lanes required");
        }
        int total = 0;
        for (int cwid : cwids) {
            if (cwid <= 0 || cwid > lengths.length) throw new IllegalArgumentException("invalid CWID");
            int row = cwid - 1;
            int start = offsets[row];
            int size = lengths[row];
            if (start < 0 || size <= 0 || start > arena.capacity() - size
                    || size > output.capacity() - total) {
                throw new IllegalArgumentException("invalid native row bounds");
            }
            total += size;
        }
        ByteBuffer source = arena.duplicate().clear();
        ByteBuffer target = output.duplicate().clear();
        int write = 0;
        for (int cwid : cwids) {
            int row = cwid - 1;
            for (int i = 0; i < lengths[row]; i++) {
                target.put(write++, source.get(offsets[row] + i));
            }
        }
        return total;
    }
}
