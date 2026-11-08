// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/JavaAdaptiveSearchEngineTest.java (blob 7062f4eb29cd) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class JavaAdaptiveSearchEngineTest {
    @Test
    void findsExactNeedlesWithPrecomputedStrategies() {
        SearchEngine engine = new JavaAdaptiveSearchEngine();
        try (CompiledSearch shortPattern = engine.compile(bytes("aba"));
             CompiledSearch longPattern = engine.compile(bytes("needle-value"))) {
            assertEquals(2, shortPattern.indexOf(bytes("xxabaaba"), 0));
            assertEquals(7, longPattern.indexOf(bytes("prefix-needle-value-suffix"), 0));
            assertEquals(-1, longPattern.indexOf(bytes("no match"), 0));
            assertEquals("KMP", shortPattern.strategy());
            assertEquals("BOYER_MOORE_HORSPOOL", longPattern.strategy());
        }
    }

    @Test
    void findsLastExactNeedleWithoutChangingForwardSemantics() {
        SearchEngine engine = new JavaAdaptiveSearchEngine();
        byte[] overlapping = bytes("ababa");
        try (CompiledSearch kmp = engine.compile(bytes("aba"));
             CompiledSearch horspool = engine.compile(bytes("needle-value"))) {
            assertEquals(2, kmp.lastIndexOf(overlapping, Integer.MAX_VALUE));
            assertEquals(0, kmp.lastIndexOf(overlapping, 1));
            assertEquals(-1, kmp.lastIndexOf(overlapping, -1));

            byte[] repeated = bytes("needle-value/needle-value");
            assertEquals(13, horspool.lastIndexOf(repeated, Integer.MAX_VALUE));
            assertEquals(0, horspool.lastIndexOf(repeated, 12));
            assertEquals(-1, horspool.lastIndexOf(repeated, -1));

            assertEquals(0, kmp.indexOf(overlapping, 0));
            assertEquals(13, horspool.indexOf(repeated, 1));
        }
    }

    @Test
    void searchesHeapAndDirectBufferRemainingRangesWithoutMutatingState() {
        SearchEngine engine = new JavaAdaptiveSearchEngine();
        byte[] payload = bytes("xxababa/needle-value/needle-valueyy");
        for (boolean direct : new boolean[] {false, true}) {
            ByteBuffer buffer = direct
                    ? ByteBuffer.allocateDirect(payload.length + 6)
                    : ByteBuffer.allocate(payload.length + 6);
            for (int index = 0; index < payload.length; index++) {
                buffer.put(3 + index, payload[index]);
            }
            buffer.position(3);
            buffer.limit(3 + payload.length);
            int position = buffer.position();
            int limit = buffer.limit();
            try (CompiledSearch kmp = engine.compile(bytes("aba"));
                 CompiledSearch horspool = engine.compile(bytes("needle-value"))) {
                assertEquals(2, kmp.indexOf(buffer, 0));
                assertEquals(4, kmp.lastIndexOf(buffer, Integer.MAX_VALUE));
                assertEquals(8, horspool.indexOf(buffer, 0));
                assertEquals(21, horspool.lastIndexOf(buffer, Integer.MAX_VALUE));
            }
            assertEquals(position, buffer.position());
            assertEquals(limit, buffer.limit());
        }
    }

    @Test
    void compiledPatternDoesNotRetainCallerArray() {
        byte[] needle = bytes("needle-value");
        CompiledSearch compiled = new JavaAdaptiveSearchEngine().compile(needle);
        String strategy = compiled.strategy();
        needle[0] = 'X';
        assertEquals(2, compiled.indexOf(bytes("xxneedle-value"), 0));
        assertNotEquals("", strategy);
        compiled.close();
    }

    @Test
    void reverseBmhNearMatchPollsWithinOneLongDirectBufferSearch() {
        byte[] text = new byte[8192];
        byte[] needle = new byte[2048];
        Arrays.fill(text, (byte) 'a');
        Arrays.fill(needle, (byte) 'a');
        needle[needle.length - 1] = (byte) 'b';
        ByteBuffer direct = ByteBuffer.allocateDirect(text.length);
        direct.put(text).flip();
        direct.mark();
        AtomicInteger polls = new AtomicInteger();
        M3ProgressMonitor stopOnSixth = new M3ProgressMonitor() {
            @Override public void beginTask(String name, long total) {}
            @Override public void done() {}
            @Override public void worked(long work) {}
            @Override public void setProgress(long worked, long total) {}
            @Override public void setTaskName(String name) {}
            @Override public void subTask(String name) {}
            @Override public boolean isCanceled() { return polls.incrementAndGet() >= 6; }
            @Override public void setCanceled(boolean value) {}
            @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
        };
        try (CompiledSearch compiled = new BoyerMooreHorspoolSearch(needle)) {
            // The old default polls only at reverse-search boundaries.
            assertThrows(CancellationException.class,
                    () -> compiled.lastIndexOf(direct, Integer.MAX_VALUE, stopOnSixth));
            assertEquals(6, polls.get(), "reverse search must poll inside BMH work");
            assertEquals(0, direct.position());
            assertEquals(text.length, direct.limit());
            direct.reset();
            assertEquals(0, direct.position(), "input mark must survive cancellation");
            assertEquals(-1, compiled.lastIndexOf(direct, Integer.MAX_VALUE),
                    "borrowed compiled pattern must remain usable");
        }
    }

    @Test
    void monitoredReverseBmhMatchesOriginalOffsetsAndNeverMovesByteBufferCursor() {
        Random random = new Random(0xB0A1156L);
        for (int sample = 0; sample < 1200; sample++) {
            byte[] text = new byte[random.nextInt(172)];
            byte[] needle = new byte[1 + random.nextInt(53)];
            random.nextBytes(text);
            random.nextBytes(needle);
            if (sample % 3 == 0) {
                Arrays.fill(text, (byte) 17);
                Arrays.fill(needle, (byte) 17);
            } else if (sample % 3 == 1) {
                for (int i = 0; i < text.length; i++) text[i] = (byte) (i % 3);
                for (int i = 0; i < needle.length; i++) needle[i] = (byte) (i % 3);
            }
            int from = sample % 7 == 0 ? Integer.MAX_VALUE
                    : sample % 7 == 1 ? -1
                    : random.nextInt(text.length + 38) - 19;
            int expected = -1;
            for (int start = Math.min(from, text.length - needle.length);
                    start >= 0; start--) {
                int matched = 0;
                while (matched < needle.length && text[start + matched] == needle[matched]) {
                    matched++;
                }
                if (matched == needle.length) {
                    expected = start;
                    break;
                }
            }
            try (CompiledSearch compiled = new BoyerMooreHorspoolSearch(needle)) {
                for (boolean direct : new boolean[]{false, true}) {
                    ByteBuffer buffer = direct ? ByteBuffer.allocateDirect(text.length + 8)
                            : ByteBuffer.allocate(text.length + 8);
                    buffer.position(3).put(text).limit(3 + text.length).position(3);
                    for (boolean readOnly : new boolean[]{false, true}) {
                        ByteBuffer view = readOnly ? buffer.asReadOnlyBuffer() : buffer.duplicate();
                        int pos = view.position(), limit = view.limit();
                        view.mark();
                        assertEquals(expected, compiled.lastIndexOf(view, from));
                        assertEquals(expected, compiled.lastIndexOf(
                                view, from, new M3DefaultProgressMonitor()));
                        assertEquals(pos, view.position());
                        assertEquals(limit, view.limit());
                        view.reset();
                        assertEquals(pos, view.position());
                    }
                }
            }
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
