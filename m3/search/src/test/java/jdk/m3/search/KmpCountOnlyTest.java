// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/KmpCountOnlyTest.java (blob 91f6c768c68f) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.DefaultProgressMonitor;
import jdk.m3.collections.ProgressMonitor;
import jdk.m3.collections.ProgressState;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** JUnit obligations for the count-only scanner and its public dispatch atom. */
final class KmpCountOnlyTest {
    @Test void randomizedIndependentOracleCoversBothCountAtoms() {
        assertEquals(100_000L, KmpCountOnlyContract.runOracle());
    }

    @Test void periodicOverlapsCountWithoutEmittingPositionCallbacks() {
        byte[] input = new byte[4096];
        byte[] pattern = new byte[256];
        Arrays.fill(input, (byte) 'x');
        Arrays.fill(pattern, (byte) 'x');
        KmpSearch kmp = new KmpSearch(pattern);
        assertEquals(4096 - 256 + 1, kmp.countMatches(input, 0, new DefaultProgressMonitor()));
        assertEquals(4096 - 256 + 1, ByteOccurrences.count(kmp, input, 0, null));
        List<Integer> first = new ArrayList<>();
        assertEquals(3, ByteOccurrences.visit(kmp, input, 0, 3, first::add, null));
        assertEquals(List.of(0, 1, 2), first);
    }

    @Test void byteArrayOwnershipAndExtremeOffsetsRemainUnchanged() {
        byte[] needle = {0, -1, 0};
        KmpSearch kmp = new KmpSearch(needle);
        needle[1] = 12;
        byte[] input = {0, -1, 0, -1, 0, -1, 0};
        assertEquals(3, ByteOccurrences.count(kmp, input, Integer.MIN_VALUE, null));
        assertEquals(2, ByteOccurrences.count(kmp, input, 1, null));
        assertEquals(0, ByteOccurrences.count(kmp, input, Integer.MAX_VALUE, null));
        assertEquals(0, ByteOccurrences.count(kmp, new byte[0], 0, null));
    }

    @Test void cancelPollingMatchesOriginalVisitorCheckpoints() {
        byte[] haystack = new byte[65_537];
        byte[] needle = {1};
        Arrays.fill(haystack, (byte) 1);
        KmpSearch kmp = new KmpSearch(needle);
        Probe counter = new Probe(-1);
        Probe visitor = new Probe(-1);
        assertEquals(haystack.length, ByteOccurrences.count(kmp, haystack, 0, counter));
        assertEquals(haystack.length, ByteOccurrences.visit(kmp, haystack, 0,
                Integer.MAX_VALUE, ignored -> {}, visitor));
        assertEquals(visitor.polls.get() + 1, counter.polls.get(),
                "count retains original count entry plus visit cancellation checks");
        Probe absentCount = new Probe(-1);
        Probe absentVisit = new Probe(-1);
        byte[] absent = new byte[8193];
        assertEquals(0, ByteOccurrences.count(kmp, absent, 0, absentCount));
        assertEquals(0, ByteOccurrences.visit(kmp, absent, 0, Integer.MAX_VALUE,
                ignored -> {}, absentVisit));
        assertEquals(absentVisit.polls.get() + 1, absentCount.polls.get(),
                "no-hit count retains original count entry before visitor checkpoints");
        Probe pastEndCount = new Probe(-1);
        Probe pastEndVisit = new Probe(-1);
        assertEquals(0, ByteOccurrences.count(kmp, absent, Integer.MAX_VALUE, pastEndCount));
        assertEquals(0, ByteOccurrences.visit(kmp, absent, Integer.MAX_VALUE,
                Integer.MAX_VALUE, ignored -> {}, pastEndVisit));
        assertEquals(pastEndVisit.polls.get() + 1, pastEndCount.polls.get(),
                "past-end count retains original count and visitor entry/exit checks");
        Probe canceled = new Probe(17);
        assertThrows(CancellationException.class,
                () -> ByteOccurrences.count(kmp, haystack, 0, canceled));
        assertTrue(canceled.polls.get() >= 17);
        assertEquals(haystack.length, ByteOccurrences.count(kmp, haystack, 0, null),
                "cancellation must not damage the precomputed pattern");
    }

    private static final class Probe implements ProgressMonitor {
        private final AtomicInteger polls = new AtomicInteger();
        private final int cancelAt;
        private Probe(int cancelAt) { this.cancelAt = cancelAt; }
        @Override public void beginTask(String name, long total) {}
        @Override public void done() {}
        @Override public void worked(long amount) {}
        @Override public void setProgress(long worked, long total) {}
        @Override public void setTaskName(String name) {}
        @Override public void subTask(String name) {}
        @Override public boolean isCanceled() {
            int observed = polls.incrementAndGet();
            return cancelAt >= 0 && observed >= cancelAt;
        }
        @Override public void setCanceled(boolean canceled) {}
        @Override public ProgressState state() { return new DefaultProgressMonitor().state(); }
    }
}
