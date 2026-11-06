// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/KmpSearch.java (blob acbec15bb187) by M3_SEARCH_RELOCATE v1.
// Modified 2026: share monitored prefix preparation and bound overlap fallback polling.
package jdk.m3.search;

import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;

/** Primitive Knuth-Morris-Pratt exact byte search with one immutable prefix table. */
final class M3KmpSearch implements M3CompiledSearch {
    private final byte[] needle;
    private final int[] prefix;

    M3KmpSearch(byte[] needle) {
        this(needle, false);
    }

    /** Takes the adaptive compiler's private snapshot without allocating a second array. */
    static M3KmpSearch fromOwnedSnapshot(byte[] snapshot) {
        return new M3KmpSearch(snapshot, true);
    }

    /** Prepare a private snapshot for lazy overlap traversal with bounded cancellation. */
    static M3KmpSearch fromOwnedSnapshot(byte[] snapshot, M3ProgressMonitor monitor) {
        return new M3KmpSearch(snapshot, true, monitor);
    }

    private M3KmpSearch(byte[] needle, boolean owned) {
        this(needle, owned, null);
    }

    private M3KmpSearch(byte[] needle, boolean owned, M3ProgressMonitor monitor) {
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        this.needle = owned ? needle : Arrays.copyOf(needle, needle.length);
        this.prefix = prefix(this.needle, monitor);
    }

    @Override
    public int indexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int start = Math.max(0, fromIndex);
        if (needle.length > haystack.length - Math.min(start, haystack.length)) return -1;
        int matched = 0;
        for (int index = start; index < haystack.length; index++) {
            while (matched > 0 && haystack[index] != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) return index - needle.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int maximum = haystack.length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int limit = Math.min(fromIndex, maximum);
        int scanEnd = limit + needle.length;
        int matched = 0;
        int last = -1;
        for (int index = 0; index < scanEnd; index++) {
            while (matched > 0 && haystack[index] != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) {
                last = index - needle.length + 1;
                matched = prefix[matched - 1];
            }
        }
        return last;
    }

    @Override
    public int indexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int start = Math.max(0, fromIndex);
        if (needle.length > length - Math.min(start, length)) return -1;
        int matched = 0;
        for (int index = start; index < length; index++) {
            byte value = haystack.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) return index - needle.length + 1;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int maximum = length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int limit = Math.min(fromIndex, maximum);
        int scanEnd = limit + needle.length;
        int matched = 0;
        int last = -1;
        for (int index = 0; index < scanEnd; index++) {
            byte value = haystack.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) {
                last = index - needle.length + 1;
                matched = prefix[matched - 1];
            }
        }
        return last;
    }

    @Override
    public int needleLength() {
        return needle.length;
    }

    @Override
    public String strategy() {
        return "KMP";
    }

    /** Continue the compiled prefix automaton after a hit, retaining overlapping matches. */
    int visitMatches(byte[] haystack, int start, int maxMatches, IntConsumer receiver,
            M3ProgressMonitor monitor) {
        return visitMatches(haystack, start, haystack.length, maxMatches, receiver, monitor);
    }

    /** Scan an exclusive byte window; a caller may reconsider its unfinished suffix. */
    int visitMatches(byte[] haystack, int start, int toIndex, int maxMatches,
            IntConsumer receiver, M3ProgressMonitor monitor) {
        return scanMatches(haystack, start, toIndex, maxMatches, receiver, false, monitor);
    }

    /**
     * Count overlapping matches without a per-hit callback or output positions.
     * Reuses the same compiled prefix table and cancellation checkpoints as visitMatches.
     * The caller has validated the input, pattern and initial monitor state.
     */
    int countMatches(byte[] haystack, int start, M3ProgressMonitor monitor) {
        return countMatches(haystack, start, haystack.length,
                Integer.MAX_VALUE, monitor);
    }

    /** Exact bounded native-independent overlap count for the Horspool dense seam. */
    int countMatches(byte[] haystack, int start, int toIndex, int maxMatches,
            M3ProgressMonitor monitor) {
        return scanMatches(haystack, start, toIndex, maxMatches, null, true, monitor);
    }

    /** Single canonical KMP scanner for visits and count-only queries. */
    private int scanMatches(byte[] haystack, int start, int toIndex, int maxMatches,
            IntConsumer receiver, boolean countOnly, M3ProgressMonitor monitor) {
        int count = 0;
        int matched = 0;
        int work = 0;
        for (int index = start; index < toIndex; index++) {
            if ((work++ & 4095) == 0) monitor.checkCanceled();
            while (matched > 0 && haystack[index] != needle[matched]) {
                if ((work++ & 4095) == 0) monitor.checkCanceled();
                matched = prefix[matched - 1];
            }
            if (haystack[index] == needle[matched]) matched++;
            if (matched == needle.length) {
                if (!countOnly) receiver.accept(index - needle.length + 1);
                count++;
                monitor.checkCanceled();
                if (count == maxMatches) return count;
                matched = prefix[matched - 1];
            }
        }
        monitor.checkCanceled();
        return count;
    }

    /**
     * One forward KMP pass over a fixed ByteBuffer window: O(remaining bytes
     * + needle length) without materializing a heap array. All reads are
     * absolute, so direct, mapped and read-only buffer state is preserved.
     */
    int visitMatches(ByteBuffer haystack, int start, int maxMatches, IntConsumer receiver,
            M3ProgressMonitor monitor) {
        int base = haystack.position();
        int length = haystack.remaining();
        int count = 0;
        int matched = 0;
        int work = 0;
        for (int index = start; index < length; index++) {
            if ((work++ & 4095) == 0) monitor.checkCanceled();
            byte value = haystack.get(base + index);
            while (matched > 0 && value != needle[matched]) {
                if ((work++ & 4095) == 0) monitor.checkCanceled();
                matched = prefix[matched - 1];
            }
            if (value == needle[matched]) matched++;
            if (matched == needle.length) {
                receiver.accept(index - needle.length + 1);
                count++;
                monitor.checkCanceled();
                if (count == maxMatches) return count;
                matched = prefix[matched - 1];
            }
        }
        monitor.checkCanceled();
        return count;
    }

    private static int[] prefix(byte[] pattern, M3ProgressMonitor monitor) {
        if (monitor != null) monitor.checkCanceled();
        int[] table = new int[pattern.length];
        int work = 0;
        for (int index = 1, matched = 0; index < pattern.length; index++) {
            if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
            while (matched > 0 && pattern[index] != pattern[matched]) {
                if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
                matched = table[matched - 1];
            }
            if (pattern[index] == pattern[matched]) matched++;
            table[index] = matched;
        }
        if (monitor != null) monitor.checkCanceled();
        return table;
    }
}
