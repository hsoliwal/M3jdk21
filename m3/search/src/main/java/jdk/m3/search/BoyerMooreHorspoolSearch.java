// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/BoyerMooreHorspoolSearch.java (blob 8141756a8c2a) by M3_SEARCH_FORWARD v1.
// Modified 2026: lazily reuse the KMP overlap owner with safe publication.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;

/** Primitive Boyer-Moore-Horspool exact byte search with one 256-entry shift table. */
final class BoyerMooreHorspoolSearch implements CompiledSearch {
    private final byte[] needle;
    private final int[] shifts = new int[256];
    private final int[] reverseShifts = new int[256];
    private volatile KmpSearch overlapSearch;

    BoyerMooreHorspoolSearch(byte[] needle) {
        this(needle, false);
    }

    /** Takes the adaptive compiler's private snapshot without allocating a second array. */
    static BoyerMooreHorspoolSearch fromOwnedSnapshot(byte[] snapshot) {
        return new BoyerMooreHorspoolSearch(snapshot, true);
    }

    private BoyerMooreHorspoolSearch(byte[] needle, boolean owned) {
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
        // Existing unmonitored public search retains its exact algorithm and API.
        return indexOfBuffer(haystack, fromIndex, null);
    }

    /** Share one BMH scan, with cancellation checks only for monitored callers. */
    private int indexOfBuffer(ByteBuffer haystack, int fromIndex, M3ProgressMonitor monitor) {
        if (haystack == null) throw new NullPointerException("haystack");
        /* Absolute indexed reads preserve position, limit and mark. */
        int base = haystack.position();
        int length = haystack.remaining();
        int start = Math.max(0, fromIndex);
        if (start > length - needle.length) return -1;
        long budget = 4L * (length - start);
        long compared = 0L;
        int cursor = start + needle.length - 1;
        int work = 0;
        while (cursor < length) {
            if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
            int pattern = needle.length - 1;
            int candidate = cursor;
            while (pattern >= 0 && haystack.get(base + candidate) == needle[pattern]) {
                if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
                candidate--;
                pattern--;
            }
            if (pattern < 0) return candidate + 1;
            compared += needle.length - pattern;
            cursor = boundedNextCursor(
                    cursor, shifts[Byte.toUnsignedInt(haystack.get(base + cursor))], length);
            if (compared > budget && cursor < length) {
                return monitoredKmpFirst(haystack, cursor - needle.length + 1, monitor);
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        // The existing unmonitored reverse API retains its exact BMH behavior.
        return lastIndexOfBuffer(haystack, fromIndex, null);
    }

    @Override
    public int lastIndexOf(ByteBuffer haystack, int fromIndex, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int result = lastIndexOfBuffer(haystack, fromIndex, checked);
        checked.checkCanceled();
        return result;
    }

    /**
     * Original reverse BMH shifts and comparisons, with an optional bounded
     * work budget for monitored callers only. Polls within near-full matches
     * as well as the candidate loop; absolute indexed reads never move the
     * caller's position, limit or mark.
     */
    private int lastIndexOfBuffer(ByteBuffer haystack, int fromIndex,
            M3ProgressMonitor monitor) {
        if (haystack == null) throw new NullPointerException("haystack");
        int base = haystack.position();
        int length = haystack.remaining();
        int maximum = length - needle.length;
        if (fromIndex < 0 || maximum < 0) return -1;
        int candidate = Math.min(fromIndex, maximum);
        long budget = 4L * ((long) candidate + needle.length);
        long compared = 0L;
        int work = 0;
        while (candidate >= 0) {
            if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
            int pattern = 0;
            int text = candidate;
            while (pattern < needle.length
                    && haystack.get(base + text) == needle[pattern]) {
                if (monitor != null && (++work & 4095) == 0) monitor.checkCanceled();
                text++;
                pattern++;
            }
            if (pattern == needle.length) return candidate;
            compared += (long) pattern + 1L;
            candidate -= reverseShifts[Byte.toUnsignedInt(haystack.get(base + candidate))];
            if (compared > budget && candidate >= 0) {
                return monitoredKmpLast(haystack, candidate, monitor);
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
                KmpSearch kmp = overlapSearch(monitor);
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
     * Preserve the original byte-buffer visitor and dense KMP continuation.
     * The count-only entry point reuses its exact search/cancellation checkpoints
     * without a per-hit discard consumer or a materialized positions array.
     */
    int visitMatches(ByteBuffer haystack, int start, int maxMatches, IntConsumer receiver,
            M3ProgressMonitor monitor) {
        return scanBufferMatches(haystack, start, maxMatches, receiver, false, monitor);
    }

    /** One count-only ByteBuffer scan using the incumbent BMH/KMP selector. */
    int countMatches(ByteBuffer haystack, int start, M3ProgressMonitor monitor) {
        return scanBufferMatches(haystack, start, Integer.MAX_VALUE, null, true, monitor);
    }

    /** Same match ordering and cancellation checkpoints in visit and count modes. */
    private int scanBufferMatches(ByteBuffer haystack, int start, int maxMatches,
            IntConsumer receiver, boolean countOnly, M3ProgressMonitor monitor) {
        int count = 0;
        int next = start;
        int densityStart = start;
        int lastStart = haystack.remaining() - needle.length;
        long matchedWork = 0L;
        while (next <= lastStart && count < maxMatches) {
            int found = indexOfBuffer(haystack, next, monitor);
            if (found < 0) break;
            if (!countOnly) receiver.accept(found);
            count++;
            monitor.checkCanceled();
            next = found + 1;
            if (count == maxMatches || next > lastStart) break;

            matchedWork += needle.length;
            long covered = (long) found - densityStart + needle.length;
            // Exactly the original byte[] density heuristic; no new selector.
            if (matchedWork > 2L * covered) {
                KmpSearch kmp = overlapSearch(monitor);
                count += countOnly
                        ? kmp.countMatches(haystack, next, monitor)
                        : kmp.visitMatches(haystack, next, maxMatches - count, receiver, monitor);
                break;
            }
        }
        monitor.checkCanceled();
        return count;
    }

    /**
     * Preserve the direct KMP path for unmonitored calls. With a real monitor,
     * continue through the existing bounded-polled KMP visitor rather than
     * silently losing cancellation at the BMH work-budget boundary.
     */
    private int monitoredKmpFirst(ByteBuffer haystack, int start,
            M3ProgressMonitor monitor) {
        if (monitor == null || monitor == new M3DefaultProgressMonitor()) {
            return overlapSearch(new M3DefaultProgressMonitor()).indexOf(haystack, start);
        }
        int[] result = {-1};
        overlapSearch(monitor).visitMatches(
                haystack, start, 1, position -> result[0] = position, monitor);
        return result[0];
    }

    /**
     * The last admitted start is inclusive. Restrict the KMP visitor's scan
     * to that final start plus the pattern width, preserving every earlier
     * candidate and excluding any later match. The supplied buffer remains
     * borrowed and unmodified (including its original mark).
     */
    private int monitoredKmpLast(ByteBuffer haystack, int lastStart,
            M3ProgressMonitor monitor) {
        if (monitor == null || monitor == new M3DefaultProgressMonitor()) {
            return overlapSearch(new M3DefaultProgressMonitor()).lastIndexOf(haystack, lastStart);
        }
        ByteBuffer bounded = haystack.duplicate();
        bounded.limit(bounded.position() + lastStart + needle.length);
        int[] result = {-1};
        overlapSearch(monitor).visitMatches(
                bounded, 0, Integer.MAX_VALUE, position -> result[0] = position, monitor);
        return result[0];
    }

    private KmpSearch overlapSearch(M3ProgressMonitor monitor) {
        KmpSearch prepared = overlapSearch;
        if (prepared == null) {
            // Monitor callbacks can reenter; never invoke them under the publication lock.
            KmpSearch candidate = KmpSearch.fromOwnedSnapshot(needle, monitor);
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
