// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/KmpBufferCountOnlyTest.java (blob 72a99367f61f) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Exact ByteBuffer overlapping-count contract with no count-path position consumer. */
final class KmpBufferCountOnlyTest {
    @Test
    void countMatchesIndependentOracleAndVisitorAcrossAllBufferKinds() {
        Random random = new Random(20261107L);
        for (int sample = 0; sample < 2_000; sample++) {
            byte[] haystack = new byte[random.nextInt(128)];
            byte[] needle = new byte[1 + random.nextInt(12)];
            for (int i = 0; i < haystack.length; i++) haystack[i] = (byte) random.nextInt(sample % 4 == 0 ? 2 : 256);
            for (int i = 0; i < needle.length; i++) needle[i] = (byte) random.nextInt(sample % 4 == 0 ? 2 : 256);
            if (sample % 5 == 0 && haystack.length >= needle.length) {
                System.arraycopy(needle, 0, haystack,
                        random.nextInt(haystack.length - needle.length + 1), needle.length);
            }
            KmpSearch compiled = new KmpSearch(needle);
            for (boolean direct : new boolean[] {false, true}) {
                ByteBuffer backing = direct
                        ? ByteBuffer.allocateDirect(haystack.length + 7)
                        : ByteBuffer.allocate(haystack.length + 7);
                for (int j = 0; j < haystack.length; j++) backing.put(j + 3, haystack[j]);
                backing.position(3);
                backing.limit(3 + haystack.length);
                for (ByteBuffer view : new ByteBuffer[] {
                        backing, backing.asReadOnlyBuffer(), backing.slice()}) {
                    view.mark();
                    int position = view.position();
                    int limit = view.limit();
                    for (int from : new int[] {Integer.MIN_VALUE, -1, 0, 1,
                            haystack.length / 2, haystack.length, Integer.MAX_VALUE}) {
                        int expected = naiveCount(haystack, needle, from);
                        List<Integer> positions = new ArrayList<>();
                        int visits = ByteOccurrences.visitBuffer(
                                compiled, view, from, Integer.MAX_VALUE, positions::add, null);
                        int counted = ByteOccurrences.countBuffer(compiled, view, from, null);
                        assertEquals(expected, visits, "visit sample=" + sample);
                        assertEquals(expected, counted, "count sample=" + sample);
                        assertEquals(positions.size(), counted);
                        assertEquals(position, view.position());
                        assertEquals(limit, view.limit());
                        view.reset();
                        assertEquals(position, view.position(), "mark should survive counting");
                    }
                }
            }
        }
    }

    @Test
    void preservesMonitorPollCountAndOriginalCancellationExceptionIdentity() {
        byte[] data = new byte[8_193];
        java.util.Arrays.fill(data, (byte) 7);
        KmpSearch compiled = new KmpSearch(new byte[] {7, 7, 7});
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length + 5);
        for (int i = 0; i < data.length; i++) direct.put(i + 2, data[i]);
        direct.position(2);
        direct.limit(2 + data.length);
        AtomicInteger countPolls = new AtomicInteger();
        AtomicInteger visitPolls = new AtomicInteger();
        int counted = ByteOccurrences.countBuffer(compiled, direct, 0,
                monitor(countPolls, -1, null));
        int visited = ByteOccurrences.visitBuffer(compiled, direct, 0, Integer.MAX_VALUE,
                ignored -> {}, monitor(visitPolls, -1, null));
        assertEquals(8_191, counted);
        assertEquals(counted, visited);
        assertEquals(visitPolls.get(), countPolls.get());

        for (int abortPoll : new int[] {1, 2, 3, 100, 4_000}) {
            CancellationException marker = new CancellationException("native-independent cancellation");
            AtomicInteger polling = new AtomicInteger();
            assertSame(marker, assertThrows(CancellationException.class,
                    () -> ByteOccurrences.countBuffer(compiled, direct, 0,
                            monitor(polling, abortPoll, marker))));
            assertEquals(abortPoll, polling.get());
        }
    }

    @Test
    void maintainsNullAndNonKmpProviderDispatch() {
        assertThrows(NullPointerException.class,
                () -> ByteOccurrences.countBuffer(null, ByteBuffer.allocate(0), 0, null));
        KmpSearch compiled = new KmpSearch(new byte[] {1});
        assertThrows(NullPointerException.class,
                () -> ByteOccurrences.countBuffer(compiled, null, 0, null));
        CompiledSearch other = new CompiledSearch() {
            @Override public int indexOf(byte[] input, int from) { return -1; }
            @Override public int indexOf(ByteBuffer input, int from) {
                for (int i = Math.max(0, from); i < input.remaining(); i++) {
                    if (input.get(input.position() + i) == 1) return i;
                }
                return -1;
            }
            @Override public int needleLength() { return 1; }
            @Override public String strategy() { return "external-provider"; }
        };
        ByteBuffer data = ByteBuffer.wrap(new byte[] {1, 2, 1, 1, 0});
        assertEquals(3, ByteOccurrences.countBuffer(other, data, 0, null));
        assertEquals(2, ByteOccurrences.countBuffer(other, data, 2, null));
        assertTrue(data.position() == 0 && data.limit() == 5);
    }

    private static int naiveCount(byte[] input, byte[] needle, int from) {
        int count = 0;
        for (int i = Math.max(0, from); i <= input.length - needle.length; i++) {
            boolean matched = true;
            for (int k = 0; k < needle.length; k++) {
                if (input[i + k] != needle[k]) { matched = false; break; }
            }
            if (matched) count++;
        }
        return count;
    }

    private static M3ProgressMonitor monitor(AtomicInteger count, int stopAt,
            CancellationException failure) {
        return new M3ProgressMonitor() {
            @Override public void checkCanceled() {
                if (count.incrementAndGet() == stopAt) throw failure;
            }
            @Override public void beginTask(String task, long total) {}
            @Override public void done() {}
            @Override public void worked(long work) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return false; }
            @Override public void setCanceled(boolean value) {}
            @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
        };
    }
}
