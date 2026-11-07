// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3IntOfflineConnectivity;
import jdk.internal.m3.collections.M3IntUndirectedLowLink;

class M3PrimitiveConnectivityKernelsTest {
    @Test void lowLinkMatchesIndependentEdgeAndVertexRemoval() {
        Random random = new Random(0x4c4f574c494e4bL);
        for (int trial = 0; trial < 1400; trial++) {
            int n = random.nextInt(11), edges = n == 0 ? 0 : random.nextInt(27);
            int[] from = new int[edges], to = new int[edges];
            for (int e = 0; e < edges; e++) { from[e] = random.nextInt(n); to[e] = random.nextInt(n); }
            M3IntUndirectedLowLink low = new M3IntUndirectedLowLink(M3WeightedIntCsrGraph.undirected(n, from, to, new long[edges]));
            boolean[] removed = new boolean[edges], bridges = new boolean[edges];
            int original = componentCount(labels(n, from, to, removed, -1));
            for (int e = 0; e < edges; e++) {
                removed[e] = true; bridges[e] = componentCount(labels(n, from, to, removed, -1)) > original;
                assertEquals(bridges[e], low.isBridge(e)); removed[e] = false;
            }
            for (int v = 0; v < n; v++) { assertEquals(componentCount(labels(n, from, to, removed, v)) > original, low.isArticulation(v)); }
            int[] components = labels(n, from, to, bridges, -1);
            assertEquals(componentCount(components), low.componentCount());
            for (int a = 0; a < n; a++) { for (int b = 0; b < n; b++) {
                assertEquals(components[a] == components[b], low.componentOf(a) == low.componentOf(b));
            } }
        }
    }

    @Test void lowLinkHandlesDeepChainsAndParallelParentEdges() {
        int n = 120_000; int[] from = new int[n - 1], to = new int[n - 1];
        for (int e = 0; e < from.length; e++) { from[e] = e; to[e] = e + 1; }
        M3IntUndirectedLowLink chain = new M3IntUndirectedLowLink(M3WeightedIntCsrGraph.undirected(n, from, to, new long[n - 1]));
        assertEquals(n, chain.componentCount());
        for (int e = 0; e < from.length; e++) { assertTrue(chain.isBridge(e)); }
        for (int v = 0; v < n; v++) { assertEquals(v > 0 && v < n - 1, chain.isArticulation(v)); }
        M3IntUndirectedLowLink parallel = new M3IntUndirectedLowLink(M3WeightedIntCsrGraph.undirected(3,
                new int[]{0, 0, 1, 1}, new int[]{1, 1, 2, 1}, new long[4]));
        assertFalse(parallel.isBridge(0)); assertFalse(parallel.isBridge(1));
        assertTrue(parallel.isBridge(2)); assertFalse(parallel.isBridge(3));
        assertTrue(parallel.isArticulation(1)); assertEquals(2, parallel.componentCount());
        assertThrows(IllegalArgumentException.class, () -> new M3IntUndirectedLowLink(M3WeightedIntCsrGraph.directed(0, new int[0], new int[0], new long[0])));
    }

    @Test void offlineConnectivityMatchesFreshGraphAtEveryQueryTime() {
        Random random = new Random(0x4f46464c494e45L);
        for (int trial = 0; trial < 800; trial++) {
            int n = 1 + random.nextInt(14), times = 1 + random.nextInt(17), edges = random.nextInt(40);
            int[] from = new int[edges], to = new int[edges], start = new int[edges], end = new int[edges];
            for (int e = 0; e < edges; e++) {
                from[e] = random.nextInt(n); to[e] = random.nextInt(n);
                start[e] = random.nextInt(times + 1); end[e] = start[e] + random.nextInt(times - start[e] + 1);
            }
            M3IntOfflineConnectivity schedule = new M3IntOfflineConnectivity(n, times, from, to, start, end);
            M3IntOfflineConnectivity.Workspace workspace = schedule.workspace(50);
            for (int batch = 0; batch < 4; batch++) {
                int q = random.nextInt(51); int[] queryTimes = new int[q], queryFrom = new int[q], queryTo = new int[q];
                boolean[] actual = new boolean[q + 1]; Arrays.fill(actual, true);
                for (int i = 0; i < q; i++) { queryTimes[i] = random.nextInt(times); queryFrom[i] = random.nextInt(n); queryTo[i] = random.nextInt(n); }
                workspace.solve(queryTimes, queryFrom, queryTo, actual);
                boolean[] removed = new boolean[edges];
                for (int i = 0; i < q; i++) {
                    for (int e = 0; e < edges; e++) { removed[e] = queryTimes[i] < start[e] || queryTimes[i] >= end[e]; }
                    int[] components = labels(n, from, to, removed, -1);
                    assertEquals(components[queryFrom[i]] == components[queryTo[i]], actual[i]);
                }
                assertTrue(actual[q], "output suffix changed");
            }
        }
    }

    @Test void offlineHalfOpenLifetimesSnapshotAndMultipleWorkspaces() {
        int[] from = {0, 1, 0, 0}, to = {1, 2, 1, 0}, start = {0, 1, 2, 0}, end = {2, 3, 4, 4};
        M3IntOfflineConnectivity schedule = new M3IntOfflineConnectivity(3, 4, from, to, start, end);
        Arrays.fill(from, 2); Arrays.fill(to, 2); Arrays.fill(start, 4); Arrays.fill(end, 4);
        int[] times = {3, 0, 2, 1, 2, 0}, a = {0, 0, 0, 0, 1, 2}, b = {2, 2, 2, 2, 1, 2};
        boolean[] output = new boolean[6], expected = {false, false, true, true, true, true};
        var first = schedule.workspace(6); var second = schedule.workspace(6);
        first.solve(times, a, b, output); assertArrayEquals(expected, output);
        Arrays.fill(output, false); second.solve(times, a, b, output); assertArrayEquals(expected, output);
        first.solve(times, a, b, output); assertArrayEquals(expected, output);
        assertArrayEquals(new int[]{3, 0, 2, 1, 2, 0}, times);
        assertEquals(3, schedule.vertices()); assertEquals(4, schedule.times()); assertEquals(4, schedule.lifetimes());
        assertTrue(schedule.scheduledOccurrences() >= 4);
    }

    @Test void offlineRejectsEntireInvalidBatchBeforeWritingAnyAnswer() {
        M3IntOfflineConnectivity schedule = new M3IntOfflineConnectivity(2, 3, new int[]{0}, new int[]{1}, new int[]{0}, new int[]{1});
        var workspace = schedule.workspace(2); boolean[] output = {false, true};
        assertThrows(IndexOutOfBoundsException.class, () -> workspace.solve(new int[]{0, 3}, new int[]{0, 0}, new int[]{1, 1}, output));
        assertArrayEquals(new boolean[]{false, true}, output);
        assertThrows(IndexOutOfBoundsException.class, () -> workspace.solve(new int[]{0, 0}, new int[]{0, 2}, new int[]{1, 1}, output));
        assertArrayEquals(new boolean[]{false, true}, output);
        assertThrows(IllegalArgumentException.class, () -> workspace.solve(new int[]{0, 0, 0}, new int[3], new int[3], new boolean[3]));
        assertThrows(IllegalArgumentException.class, () -> workspace.solve(new int[1], new int[0], new int[1], output));
        assertThrows(IllegalArgumentException.class, () -> workspace.solve(new int[2], new int[2], new int[2], new boolean[1]));
        workspace.solve(new int[]{0, 1}, new int[]{0, 0}, new int[]{1, 1}, output);
        assertArrayEquals(new boolean[]{true, false}, output);
        assertThrows(IllegalArgumentException.class, () -> schedule.workspace(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> new M3IntOfflineConnectivity(2, 3, new int[]{0}, new int[]{1}, new int[]{2}, new int[]{1}));
        assertThrows(IllegalArgumentException.class, () -> new M3IntOfflineConnectivity(0, Integer.MAX_VALUE, new int[0], new int[0], new int[0], new int[0]));
    }

    @Test void offlineAllowsEmptyHorizonAndIgnoredEmptyLifetimes() {
        M3IntOfflineConnectivity empty = new M3IntOfflineConnectivity(0, 0, new int[0], new int[0], new int[0], new int[0]);
        empty.workspace(0).solve(new int[0], new int[0], new int[0], new boolean[0]);
        assertEquals(0, empty.scheduledOccurrences());
        M3IntOfflineConnectivity zero = new M3IntOfflineConnectivity(1, 0, new int[]{0}, new int[]{0}, new int[]{0}, new int[]{0});
        assertEquals(0, zero.scheduledOccurrences());
        assertThrows(IndexOutOfBoundsException.class, () -> zero.workspace(1).solve(new int[]{0}, new int[]{0}, new int[]{0}, new boolean[1]));
    }

    private static int[] labels(int n, int[] from, int[] to, boolean[] removed, int removedVertex) {
        int[] result = new int[n], queue = new int[n]; Arrays.fill(result, -1); int count = 0;
        for (int root = 0; root < n; root++) {
            if (root == removedVertex || result[root] >= 0) { continue; }
            int head = 0, tail = 0; queue[tail++] = root; result[root] = count;
            while (head < tail) {
                int vertex = queue[head++];
                for (int e = 0; e < from.length; e++) {
                    if (removed[e] || from[e] == removedVertex || to[e] == removedVertex) { continue; }
                    int target = from[e] == vertex ? to[e] : to[e] == vertex ? from[e] : -1;
                    if (target >= 0 && result[target] < 0) { result[target] = count; queue[tail++] = target; }
                }
            }
            count++;
        }
        return result;
    }
    private static int componentCount(int[] labels) {
        int count = 0; for (int label : labels) { count = Math.max(count, label + 1); } return count;
    }
}
