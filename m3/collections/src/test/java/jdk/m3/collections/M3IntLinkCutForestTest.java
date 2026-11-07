// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;
import jdk.m3.collections.graph.IntLinkCutForest;

class M3IntLinkCutForestTest {
    @Test void dynamicOperationsMatchRebuiltAdjacencyPaths() {
        Random random = new Random(0x4c494e4b435554L);
        int n = 25;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) { values[i] = random.nextInt(); }
        IntLinkCutForest forest = new IntLinkCutForest(values);
        boolean[][] edges = new boolean[n][n];
        for (int step = 0; step < 30_000; step++) {
            int a = random.nextInt(n), b = random.nextInt(n);
            int[] path = path(edges, a, b);
            switch (random.nextInt(5)) {
                case 0 -> {
                    assertEquals(path.length == 0, forest.link(a, b), "link step=" + step);
                    if (path.length == 0) { edges[a][b] = true; edges[b][a] = true; }
                }
                case 1 -> {
                    assertEquals(edges[a][b], forest.cut(a, b), "cut step=" + step);
                    edges[a][b] = false; edges[b][a] = false;
                }
                case 2 -> { values[a] = random.nextInt(); forest.setValue(a, values[a]); }
                default -> {
                    assertEquals(path.length != 0, forest.connected(a, b));
                    if (path.length > 0) { assertPath(forest, values, path); }
                    else { assertThrows(IllegalArgumentException.class, () -> forest.pathSum(a, b)); }
                }
            }
            assertEquals(values[a], forest.valueAt(a));
            if (step % 1000 == 0) {
                for (int x = 0; x < n; x++) {
                    for (int y = 0; y < n; y++) {
                        int[] expected = path(edges, x, y);
                        assertEquals(expected.length > 0, forest.connected(x, y));
                        if (expected.length > 0) { assertPath(forest, values, expected); }
                    }
                }
            }
        }
    }

    @Test void deepChainsUseExplicitAncestorsAndExactLongAggregates() {
        int n = 120_000;
        int[] values = new int[n]; Arrays.fill(values, Integer.MAX_VALUE);
        IntLinkCutForest forest = new IntLinkCutForest(values);
        for (int i = 1; i < n; i++) { assertTrue(forest.link(i - 1, i)); }
        assertEquals((long) n * Integer.MAX_VALUE, forest.pathSum(0, n - 1));
        assertEquals(n, forest.pathSize(n - 1, 0));
        assertEquals(12_345, forest.vertexAt(0, n - 1, 12_345));
        assertEquals(n - 1 - 12_345, forest.vertexAt(n - 1, 0, 12_345));
        forest.setValue(n / 2, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, forest.pathMinimum(0, n - 1));
        assertEquals((long) (n - 1) * Integer.MAX_VALUE + Integer.MIN_VALUE, forest.pathSum(n - 1, 0));
        assertFalse(forest.cut(0, n - 1)); assertFalse(forest.link(0, n - 1));
        assertTrue(forest.cut(n / 2 - 1, n / 2)); assertFalse(forest.connected(0, n - 1));
        assertTrue(forest.link(n / 2, n / 2 - 1)); assertTrue(forest.connected(0, n - 1));
    }

    @Test void snapshotsAndRejectedOperationsPreserveLogicalState() {
        int[] values = {Integer.MIN_VALUE, 7, Integer.MAX_VALUE, -5};
        IntLinkCutForest forest = new IntLinkCutForest(values);
        values[0] = 123;
        assertEquals(Integer.MIN_VALUE, forest.valueAt(0));
        assertTrue(forest.link(0, 1)); assertTrue(forest.link(1, 2));
        assertFalse(forest.link(0, 2)); assertFalse(forest.link(1, 1));
        assertFalse(forest.cut(0, 2)); assertFalse(forest.cut(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> forest.link(-1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> forest.cut(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> forest.setValue(4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> forest.vertexAt(0, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> forest.pathMinimum(0, 3));
        assertThrows(IllegalArgumentException.class, () -> forest.pathMaximum(3, 1));
        assertThrows(IllegalArgumentException.class, () -> forest.pathSize(3, 2));
        assertThrows(IllegalArgumentException.class, () -> forest.vertexAt(3, 2, 0));
        assertEquals(6, forest.pathSum(0, 2));
        assertEquals(2, forest.vertexAt(2, 0, 0)); assertEquals(0, forest.vertexAt(2, 0, 2));
        assertTrue(forest.cut(2, 1)); assertFalse(forest.connected(0, 2));
        assertEquals(0, new IntLinkCutForest(new int[0]).vertices());
        assertThrows(NullPointerException.class, () -> new IntLinkCutForest(null));
    }

    private static void assertPath(IntLinkCutForest forest, int[] values, int[] path) {
        long sum = 0; int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int vertex : path) { sum += values[vertex]; min = Math.min(min, values[vertex]); max = Math.max(max, values[vertex]); }
        int from = path[0], to = path[path.length - 1];
        assertEquals(sum, forest.pathSum(from, to)); assertEquals(min, forest.pathMinimum(from, to));
        assertEquals(max, forest.pathMaximum(from, to)); assertEquals(path.length, forest.pathSize(from, to));
        for (int ordinal = 0; ordinal < path.length; ordinal++) { assertEquals(path[ordinal], forest.vertexAt(from, to, ordinal)); }
    }

    private static int[] path(boolean[][] edges, int from, int to) {
        int n = edges.length; int[] previous = new int[n], queue = new int[n];
        Arrays.fill(previous, -1); previous[from] = from; queue[0] = from;
        int read = 0, write = 1;
        while (read < write) {
            int a = queue[read++];
            for (int b = 0; b < n; b++) {
                if (edges[a][b] && previous[b] < 0) { previous[b] = a; queue[write++] = b; }
            }
        }
        if (previous[to] < 0) { return new int[0]; }
        int count = 1;
        for (int at = to; at != from; at = previous[at]) { count++; }
        int[] result = new int[count];
        for (int at = to; count > 0; at = previous[at]) { result[--count] = at; }
        return result;
    }
}
