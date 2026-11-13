// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Explicit opt-in C17 JNI batch rank selection; default Java index never loads JNI. */
public final class M3MultiRankedByteSubstringsNative {
    private static final boolean ENABLED;
    static {
        String path = System.getProperty("m3.cpu.multi.ranked.native.path");
        if (path == null || path.isEmpty()) {
            ENABLED = false;
        } else {
            if (!Path.of(path).isAbsolute())
                throw new IllegalArgumentException("native library path must be absolute");
            System.load(path);
            ENABLED = true;
        }
    }

    private M3MultiRankedByteSubstringsNative() { }

    public static boolean isAvailable() { return ENABLED; }

    /** Fresh (document,relativeOffset,length) triples, exactly matching Java. */
    public static int[] selectSpans(M3MultiRankedByteSubstrings index, long[] ranks) {
        Objects.requireNonNull(index, "index");
        index.validateBatch(ranks);
        if (!ENABLED) throw new IllegalStateException("multi ranked native backend not configured");
        return select0(index.suffixDocumentCopy(), index.suffixOffsetCopy(),
                index.documentStartCopy(), index.lcpCopy(), index.cumulativeCopy(), ranks);
    }

    private static native int[] select0(int[] documentIds, int[] packedOffsets,
            int[] documentStarts, int[] lcp, long[] cumulative, long[] ranks);
}
