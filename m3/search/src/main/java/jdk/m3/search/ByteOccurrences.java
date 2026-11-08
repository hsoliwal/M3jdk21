// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/ByteOccurrences.java (blob 621ae35905d0) by M3_SEARCH_FORWARD v1.
// Modified 2026: continue periodic Horspool overlaps through the existing KMP owner.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.IntConsumer;

/** Overlapping byte matches over a borrowed compiled pattern, without an output array. */
public final class ByteOccurrences {
    private static final IntConsumer DISCARD = position -> {};

    private ByteOccurrences() {}

    /**
     * Visits at most {@code maxMatches} starts in ascending byte order, including overlaps.
     * The caller retains and closes the compiled pattern; the haystack must not be mutated
     * during traversal. The returned count is the number of positions actually delivered.
     */
    public static int visit(CompiledSearch compiled, byte[] haystack, int fromIndex,
            int maxMatches, IntConsumer receiver, M3ProgressMonitor monitor) {
        Objects.requireNonNull(compiled, "compiled");
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(receiver, "receiver");
        if (maxMatches < 0) throw new IllegalArgumentException("maxMatches must be nonnegative");
        int length = compiled.needleLength();
        if (length <= 0) throw new IllegalArgumentException("compiled needle must be nonempty");
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        if (maxMatches == 0) return 0;
        int start = Math.max(0, fromIndex);
        if (compiled instanceof KmpSearch kmp) {
            return kmp.visitMatches(haystack, start, maxMatches, receiver, checked);
        }
        if (compiled instanceof BoyerMooreHorspoolSearch horspool) {
            return horspool.visitMatches(haystack, start, maxMatches, receiver, checked);
        }
        int[] nativePositions = JniSearchEngine.positionsIfNative(
                compiled, haystack, start, maxMatches, checked);
        if (nativePositions != null) {
            for (int position : nativePositions) {
                receiver.accept(position);
                checked.checkCanceled();
            }
            return nativePositions.length;
        }
        int count = 0;
        int lastStart = haystack.length - length;
        while (start <= lastStart && count < maxMatches) {
            int found = compiled.indexOf(haystack, start, checked);
            if (found < 0) break;
            if (found < start || found > lastStart) {
                throw new IllegalStateException("compiled search returned an invalid byte offset");
            }
            receiver.accept(found);
            count++;
            checked.checkCanceled();
            start = found + 1;
        }
        checked.checkCanceled();
        return count;
    }

    /**
     * Streams overlapping starts in the remaining ByteBuffer window, relative
     * to its position at entry. The caller's position, limit and mark remain
     * unchanged. Results use the existing compiled Java/JNI search provider.
     *
     * <p>The pattern remains borrowed; the caller must not mutate the buffer
     * while traversal is active. The generic provider path reuses direct-buffer
     * JNI search when present, without copying the entire haystack. A separate
     * method name avoids making legacy null-literal byte[] calls ambiguous.
     */
    public static int visitBuffer(CompiledSearch compiled, ByteBuffer haystack, int fromIndex,
            int maxMatches, IntConsumer receiver, M3ProgressMonitor monitor) {
        Objects.requireNonNull(compiled, "compiled");
        Objects.requireNonNull(haystack, "haystack");
        Objects.requireNonNull(receiver, "receiver");
        if (maxMatches < 0) throw new IllegalArgumentException("maxMatches must be nonnegative");
        int needleLength = compiled.needleLength();
        if (needleLength <= 0) throw new IllegalArgumentException("compiled needle must be nonempty");
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        if (maxMatches == 0) return 0;

        int start = Math.max(0, fromIndex);
        ByteBuffer view = haystack.duplicate();
        // KMP's existing prefix table scans once and retains overlapping hits;
        // absolute indexed reads preserve the original buffer's cursor and mark.
        if (compiled instanceof KmpSearch kmp) {
            return kmp.visitMatches(view, start, maxMatches, receiver, checked);
        }
        // Keep dense ByteBuffer overlaps on the same lazy KMP crossover
        // already used by the original byte[] Boyer-Moore-Horspool visitor.
        if (compiled instanceof BoyerMooreHorspoolSearch horspool) {
            return horspool.visitMatches(view, start, maxMatches, receiver, checked);
        }

        int count = 0;
        int lastStart = view.remaining() - needleLength;
        while (start <= lastStart && count < maxMatches) {
            // A per-call view protects the fixed window against third-party
            // M3CompiledSearch implementations that advance their input cursor.
            int found = compiled.indexOf(view.duplicate(), start, checked);
            if (found < 0) break;
            if (found < start || found > lastStart) {
                throw new IllegalStateException("compiled search returned an invalid byte offset");
            }
            receiver.accept(found);
            count++;
            checked.checkCanceled();
            start = found + 1;
        }
        checked.checkCanceled();
        return count;
    }

    /** Counts overlapping matches in the remaining ByteBuffer window, without retaining offsets. */
    public static int countBuffer(CompiledSearch compiled, ByteBuffer haystack, int fromIndex,
            M3ProgressMonitor monitor) {
        if (compiled instanceof KmpSearch kmp) {
            Objects.requireNonNull(haystack, "haystack");
            M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
            checked.checkCanceled();
            return kmp.countMatches(haystack.duplicate(), Math.max(0, fromIndex), checked);
        }
        if (compiled instanceof BoyerMooreHorspoolSearch horspool) {
            Objects.requireNonNull(haystack, "haystack");
            M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
            checked.checkCanceled();
            return horspool.countMatches(haystack.duplicate(), Math.max(0, fromIndex), checked);
        }
        return visitBuffer(compiled, haystack, fromIndex, Integer.MAX_VALUE, DISCARD, monitor);
    }

    /** Counts all overlapping occurrences without retaining positions. */
    public static int count(CompiledSearch compiled, byte[] haystack, int fromIndex,
            M3ProgressMonitor monitor) {
        Objects.requireNonNull(compiled, "compiled");
        Objects.requireNonNull(haystack, "haystack");
        int needleLength = compiled.needleLength();
        if (needleLength <= 0) {
            throw new IllegalArgumentException("compiled needle must be nonempty");
        }
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int nativeCount = JniSearchEngine.countIfNative(compiled, haystack, fromIndex, checked);
        if (nativeCount < -1) {
            throw new IllegalStateException("native overlap count returned an invalid value");
        }
        if (nativeCount >= 0) {
            int start = Math.max(0, fromIndex);
            int lastStart = haystack.length - needleLength;
            // Avoid overflow when both the pattern and fromIndex exceed the input.
            int possible = lastStart < start ? 0 : lastStart - start + 1;
            if (nativeCount > possible) {
                throw new IllegalStateException("native overlap count returned an invalid value");
            }
            return nativeCount;
        }
        if (compiled instanceof KmpSearch kmp) {
            // Preserve the original count -> visit entry cancellation checkpoint.
            checked.checkCanceled();
            // Count-only uses the compiled automaton without a per-hit consumer.
            return kmp.countMatches(haystack, Math.max(0, fromIndex), checked);
        }
        if (compiled instanceof BoyerMooreHorspoolSearch horspool) {
            // Retain the count -> visit entry check and the original dense KMP seam.
            checked.checkCanceled();
            return horspool.countMatches(haystack, Math.max(0, fromIndex), checked);
        }
        return visit(compiled, haystack, fromIndex, Integer.MAX_VALUE, DISCARD, checked);
    }
}
