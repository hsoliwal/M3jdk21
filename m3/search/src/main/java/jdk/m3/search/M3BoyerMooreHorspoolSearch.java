// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/BoyerMooreHorspoolSearch.java (blob 41b8b0627378) by M3_SEARCH_RELOCATE v1.
// Modified 2026: lazily reuse the KMP overlap owner with safe publication.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;

/** Primitive Boyer-Moore-Horspool exact byte search with one 256-entry shift table. */
final class M3BoyerMooreHorspoolSearch implements M3CompiledSearch {
    private final byte[] needle;
    private final int[] shifts = new int[256];
    private final int[] reverseShifts = new int[256];
    private volatile M3KmpSearch overlapSearch;

    M3BoyerMooreHorspoolSearch(byte[] needle) {
        this(needle, false);
    }

    /** Takes the adaptive compiler's private snapshot without allocating a second array. */
    static M3BoyerMooreHorspoolSearch fromOwnedSnapshot(byte[] snapshot) {
        return new M3BoyerMooreHorspoolSearch(snapshot, true);
    }

    private M3BoyerMooreHorspoolSearch(byte[] needle, boolean owned) {
        if (needle.length == 0) throw new IllegalArgumentException("needle must not be empty");
        this.needle = owned ? needle : Arrays.copyOf(needle, needle.length);
        Arrays.fill(shifts, this.needle.length);
        Arrays.fill(reverseShifts, this.needle.length);
        for (int index = 0; index < this.needle.length - 1; index++) {
            shifts[Byte.toUnsignedInt(this.needle[index])] = this.needle.length - 1 - index;
        }
        for (int index = this.needle.length - 1; index > 0; index--) {
            reverseShifts[Byte.toUnsignedInt(this.needle[index])] = index;
        }
    }

    @Override
    public int indexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int start = Math.max(0, fromIndex);
        if (start > haystack.length - needle.length) return -1;

        // JNI search_forward already bounds adversarial Horspool work this way.
        // Count actual byte comparisons; keep fast sparse/hit paths allocation-free.
        long budget = 4L * (haystack.length - start);
        long compared = 0L;
        int cursor = start + needle.length - 1;
        while (cursor < haystack.length) {
            int pattern = needle.length - 1;
            int candidate = cursor;
            while (pattern >= 0 && haystack[candidate] == needle[pattern]) {
                candidate--;
                pattern--;
            }
            if (pattern < 0) return candidate + 1;
            compared += needle.length - pattern;
            cursor = boundedNextCursor(
                    cursor, shifts[Byte.toUnsignedInt(haystack[cursor])], haystack.length);
            if (compared > budget && cursor < haystack.length) {
                // BMH cursor denotes the last pattern byte, not the start offset.
                return overlapSearch(new M3DefaultProgressMonitor()).indexOf(
                        haystack, cursor - needle.length + 1);
            }
        }
        return -1;
    }

    /**
     * Saturate an admitted positive shift at the exclusive end instead of overflowing int.
     * Preconditions: 0 <= cursor < length <= Integer.MAX_VALUE, shift > 0.
     * This is an index transition, not a second search algorithm or a new public API.
     */
    static int boundedNextCursor(int cursor, int shift, int length) {
        return shift >= length - cursor ? length : cursor + shift;
    }

    @Override
    public int lastIndexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int maximum = haystack.length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int candidate = Math.min(fromIndex, maximum);
        long budget = 4L * ((long) candidate + needle.length);
        long compared = 0L;
        while (candidate >= 0) {
            int pattern = 0;
            int text = candidate;
            while (pattern < needle.length && haystack[text] == needle[pattern]) {
                text++;
                pattern++;
            }
            if (pattern == needle.length) return candidate;
            compared += (long) pattern + 1L;
            candidate -= reverseShifts[Byte.toUnsignedInt(haystack[candidate])];
            if (compared > budget && candidate >= 0) {
                // All later starts were eliminated by reverse Horspool's shift.
                return overlapSearch(new M3DefaultProgressMonitor()).lastIndexOf(haystack, candidate);
            }
        }
        return -1;
    }

    @Override
    public int indexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        /* Absolute indexed reads preserve the caller's position, limit and mark. */
        int base = haystack.position();
        int length = haystack.remaining();
        int start = Math.max(0, fromIndex);
        if (start > length - needle.length) return -1;
        long budget = 4L * (length - start);
        long compared = 0L;
        int cursor = start + needle.length - 1;
        while (cursor < length) {
            int pattern = needle.length - 1;
            int candidate = cursor;
            while (pattern >= 0 && haystack.get(base + candidate) == needle[pattern]) {
                candidate--;
                pattern--;
            }
            if (pattern < 0) return candidate + 1;
            compared += needle.length - pattern;
            cursor = boundedNextCursor(
                    cursor, shifts[Byte.toUnsignedInt(haystack.get(base + cursor))], length);
            if (compared > budget && cursor < length) {
                return overlapSearch(new M3DefaultProgressMonitor()).indexOf(
                        haystack, cursor - needle.length + 1);
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        /* Absolute indexed reads preserve the caller's position, limit and mark. */
        int base = haystack.position();
        int length = haystack.remaining();
        int maximum = length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int candidate = Math.min(fromIndex, maximum);
        long budget = 4L * ((long) candidate + needle.length);
        long compared = 0L;
        while (candidate >= 0) {
            int pattern = 0;
            int text = candidate;
            while (pattern < needle.length
                    && haystack.get(base + text) == needle[pattern]) {
                text++;
                pattern++;
            }
            if (pattern == needle.length) return candidate;
            compared += (long) pattern + 1L;
            candidate -= reverseShifts[Byte.toUnsignedInt(haystack.get(base + candidate))];
            if (compared > budget && candidate >= 0) {
                return overlapSearch(new M3DefaultProgressMonitor()).lastIndexOf(haystack, candidate);
            }
        }
        return -1;
    }

    /** Reuse the canonical KMP continuation without changing single-search strategy. */
    int visitMatches(byte[] haystack, int start, int maxMatches, IntConsumer receiver,
            M3ProgressMonitor monitor) {
        return scanMatches(haystack, start, maxMatches, receiver, false, monitor);
    }

    /** Count-only path reuses the same Horspool and bounded KMP overlap decisions. */
    int countMatches(byte[] haystack, int start, M3ProgressMonitor monitor) {
        return scanMatches(haystack, start, Integer.MAX_VALUE, null, true, monitor);
    }

    private int scanMatches(byte[] haystack, int start, int maxMatches,
            IntConsumer receiver, boolean countOnly, M3ProgressMonitor monitor) {
        int count = 0;
        int next = start;
        int densityStart = start;
        int lastStart = haystack.length - needle.length;
        long matchedWork = 0L;
        while (next <= lastStart && count < maxMatches) {
            int found = indexOf(haystack, next, monitor);
            if (found < 0) break;
            if (!countOnly) receiver.accept(found);
            count++;
            monitor.checkCanceled();
            next = found + 1;
            if (count == maxMatches || next > lastStart) break;
            matchedWork += needle.length;
            long covered = (long) found - densityStart + needle.length;
            // Keep sparse, absent and bounded searches on Horspool. Reconsider per call.
            if (matchedWork > 2L * covered) {
                int end = (int) Math.min((long) haystack.length,
                        (long) next + Math.max(4096L, 4L * needle.length));
                M3KmpSearch kmp = overlapSearch(monitor);
                count += countOnly
                        ? kmp.countMatches(haystack, next, end, maxMatches - count, monitor)
                        : kmp.visitMatches(haystack, next, end, maxMatches - count, receiver, monitor);
                if (count == maxMatches || end == haystack.length) break;
                // Only starts after end - length could still cross the window seam.
                next = end - needle.length + 1;
                densityStart = next;
                matchedWork = 0L;
            }
        }
        monitor.checkCanceled();
        return count;
    }

    /**
     * Direct ByteBuffer match visitor: sparse matches use the existing BMH
     * shift tables; dense overlapping hits switch once to the existing lazy
     * KMP prefix automaton. One forward KMP pass avoids repeated searches
     * and does not materialize the selected buffer window.
     */
    int visitMatches(ByteBuffer haystack, int start, int maxMatches, IntConsumer receiver,
            M3ProgressMonitor monitor) {
        int count = 0;
        int next = start;
        int densityStart = start;
        int lastStart = haystack.remaining() - needle.length;
        long matchedWork = 0L;
        while (next <= lastStart && count < maxMatches) {
            int found = indexOf(haystack, next, monitor);
            if (found < 0) break;
            receiver.accept(found);
            count++;
            monitor.checkCanceled();
            next = found + 1;
            if (count == maxMatches || next > lastStart) break;

            matchedWork += needle.length;
            long covered = (long) found - densityStart + needle.length;
            // Exactly the original byte[] density heuristic; no new selector.
            if (matchedWork > 2L * covered) {
                count += overlapSearch(monitor).visitMatches(
                        haystack, next, maxMatches - count, receiver, monitor);
                break;
            }
        }
        monitor.checkCanceled();
        return count;
    }

    private M3KmpSearch overlapSearch(M3ProgressMonitor monitor) {
        M3KmpSearch prepared = overlapSearch;
        if (prepared == null) {
            // Monitor callbacks can reenter; never invoke them under the publication lock.
            M3KmpSearch candidate = M3KmpSearch.fromOwnedSnapshot(needle, monitor);
            synchronized (this) {
                prepared = overlapSearch;
                if (prepared == null) {
                    prepared = candidate;
                    overlapSearch = prepared;
                }
            }
        }
        return prepared;
    }

    @Override
    public int needleLength() {
        return needle.length;
    }

    @Override
    public String strategy() {
        return "BOYER_MOORE_HORSPOOL";
    }
}
