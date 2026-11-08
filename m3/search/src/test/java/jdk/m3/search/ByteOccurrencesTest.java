// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/ByteOccurrencesTest.java (blob 7a7e681fec53) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ByteOccurrencesTest {
    @Test
    void matchesIndependentOracleForOverlapsLimitsOffsetsAndAllJavaStrategies() {
        Random random = new Random(0x0cc0ffeeL);
        for (int sample = 0; sample < 10_000; sample++) {
            byte[] haystack = new byte[random.nextInt(80)];
            byte[] needle = new byte[1 + random.nextInt(12)];
            for (int i = 0; i < haystack.length; i++) haystack[i] = (byte) random.nextInt(4);
            for (int i = 0; i < needle.length; i++) needle[i] = (byte) random.nextInt(4);
            int from = random.nextInt(haystack.length + 12) - 6;
            List<Integer> expected = oracle(haystack, needle, from);
            CompiledSearch[] strategies = {
                    new KmpSearch(needle), new BoyerMooreHorspoolSearch(needle),
                    new JavaAdaptiveSearchEngine().compile(needle)};
            for (CompiledSearch compiled : strategies) {
                try (compiled) {
                    for (int limit : new int[] {0, 1, 3, Integer.MAX_VALUE}) {
                        List<Integer> actual = new ArrayList<>();
                        int delivered = ByteOccurrences.visit(compiled, haystack, from,
                                limit, actual::add, null);
                        List<Integer> prefix = expected.subList(0, Math.min(limit, expected.size()));
                        assertEquals(prefix, actual, "sample=" + sample + " " + compiled.strategy());
                        assertEquals(prefix.size(), delivered);
                    }
                    assertEquals(expected.size(), ByteOccurrences.count(compiled, haystack,
                            from, null));
                }
            }
        }
    }

    @Test
    void pollsCancellationInsideLongKmpScanAndPreservesBorrowedPattern() {
        byte[] haystack = new byte[20_000];
        try (CompiledSearch compiled = new KmpSearch(new byte[] {1})) {
            AtomicInteger polls = new AtomicInteger();
            ProgressMonitor cancelDuringScan = new ProgressMonitor() {
                @Override public void beginTask(String name, long total) {}
                @Override public void done() {}
                @Override public void worked(long work) {}
                @Override public void setProgress(long worked, long total) {}
                @Override public void setTaskName(String name) {}
                @Override public void subTask(String name) {}
                @Override public boolean isCanceled() { return polls.incrementAndGet() >= 3; }
                @Override public void setCanceled(boolean value) {}
                @Override public ProgressState state() {
                    return new DefaultProgressMonitor().state();
                }
            };
            assertThrows(CancellationException.class,
                    () -> ByteOccurrences.count(compiled, haystack, 0, cancelDuringScan));
            assertTrue(polls.get() >= 3);
            assertEquals(-1, compiled.indexOf(haystack, 0));
            assertThrows(IllegalArgumentException.class,
                    () -> ByteOccurrences.visit(compiled, haystack, 0, -1, ignored -> {}, null));
        }
    }

    @Test
    void repetitiveInputContinuesAfterOverlappingHits() {
        byte[] haystack = new byte[4096];
        byte[] needle = new byte[256];
        Arrays.fill(haystack, (byte) 'a');
        Arrays.fill(needle, (byte) 'a');
        try (CompiledSearch compiled = new JavaAdaptiveSearchEngine().compile(needle)) {
            assertEquals("KMP", compiled.strategy());
            assertEquals(4096 - 256 + 1, ByteOccurrences.count(compiled, haystack, 0, null));
            List<Integer> first = new ArrayList<>();
            assertEquals(3, ByteOccurrences.visit(compiled, haystack, 0, 3,
                    first::add, null));
            assertEquals(List.of(0, 1, 2), first);
        }
    }

    @Test
    void byteBufferStreamingPreservesWindowStateAndMatchesAllJavaStrategies() {
        Random random = new Random(0x6A254B19L);
        for (int sample = 0; sample < 3_000; sample++) {
            byte[] haystack = new byte[random.nextInt(96)];
            byte[] needle = new byte[1 + random.nextInt(18)];
            random.nextBytes(haystack);
            random.nextBytes(needle);
            if ((sample & 1) == 0) {
                for (int i = 0; i < haystack.length; i++)
                    haystack[i] = (byte) random.nextInt(3);
                for (int i = 0; i < needle.length; i++)
                    needle[i] = (byte) random.nextInt(3);
            }
            int from = random.nextInt(haystack.length + 18) - 9;
            List<Integer> expected = oracle(haystack, needle, from);
            ByteBuffer heap = ByteBuffer.allocate(haystack.length + 7);
            ByteBuffer direct = ByteBuffer.allocateDirect(haystack.length + 7);
            heap.position(3).put(haystack).limit(3 + haystack.length).position(3);
            direct.position(3).put(haystack).limit(3 + haystack.length).position(3);
            ByteBuffer[] buffers = {heap, heap.asReadOnlyBuffer(), direct,
                    direct.asReadOnlyBuffer()};
            CompiledSearch[] strategies = {
                    new KmpSearch(needle), new BoyerMooreHorspoolSearch(needle),
                    new JavaAdaptiveSearchEngine().compile(needle)};
            for (CompiledSearch compiled : strategies) {
                try (compiled) {
                    for (ByteBuffer buffer : buffers) {
                        int position = buffer.position();
                        int limit = buffer.limit();
                        buffer.mark();
                        for (int maximum : new int[]{0, 1, 3, Integer.MAX_VALUE}) {
                            List<Integer> observed = new ArrayList<>();
                            int visited = ByteOccurrences.visitBuffer(compiled, buffer, from,
                                    maximum, observed::add, null);
                            List<Integer> prefix = expected.subList(0,
                                    Math.min(maximum, expected.size()));
                            assertEquals(prefix, observed, "sample=" + sample);
                            assertEquals(prefix.size(), visited, "sample=" + sample);
                            assertEquals(position, buffer.position(), "buffer position");
                            assertEquals(limit, buffer.limit(), "buffer limit");
                            buffer.reset();
                            assertEquals(position, buffer.position(), "buffer mark");
                        }
                        assertEquals(expected.size(),
                                ByteOccurrences.countBuffer(compiled, buffer, from, null));
                        buffer.reset();
                        assertEquals(position, buffer.position(), "buffer count mark");
                    }
                }
            }
        }
    }

    @Test
    void byteBufferOverlapsRejectBadOffsetsAndPollKmpCancellation() {
        byte[] haystack = new byte[25_000];
        byte[] needle = new byte[256];
        Arrays.fill(haystack, (byte) 'a');
        Arrays.fill(needle, (byte) 'a');
        ByteBuffer direct = ByteBuffer.allocateDirect(haystack.length);
        direct.put(haystack).flip();
        try (CompiledSearch kmp = new KmpSearch(needle)) {
            assertEquals(haystack.length - needle.length + 1,
                    ByteOccurrences.countBuffer(kmp, direct, 0, null));
            List<Integer> first = new ArrayList<>();
            assertEquals(4, ByteOccurrences.visitBuffer(kmp, direct, 0, 4,
                    first::add, null));
            assertEquals(List.of(0, 1, 2, 3), first);
            assertThrows(IllegalArgumentException.class,
                    () -> ByteOccurrences.visitBuffer(kmp, direct, 0, -1,
                            ignored -> {}, null));
            assertThrows(NullPointerException.class,
                    () -> ByteOccurrences.countBuffer(kmp, null, 0, null));
        }
        AtomicInteger polls = new AtomicInteger();
        ProgressMonitor cancel = new ProgressMonitor() {
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long work) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return polls.incrementAndGet() >= 3; }
            @Override public void setCanceled(boolean value) {}
            @Override public ProgressState state() {
                return new DefaultProgressMonitor().state();
            }
        };
        try (CompiledSearch kmp = new KmpSearch(new byte[] {0, 1})) {
            assertThrows(CancellationException.class,
                    () -> ByteOccurrences.countBuffer(kmp, direct, 0, cancel));
            assertTrue(polls.get() >= 3);
        }
        CompiledSearch broken = new CompiledSearch() {
            @Override public int needleLength() { return 1; }
            @Override public String strategy() { return "BROKEN"; }
            @Override public int indexOf(byte[] data, int from) { return -1; }
            @Override public int indexOf(ByteBuffer data, int from) { return 0; }
        };
        assertThrows(IllegalStateException.class,
                () -> ByteOccurrences.visitBuffer(broken, ByteBuffer.wrap(new byte[] {1, 1}),
                        0, 4, ignored -> {}, null));
    }

    @Test
    void denseBmhByteBufferOverlapsReuseKmpAndPreserveBufferState() {
        for (int size : new int[]{0, 1, 64, 4096, 25000}) {
            for (int width : new int[]{1, 3, 128, 256}) {
                byte[] haystack = new byte[size];
                byte[] needle = new byte[width];
                Arrays.fill(haystack, (byte) 21);
                Arrays.fill(needle, (byte) 21);
                int count = Math.max(0, size - width + 1);
                ByteBuffer heap = ByteBuffer.allocate(size + 8);
                ByteBuffer direct = ByteBuffer.allocateDirect(size + 8);
                heap.position(4).put(haystack).limit(size + 4).position(4);
                direct.position(4).put(haystack).limit(size + 4).position(4);

                try (CompiledSearch compiled = new BoyerMooreHorspoolSearch(needle)) {
                    for (ByteBuffer buffer : new ByteBuffer[]{
                            heap, heap.asReadOnlyBuffer(),
                            direct, direct.asReadOnlyBuffer()}) {
                        int position = buffer.position();
                        int limit = buffer.limit();
                        buffer.mark();
                        assertEquals(count, ByteOccurrences.countBuffer(
                                compiled, buffer, 0, null));
                        List<Integer> first = new ArrayList<>();
                        int capped = Math.min(count, 5);
                        assertEquals(capped, ByteOccurrences.visitBuffer(
                                compiled, buffer, 0, 5, first::add, null));
                        for (int i = 0; i < first.size(); i++)
                            assertEquals(i, first.get(i));
                        assertEquals(position, buffer.position());
                        assertEquals(limit, buffer.limit());
                        buffer.reset();
                        assertEquals(position, buffer.position());
                    }
                }
            }
        }
    }

    @Test
    void denseBmhByteBufferContinuationRespectsCancellation() {
        byte[] haystack = new byte[24000];
        byte[] needle = new byte[256];
        Arrays.fill(haystack, (byte) 42);
        Arrays.fill(needle, (byte) 42);
        ByteBuffer direct = ByteBuffer.allocateDirect(haystack.length);
        direct.put(haystack).flip();
        AtomicInteger polls = new AtomicInteger();
        ProgressMonitor monitor = new ProgressMonitor() {
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long work) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return polls.incrementAndGet() >= 13; }
            @Override public void setCanceled(boolean value) {}
            @Override public ProgressState state() {
                return new DefaultProgressMonitor().state();
            }
        };
        direct.mark();
        try (CompiledSearch compiled = new BoyerMooreHorspoolSearch(needle)) {
            assertThrows(CancellationException.class,
                    () -> ByteOccurrences.countBuffer(compiled, direct, 0, monitor));
            assertTrue(polls.get() >= 13);
            assertEquals(0, direct.position());
            direct.reset();
            assertEquals(0, direct.position());
            assertEquals(haystack.length - needle.length + 1,
                    ByteOccurrences.countBuffer(compiled, direct, 0, null),
                    "a canceled scan must not close or corrupt its borrowed pattern");
        }
    }

    @Test
    void bmhBufferLongNearMatchChecksCancellationWithinOneSearch() {
        byte[] source = new byte[8192], needle = new byte[2048];
        Arrays.fill(source, (byte) 'a');
        Arrays.fill(needle, (byte) 'a');
        needle[0] = (byte) 'b';
        ByteBuffer direct = ByteBuffer.allocateDirect(source.length);
        direct.put(source).flip();
        direct.mark();
        AtomicInteger polls = new AtomicInteger();
        ProgressMonitor cancellation = new ProgressMonitor() {
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long work) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return polls.incrementAndGet() >= 6; }
            @Override public void setCanceled(boolean value) {}
            @Override public ProgressState state() { return new DefaultProgressMonitor().state(); }
        };
        try (CompiledSearch compiled = new BoyerMooreHorspoolSearch(needle)) {
            // Previous boundary-only implementation polls <6 times here.
            assertThrows(CancellationException.class,
                    () -> ByteOccurrences.countBuffer(compiled, direct, 0, cancellation));
            assertEquals(6, polls.get());
            assertEquals(0, direct.position());
            direct.reset();
            assertEquals(0, direct.position());
            assertEquals(-1, compiled.indexOf(direct, 0));
        }
    }

    private static List<Integer> oracle(byte[] haystack, byte[] needle, int fromIndex) {
        List<Integer> positions = new ArrayList<>();
        for (int start = Math.max(0, fromIndex); start <= haystack.length - needle.length;
                start++) {
            int matched = 0;
            while (matched < needle.length && haystack[start + matched] == needle[matched]) {
                matched++;
            }
            if (matched == needle.length) positions.add(start);
        }
        return positions;
    }
}
