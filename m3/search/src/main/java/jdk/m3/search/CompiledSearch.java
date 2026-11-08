// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/main/java/jdk/internal/m3/search/CompiledSearch.java (blob bc7395a6953a) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import java.nio.ByteBuffer;

/** Precomputed search pattern safe for repeated byte-array lookups. */
public interface CompiledSearch extends AutoCloseable {
    int indexOf(byte[] haystack, int fromIndex);

    /** Progress-aware compatibility entrypoint; native execution polls only at its boundaries. */
    default int indexOf(byte[] haystack, int fromIndex, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int result = indexOf(haystack, fromIndex);
        checked.checkCanceled();
        return result;
    }

    /**
     * Last exact byte occurrence whose start offset is at most {@code fromIndex}.
     *
     * <p>The default preserves compatibility for third-party implementations by repeatedly using
     * {@link #indexOf(byte[], int)}. Built-in compiled strategies override this with one-pass
     * precomputed search, and JNI may use an additive native symbol.</p>
     */
    default int lastIndexOf(byte[] haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        int maximum = haystack.length - needleLength();
        if (fromIndex < 0 || maximum < 0) return -1;
        int limit = Math.min(fromIndex, maximum);
        int found = -1;
        for (int next = 0; next <= limit; ) {
            int hit = indexOf(haystack, next);
            if (hit >= 0 && hit < next) {
                throw new IllegalStateException("compiled search returned an invalid byte offset");
            }
            if (hit < 0 || hit > limit) break;
            found = hit;
            next = hit + 1;
        }
        return found;
    }

    /** Progress-aware reverse lookup; native execution polls at its existing Java boundaries. */
    default int lastIndexOf(byte[] haystack, int fromIndex, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int result = lastIndexOf(haystack, fromIndex);
        checked.checkCanceled();
        return result;
    }

    /**
     * Exact search over the buffer's remaining range. Returned offsets are relative to position.
     *
     * <p>The compatibility fallback copies the logical range once. Built-in Java and JNI
     * implementations override this method and do not copy the haystack.</p>
     */
    default int indexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        ByteBuffer view = haystack.duplicate();
        byte[] copy = new byte[view.remaining()];
        view.get(copy);
        return indexOf(copy, fromIndex);
    }

    /** Reverse exact search over the buffer's remaining range, relative to position. */
    default int lastIndexOf(ByteBuffer haystack, int fromIndex) {
        if (haystack == null) throw new NullPointerException("haystack");
        ByteBuffer view = haystack.duplicate();
        byte[] copy = new byte[view.remaining()];
        view.get(copy);
        return lastIndexOf(copy, fromIndex);
    }

    default int indexOf(ByteBuffer haystack, int fromIndex, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int result = indexOf(haystack, fromIndex);
        checked.checkCanceled();
        return result;
    }

    default int lastIndexOf(ByteBuffer haystack, int fromIndex, M3ProgressMonitor monitor) {
        M3ProgressMonitor checked = monitor == null ? new M3DefaultProgressMonitor() : monitor;
        checked.checkCanceled();
        int result = lastIndexOf(haystack, fromIndex);
        checked.checkCanceled();
        return result;
    }

    int needleLength();
    String strategy();

    @Override
    default void close() {
        // Pure-Java implementations have no external lifecycle.
    }
}
