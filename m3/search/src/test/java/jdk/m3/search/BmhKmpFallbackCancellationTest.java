// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BmhKmpFallbackCancellationTest.java (blob 9c40b62d9ed0) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Contract regression: monitored BMH budget fallback must not discard the caller's monitor. */
final class BmhKmpFallbackCancellationTest {
    private static final int HAYSTACK_LENGTH = 65_536;
    private static final int NEEDLE_LENGTH = 1_024;
    private static final int FALLBACK_CANCEL_POLL = 68;

    @Test
    void forwardKmpFallbackHonorsOriginalCancellationInstance() {
        ByteBuffer buffer = directAllA();
        byte[] needle = forwardNeedle();
        CancellationException original = new CancellationException("forward-monitor");
        Polling monitor = new Polling(FALLBACK_CANCEL_POLL, original);
        assertSame(original, assertThrows(CancellationException.class,
                () -> ByteOccurrences.countBuffer(
                        new BoyerMooreHorspoolSearch(needle), buffer, 0, monitor)));
        assertEquals(FALLBACK_CANCEL_POLL, monitor.polls.get());
        assertEquals(3, buffer.position());
        assertEquals(HAYSTACK_LENGTH + 3, buffer.limit());
        buffer.reset();
        assertEquals(3, buffer.position());
    }

    @Test
    void reverseKmpFallbackHonorsOriginalCancellationInstance() {
        ByteBuffer buffer = directAllA();
        byte[] needle = reverseNeedle();
        CancellationException original = new CancellationException("reverse-monitor");
        Polling monitor = new Polling(FALLBACK_CANCEL_POLL, original);
        assertSame(original, assertThrows(CancellationException.class,
                () -> new BoyerMooreHorspoolSearch(needle)
                        .lastIndexOf(buffer, Integer.MAX_VALUE, monitor)));
        assertEquals(FALLBACK_CANCEL_POLL, monitor.polls.get());
        assertEquals(3, buffer.position());
        assertEquals(HAYSTACK_LENGTH + 3, buffer.limit());
        buffer.reset();
        assertEquals(3, buffer.position());
    }

    @Test
    void firstAndLastOffsetsStayExactAfterMonitoredFallback() {
        byte[] text = new byte[HAYSTACK_LENGTH];
        Arrays.fill(text, (byte) 'a');
        byte[] forward = forwardNeedle();
        text[60_000] = (byte) 'b';
        ByteBuffer direct = ByteBuffer.allocateDirect(text.length + 5);
        for (int i = 0; i < text.length; ++i) direct.put(i + 3, text[i]);
        direct.position(3);
        direct.limit(text.length + 3);
        direct.mark();
        assertEquals(1, ByteOccurrences.countBuffer(
                new BoyerMooreHorspoolSearch(forward), direct, 0, new Polling(-1, null)));
        assertEquals(60_000, new BoyerMooreHorspoolSearch(forward).indexOf(direct, 0));
        direct.reset();
        assertEquals(3, direct.position());

        Arrays.fill(text, (byte) 'a');
        text[NEEDLE_LENGTH - 1] = (byte) 'b';
        for (int i = 0; i < text.length; ++i) direct.put(i + 3, text[i]);
        assertEquals(0, new BoyerMooreHorspoolSearch(reverseNeedle())
                .lastIndexOf(direct, Integer.MAX_VALUE, new Polling(-1, null)));
        assertEquals(3, direct.position());
        assertEquals(HAYSTACK_LENGTH + 3, direct.limit());
        direct.reset();
        assertEquals(3, direct.position());
    }

    @Test
    void noMatchAndNoopPreserveOriginalSearchResults() {
        ByteBuffer buffer = directAllA();
        byte[] forward = forwardNeedle();
        byte[] reverse = reverseNeedle();
        assertEquals(0, ByteOccurrences.countBuffer(
                new BoyerMooreHorspoolSearch(forward), buffer, 0, null));
        assertEquals(-1, new BoyerMooreHorspoolSearch(reverse)
                .lastIndexOf(buffer, Integer.MAX_VALUE, new M3DefaultProgressMonitor()));
        assertEquals(-1, new BoyerMooreHorspoolSearch(reverse)
                .lastIndexOf(buffer, Integer.MAX_VALUE));
    }

    private static byte[] forwardNeedle() {
        byte[] pattern = new byte[NEEDLE_LENGTH];
        Arrays.fill(pattern, (byte) 'a');
        pattern[0] = (byte) 'b';
        return pattern;
    }
    private static byte[] reverseNeedle() {
        byte[] pattern = new byte[NEEDLE_LENGTH];
        Arrays.fill(pattern, (byte) 'a');
        pattern[NEEDLE_LENGTH - 1] = (byte) 'b';
        return pattern;
    }
    private static ByteBuffer directAllA() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(HAYSTACK_LENGTH + 5);
        for (int i = 0; i < HAYSTACK_LENGTH; i++) buffer.put(3 + i, (byte) 'a');
        buffer.position(3);
        buffer.limit(3 + HAYSTACK_LENGTH);
        buffer.mark();
        return buffer;
    }
    private static final class Polling implements M3ProgressMonitor {
        final AtomicInteger polls = new AtomicInteger();
        final int cancelAt;
        final CancellationException failure;
        Polling(int cancelAt, CancellationException failure) { this.cancelAt = cancelAt; this.failure = failure; }
        @Override public void checkCanceled() {
            if (polls.incrementAndGet() == cancelAt) throw failure;
        }
        @Override public void beginTask(String name,long total) {}
        @Override public void done() {}
        @Override public void worked(long work) {}
        @Override public void setProgress(long worked,long total) {}
        @Override public void setTaskName(String name) {}
        @Override public void subTask(String name) {}
        @Override public boolean isCanceled() { return false; }
        @Override public void setCanceled(boolean value) {}
        @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
    }
}
