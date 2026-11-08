// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/BmhCountOnlyTest.java (blob af970afc947c) by M3_SEARCH_FORWARD v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jdk.m3.collections.M3DefaultProgressMonitor;
import jdk.m3.collections.M3ProgressMonitor;
import jdk.m3.collections.M3ProgressState;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** JUnit obligations for original Horspool dispatch and shared count/KMP seam atoms. */
final class BmhCountOnlyTest {
    @Test void sourceLocalDifferentialAndIndependentOracle() {
        assertEquals(122_875L, BmhCountOnlyContract.runOracle());
    }

    @Test void densePeriodicSeamCountsAllOverlapsWithoutChangingVisitor() throws Exception {
        byte[] pattern = new byte[768], input = new byte[3072];
        for (int i = 0; i < pattern.length; i++) pattern[i] = (byte) (i % 3);
        for (int i = 0; i < input.length; i++) input[i] = (byte) (i % 3);
        BoyerMooreHorspoolSearch compiled = new BoyerMooreHorspoolSearch(pattern);
        Field lazy = compiled.getClass().getDeclaredField("overlapSearch");
        lazy.setAccessible(true);
        assertEquals(null, lazy.get(compiled));
        int count = ByteOccurrences.count(compiled, input, 0, null);
        assertEquals(769, count);
        assertTrue(lazy.get(compiled) != null, "dense fallback must reuse original KMP owner");
        List<Integer> visited = new ArrayList<>();
        assertEquals(769, ByteOccurrences.visit(compiled, input, 0, Integer.MAX_VALUE,
                visited::add, null));
        for (int i = 0; i < visited.size(); i++) assertEquals(3 * i, visited.get(i).intValue());
        assertEquals(0, ByteOccurrences.count(compiled, input, Integer.MAX_VALUE, null));
    }

    @Test void cancellationChecksAndExceptionBoundaryEqualOldCountVisitor() {
        byte[] needle = new byte[24], input = new byte[9216];
        for (int i = 0; i < needle.length; i++) needle[i] = (byte) (i % 3);
        for (int i = 0; i < input.length; i++) input[i] = (byte) (i % 3);
        BoyerMooreHorspoolSearch candidate = new BoyerMooreHorspoolSearch(needle);
        BoyerMooreHorspoolSearch baseline = new BoyerMooreHorspoolSearch(needle);
        CheckMonitor counted = new CheckMonitor(-1), visited = new CheckMonitor(-1);
        assertEquals(3065, ByteOccurrences.count(candidate, input, 0, counted));
        visited.checkCanceled();
        assertEquals(3065, ByteOccurrences.visit(baseline, input, 0,
                Integer.MAX_VALUE, ignored -> {}, visited));
        assertEquals(visited.polls.get(), counted.polls.get(),
                "original count->visit and direct count must poll equally");
        CheckMonitor cancellation = new CheckMonitor(17);
        assertThrows(CancellationException.class,
                () -> ByteOccurrences.count(candidate, input, 0, cancellation));
        assertTrue(cancellation.polls.get() >= 17);
        assertEquals(3065, ByteOccurrences.count(candidate, input, 0, null));
    }

    @Test void compiledNeedleOwnershipAndExternalProvidersRemainUnchanged() {
        byte[] needle = {0, -1, 0};
        BoyerMooreHorspoolSearch candidate = new BoyerMooreHorspoolSearch(needle);
        needle[1] = 42;
        byte[] input = {0, -1, 0, -1, 0, -1, 0};
        byte[] original = input.clone();
        assertEquals(3, ByteOccurrences.count(candidate, input, Integer.MIN_VALUE, null));
        assertEquals(2, ByteOccurrences.count(candidate, input, 1, null));
        assertEquals(0, ByteOccurrences.count(candidate, input, Integer.MAX_VALUE, null));
        assertTrue(Arrays.equals(original, input), "count must not mutate caller byte array");
        assertThrows(IllegalArgumentException.class,
                () -> ByteOccurrences.visit(candidate, input, 0, -1, ignored -> {}, null));
    }

    private static final class CheckMonitor implements M3ProgressMonitor {
        private final AtomicInteger polls = new AtomicInteger();
        private final int failAt;
        CheckMonitor(int failAt) { this.failAt = failAt; }
        @Override public void beginTask(String name, long totalWork) { }
        @Override public void done() { }
        @Override public void worked(long count) { }
        @Override public void setProgress(long worked, long totalWork) { }
        @Override public void setTaskName(String name) { }
        @Override public void subTask(String subTaskName) { }
        @Override public boolean isCanceled() {
            return failAt > 0 && polls.incrementAndGet() >= failAt;
        }
        @Override public void setCanceled(boolean canceled) { }
        @Override public M3ProgressState state() { return new M3DefaultProgressMonitor().state(); }
    }
}
