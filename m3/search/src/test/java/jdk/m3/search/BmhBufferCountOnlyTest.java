// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BmhBufferCountOnlyTest.java (blob 61dac0330d9f) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import org.junit.jupiter.api.Test;

/** Exact ByteBuffer BMH overlapping count, selected dense-KMP seam and monitor parity. */
final class BmhBufferCountOnlyTest {
    private static final int[] STARTS = {
        Integer.MIN_VALUE, -7, -1, 0, 1, 3, 25, Integer.MAX_VALUE
    };

    @Test
    void countAndVisitorMatchIndependentOracleAcrossBufferRepresentations() {
        Random random = new Random(0x4d33424d48425546L);
        for (int sample = 0; sample < 1_800; sample++) {
            int len = random.nextInt(117);
            byte[] data = new byte[len];
            byte[] pattern = new byte[1 + random.nextInt(12)];
            int alphabet = sample % 4 == 0 ? 2 : sample % 4 == 1 ? 4 : 256;
            for (int i = 0; i < data.length; i++) data[i] = (byte) random.nextInt(alphabet);
            for (int i = 0; i < pattern.length; i++) pattern[i] = (byte) random.nextInt(alphabet);
            if (sample % 7 == 0 && data.length >= pattern.length) {
                System.arraycopy(pattern, 0, data,
                        random.nextInt(data.length - pattern.length + 1), pattern.length);
            }
            BoyerMooreHorspoolSearch compiled = new BoyerMooreHorspoolSearch(pattern);
            for (boolean direct : new boolean[] {false, true}) {
                ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(len + 9)
                        : ByteBuffer.allocate(len + 9);
                for (int i = 0; i < len; i++) buffer.put(i + 4, data[i]);
                buffer.position(4);
                buffer.limit(4 + len);
                for (ByteBuffer view : new ByteBuffer[] {
                        buffer, buffer.asReadOnlyBuffer(), buffer.slice()}) {
                    view.mark();
                    int position = view.position();
                    int limit = view.limit();
                    for (int from : STARTS) {
                        int expected = independentCount(data, pattern, from);
                        List<Integer> positions = new ArrayList<>();
                        int visits = ByteOccurrences.visitBuffer(
                                compiled, view, from, Integer.MAX_VALUE, positions::add, null);
                        int actual = ByteOccurrences.countBuffer(compiled, view, from, null);
                        assertEquals(expected, visits, "visit sample=" + sample + " from=" + from);
                        assertEquals(expected, actual, "count sample=" + sample + " from=" + from);
                        assertEquals(visits, positions.size());
                        assertEquals(position, view.position());
                        assertEquals(limit, view.limit());
                        view.reset();
                        assertEquals(position, view.position(), "buffer mark preserved");
                    }
                }
            }
        }
    }

    @Test
    void denseKmpCrossoverPreservesCheckpointsAndCancellationIdentity() {
        byte[] data = new byte[8_193];
        java.util.Arrays.fill(data, (byte) 7);
        byte[] needle = {7, 7, 7, 7, 7, 7, 7};
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + 8);
        for (int i = 0; i < data.length; i++) direct.put(i + 3, data[i]);
        direct.position(3);
        direct.limit(3 + data.length);
        AtomicInteger visits = new AtomicInteger();
        AtomicInteger counts = new AtomicInteger();
        BoyerMooreHorspoolSearch v = new BoyerMooreHorspoolSearch(needle);
        BoyerMooreHorspoolSearch c = new BoyerMooreHorspoolSearch(needle);
        int visited = ByteOccurrences.visitBuffer(v, direct, 0, Integer.MAX_VALUE,
                ignored -> {}, monitor(visits, -1, null));
        int counted = ByteOccurrences.countBuffer(c, direct, 0,
                monitor(counts, -1, null));
        assertEquals(8_187, visited);
        assertEquals(visited, counted);
        assertEquals(visits.get(), counts.get());
        for (int abort : new int[] {1, 2, 3, 5, 25, 100, 4_000}) {
            CancellationException marker = new CancellationException("M3 canceled " + abort);
            AtomicInteger p1 = new AtomicInteger();
            AtomicInteger p2 = new AtomicInteger();
            BoyerMooreHorspoolSearch a = new BoyerMooreHorspoolSearch(needle);
            BoyerMooreHorspoolSearch b = new BoyerMooreHorspoolSearch(needle);
            assertSame(marker, assertThrows(CancellationException.class,
                    () -> ByteOccurrences.visitBuffer(a, direct, 0, Integer.MAX_VALUE,
                            ignored -> {}, monitor(p1, abort, marker))));
            assertSame(marker, assertThrows(CancellationException.class,
                    () -> ByteOccurrences.countBuffer(b, direct, 0,
                            monitor(p2, abort, marker))));
            assertEquals(abort, p1.get());
            assertEquals(abort, p2.get());
        }
        assertEquals(3, direct.position());
        assertEquals(data.length + 3, direct.limit());
    }

    @Test
    void nullAndForeignProviderRetainOriginalDispatchAndBounds() {
        BoyerMooreHorspoolSearch compiled = new BoyerMooreHorspoolSearch(new byte[] {1, 2});
        assertThrows(NullPointerException.class,
                () -> ByteOccurrences.countBuffer(compiled, null, 0, null));
        assertThrows(NullPointerException.class,
                () -> ByteOccurrences.countBuffer(null, ByteBuffer.wrap(new byte[] {1}), 0, null));
        CompiledSearch foreign = new CompiledSearch() {
            @Override public int indexOf(byte[] data, int from) {
                for (int i = Math.max(0, from); i < data.length; ++i) {
                    if (data[i] == 9) return i;
                }
                return -1;
            }
            @Override public int indexOf(ByteBuffer bytes, int from) {
                for (int i = Math.max(0, from); i < bytes.remaining(); ++i) {
                    if (bytes.get(bytes.position() + i) == 9) return i;
                }
                return -1;
            }
            @Override public int needleLength() { return 1; }
            @Override public String strategy() { return "foreign"; }
        };
        assertEquals(3, ByteOccurrences.countBuffer(foreign,
                ByteBuffer.wrap(new byte[] {9, 9, 2, 9}), -5, null));
        assertEquals(0, ByteOccurrences.countBuffer(foreign,
                ByteBuffer.wrap(new byte[] {9, 9}), Integer.MAX_VALUE, null));
    }

    private static int independentCount(byte[] bytes, byte[] needle, int start) {
        int hits = 0;
        for (int i = Math.max(0, start); i <= bytes.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) { found = false; break; }
            }
            if (found) hits++;
        }
        return hits;
    }

    private static M3ProgressMonitor monitor(AtomicInteger polls, int abortAt,
            CancellationException failure) {
        return new M3ProgressMonitor() {
            @Override public void checkCanceled() {
                if (polls.incrementAndGet() == abortAt) throw failure;
            }
            @Override public void beginTask(String task, long total) {}
            @Override public void done() {}
            @Override public void worked(long count) {}
            @Override public void setProgress(long current, long total) {}
            @Override public void setTaskName(String task) {}
            @Override public void subTask(String task) {}
            @Override public boolean isCanceled() { return false; }
            @Override public void setCanceled(boolean canceled) {}
            @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
        };
    }
}
