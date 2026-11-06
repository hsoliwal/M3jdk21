// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/JavaAdaptiveSearchEngine.java (blob 63426d37c920) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import java.util.Arrays;

/**
 * Deterministic Java byte-search provider.
 *
 * <p>Short or highly repetitive needles use KMP. Longer diverse needles use a precomputed
 * Boyer-Moore-Horspool shift table. Caller arrays are never retained.</p>
 */
public final class M3JavaAdaptiveSearchEngine implements M3SearchEngine {
    @Override
    public String name() {
        return "JAVA_ADAPTIVE";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public M3CompiledSearch compile(byte[] needle) {
        if (needle == null) throw new NullPointerException("needle");
        byte[] snapshot = Arrays.copyOf(needle, needle.length);
        if (snapshot.length == 0) throw new IllegalArgumentException("needle must not be empty");
        return preferKmp(snapshot)
                ? M3KmpSearch.fromOwnedSnapshot(snapshot)
                : M3BoyerMooreHorspoolSearch.fromOwnedSnapshot(snapshot);
    }

    private static boolean preferKmp(byte[] needle) {
        if (needle.length < 4) return true;
        int threshold = majorityThreshold(needle.length);
        int[] frequency = new int[256];
        for (byte value : needle) {
            // Once a single byte reaches half the needle, later bytes cannot
            // reverse the KMP decision. Avoid scanning the remaining suffix.
            if (++frequency[Byte.toUnsignedInt(value)] >= threshold) return true;
        }
        return false;
    }

    /** Ceiling of half the needle length, without overflowing for Integer.MAX_VALUE. */
    private static int majorityThreshold(int length) {
        return (length >>> 1) + (length & 1);
    }
}
