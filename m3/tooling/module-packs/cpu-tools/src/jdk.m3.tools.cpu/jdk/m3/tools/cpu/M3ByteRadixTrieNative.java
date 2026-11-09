// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tools.cpu;

import java.nio.file.Path;
import java.util.Objects;

/** Explicit opt-in C17 JNI batch prefix-count provider for M3ByteRadixTrie. */
public final class M3ByteRadixTrieNative {
    private static final boolean AVAILABLE;
    static {
        String path = System.getProperty("m3.cpu.radix.native.path");
        if (path == null || path.isEmpty()) {
            AVAILABLE = false;
        } else {
            if (!Path.of(path).isAbsolute())
                throw new IllegalArgumentException("native library path must be absolute");
            System.load(path);
            AVAILABLE = true;
        }
    }

    private M3ByteRadixTrieNative() { }
    public static boolean isAvailable() { return AVAILABLE; }

    /** Returns new independent prefix counts in original query order. */
    public static int[] prefixCounts(M3ByteRadixTrie index, byte[] packed,
                                      int[] offsets, int[] lengths) {
        M3ByteRadixTrie owner = Objects.requireNonNull(index, "index");
        byte[] data = Objects.requireNonNull(packed, "packed").clone();
        int[] starts = Objects.requireNonNull(offsets, "offsets").clone();
        int[] sizes = Objects.requireNonNull(lengths, "lengths").clone();
        M3ByteRadixTrie.validateSlices(data, starts, sizes);
        if (!AVAILABLE) throw new IllegalStateException("radix trie JNI backend not configured");
        return prefixCounts0(owner.ownedNodeStarts(), owner.ownedEdges(),
                owner.ownedPrefixTotals(), data, starts, sizes);
    }

    private static native int[] prefixCounts0(int[] nodeStarts, int[] encodedEdges,
            int[] prefixTotals, byte[] packed, int[] starts, int[] lengths);
}
