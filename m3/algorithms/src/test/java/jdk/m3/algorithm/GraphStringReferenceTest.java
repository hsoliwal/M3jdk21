// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import static jdk.m3.algorithm.SortingRangeStreamingReferenceTest.finding;
import static jdk.m3.algorithm.SortingRangeStreamingReferenceTest.report;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

//FLLDCIM: ORACLE - M3JDK-authored reference oracle of recipe/algorithm-extras-20261106 (the donor catalogue has no tests)
/**
 * Reference oracle of the relocated graph members (GraphAlgorithms) and string
 * members (StringAlgorithms) against brute force on small instances:
 * Bellman-Ford over BigInteger, exhaustive 2-colouring, Prim per component, sorted suffix lists, quadratic windows.
 */
class GraphStringReferenceTest {
    private static final long SEED = 20261106L;
    private static final int GRAPH_CASES = 20_000;
    private static final int STRING_CASES = 20_000;

    private static int[][] randomEdges(SplittableRandom random, int n, int maxEdges) {
        int m = n == 0 ? 0 : random.nextInt(0, maxEdges + 1);
        int[] from = new int[m];
        int[] to = new int[m];
        for (int i = 0; i < m; i++) {
            from[i] = random.nextInt(n);
            to[i] = random.nextInt(n);
        }
        return new int[][] {from, to};
    }

    /** Distances by |V|-1 rounds of exact relaxation; null when a negative cycle is reachable. */
    private static BigInteger[] bellmanFord(int n, int[] from, int[] to, long[] weight, int source) {
        BigInteger[] distance = new BigInteger[n];
        distance[source] = BigInteger.ZERO;
        for (int round = 0; round < n; round++) {
            boolean changed = false;
            for (int e = 0; e < from.length; e++) {
                if (distance[from[e]] == null) {
                    continue;
                }
                BigInteger candidate = distance[from[e]].add(BigInteger.valueOf(weight[e]));
                if (distance[to[e]] == null || candidate.compareTo(distance[to[e]]) < 0) {
                    distance[to[e]] = candidate;
                    changed = true;
                    if (round == n - 1) {
                        return null;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return distance;
    }

    private static long[] expectedDistances(BigInteger[] exact) {
        long[] out = new long[exact.length];
        for (int v = 0; v < exact.length; v++) {
            out[v] = exact[v] == null ? Long.MAX_VALUE : exact[v].longValueExact();
        }
        return out;
    }

    @Test
    void shortestPathsMatchExactBellmanFord() {
        SplittableRandom random = new SplittableRandom(SEED);
        for (int i = 0; i < GRAPH_CASES; i++) {
            int n = random.nextInt(1, 13);
            int[][] e = randomEdges(random, n, 30);
            long[] weights = new long[e[0].length];
            for (int k = 0; k < weights.length; k++) {
                weights[k] = random.nextInt(2);
            }
            int source = random.nextInt(n);
            GraphAlgorithms.WeightedCsrGraph graph = random.nextBoolean()
                    ? GraphAlgorithms.WeightedCsrGraph.directed(n, e[0], e[1], weights)
                    : GraphAlgorithms.WeightedCsrGraph.undirected(n, e[0], e[1], weights);
            int[] offsets = graph.offsets();
            int[] targets = graph.edges();
            long[] csrWeights = graph.weights();
            int[] from = new int[targets.length];
            for (int v = 0; v < n; v++) {
                for (int k = offsets[v]; k < offsets[v + 1]; k++) {
                    from[k] = v;
                }
            }
            assertArrayEquals(expectedDistances(bellmanFord(n, from, targets, csrWeights, source)),
                    GraphAlgorithms.zeroOneBfs(graph, source), "case " + i);
        }
        report("graph", "zeroOneBfs", GRAPH_CASES, GRAPH_CASES);
        long negativeCycles = 0;
        for (int i = 0; i < GRAPH_CASES; i++) {
            int n = random.nextInt(1, 9);
            int[][] e = randomEdges(random, n, 16);
            long[] weights = new long[e[0].length];
            for (int k = 0; k < weights.length; k++) {
                weights[k] = random.nextInt(-20, 61);
            }
            int source = random.nextInt(n);
            BigInteger[] exact = bellmanFord(n, e[0], e[1], weights, source);
            GraphAlgorithms.EdgeList list = new GraphAlgorithms.EdgeList(n, e[0], e[1], weights);
            if (exact == null) {
                negativeCycles++;
                assertThrows(IllegalArgumentException.class, () -> GraphAlgorithms.bellmanFord(list,
                        source), "case " + i);
            } else {
                assertArrayEquals(expectedDistances(exact), GraphAlgorithms.bellmanFord(list, source),
                        "case " + i);
            }
        }
        report("graph", "bellmanFord", GRAPH_CASES, GRAPH_CASES);
        System.out.println("REFERENCE-DETAIL bellmanFord reachable_negative_cycles=" + negativeCycles);
    }

    private static int components(int n, int[] left, int[] right) {
        int[] label = new int[n];
        Arrays.fill(label, -1);
        int count = 0;
        for (int start = 0; start < n; start++) {
            if (label[start] >= 0) {
                continue;
            }
            label[start] = count;
            boolean grew = true;
            while (grew) {
                grew = false;
                for (int k = 0; k < left.length; k++) {
                    if (label[left[k]] == count && label[right[k]] < 0) {
                        label[right[k]] = count;
                        grew = true;
                    } else if (label[right[k]] == count && label[left[k]] < 0) {
                        label[left[k]] = count;
                        grew = true;
                    }
                }
            }
            count++;
        }
        return count;
    }

    /** Minimum spanning forest weight by Prim from every unvisited vertex (exact, signed weights, multi-edges). */
    private static long primForest(int n, int[] left, int[] right, long[] weight) {
        boolean[] inTree = new boolean[n];
        long total = 0;
        for (int start = 0; start < n; start++) {
            if (inTree[start]) {
                continue;
            }
            inTree[start] = true;
            while (true) {
                int bestEdge = -1;
                for (int k = 0; k < left.length; k++) {
                    if (inTree[left[k]] != inTree[right[k]] && (bestEdge < 0 || weight[k] < weight[bestEdge])) {
                        bestEdge = k;
                    }
                }
                if (bestEdge < 0) {
                    break;
                }
                inTree[left[bestEdge]] = true;
                inTree[right[bestEdge]] = true;
                total += weight[bestEdge];
            }
        }
        return total;
    }

    @Test
    void unionFindFamilyMatchesBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED + 1);
        for (int i = 0; i < GRAPH_CASES; i++) {
            int n = random.nextInt(0, 12);
            int[][] e = randomEdges(random, n, 20);
            long[] weights = new long[e[0].length];
            for (int k = 0; k < weights.length; k++) {
                weights[k] = random.nextInt(-50, 51);
            }
            int c = components(n, e[0], e[1]);
            assertEquals(c, GraphAlgorithms.unionFindComponents(n, e[0], e[1]), "case " + i);
            assertEquals(c, GraphAlgorithms.connectedComponents(
                    GraphAlgorithms.CsrGraph.undirected(n, e[0], e[1])), "case " + i);
            assertEquals(e[0].length > n - c, GraphAlgorithms.hasUndirectedCycle(n, e[0], e[1]));
            GraphAlgorithms.MstResult mst = GraphAlgorithms.kruskalMst(n, e[0], e[1], weights);
            assertEquals(primForest(n, e[0], e[1], weights), mst.totalWeight(), "case " + i);
            assertEquals(n - c, mst.edgesUsed());
            assertEquals(n <= 1 || c == 1, mst.connected());
        }
        report("graph", "unionFindComponents+connectedComponents+hasUndirectedCycle+kruskalMst", 4L * GRAPH_CASES,
                4L * GRAPH_CASES);
    }

    @Test
    void bipartiteAndDfsMatchExhaustiveSearch() {
        SplittableRandom random = new SplittableRandom(SEED + 2);
        for (int i = 0; i < GRAPH_CASES; i++) {
            int n = random.nextInt(0, 11);
            int[][] e = randomEdges(random, n, 14);
            boolean expected = false;
            for (int mask = 0; mask < 1 << n && !expected; mask++) {
                boolean ok = true;
                for (int k = 0; k < e[0].length && ok; k++) {
                    ok = ((mask >> e[0][k]) & 1) != ((mask >> e[1][k]) & 1);
                }
                expected = ok;
            }
            assertEquals(expected || n == 0, GraphAlgorithms.isBipartite(
                    GraphAlgorithms.CsrGraph.undirected(n, e[0], e[1])), "case " + i);
        }
        report("graph", "isBipartite", GRAPH_CASES, GRAPH_CASES);
        long canonicalDifferences = 0;
        for (int i = 0; i < GRAPH_CASES; i++) {
            int n = random.nextInt(1, 12);
            int[][] e = randomEdges(random, n, 24);
            GraphAlgorithms.CsrGraph graph = GraphAlgorithms.CsrGraph.directed(n, e[0], e[1]);
            int source = random.nextInt(n);
            int[] order = GraphAlgorithms.dfsPreorder(graph, source);
            List<Integer> recursive = new ArrayList<>();
            dfs(graph.offsets(), graph.edges(), source, new boolean[n], recursive);
            assertEquals(source, order[0]);
            List<Integer> got = new ArrayList<>();
            for (int v : order) {
                got.add(v);
            }
            List<Integer> sortedGot = new ArrayList<>(got);
            List<Integer> sortedReach = new ArrayList<>(recursive);
            Collections.sort(sortedGot);
            Collections.sort(sortedReach);
            assertEquals(sortedReach, sortedGot, "visits each reachable vertex once, case " + i);
            canonicalDifferences += got.equals(recursive) ? 0 : 1;
        }
        report("graph", "dfsPreorder", GRAPH_CASES, GRAPH_CASES);
        finding("dfsPreorder", "marks a vertex when it is pushed, not when it is visited, so the order differs from the "
                + "recursive DFS preorder (CSR neighbour order) in " + canonicalDifferences + " of " + GRAPH_CASES
                + " cases (reachable set and source-first hold in all)");
    }

    private static void dfs(int[] offsets, int[] targets, int vertex, boolean[] seen, List<Integer> out) {
        seen[vertex] = true;
        out.add(vertex);
        for (int k = offsets[vertex]; k < offsets[vertex + 1]; k++) {
            if (!seen[targets[k]]) {
                dfs(offsets, targets, targets[k], seen, out);
            }
        }
    }

    // ---------------------------------------------------------------- strings

    private static String text(SplittableRandom random, int length, String alphabet) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return new String(chars);
    }

    @Test
    void palindromeAndSuffixMembersMatchBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED + 3);
        for (int i = 0; i < STRING_CASES; i++) {
            String value = text(random, random.nextInt(0, 40), i % 2 == 0 ? "ab" : "abc😀");
            int n = value.length();
            int[] odd = StringAlgorithms.manacherOdd(value);
            int[] even = StringAlgorithms.manacherEven(value);
            for (int c = 0; c < n; c++) {
                int k = 1;
                while (c - k >= 0 && c + k < n && value.charAt(c - k) == value.charAt(c + k)) {
                    k++;
                }
                assertEquals(k, odd[c], value + " odd " + c);
                int j = 0;
                while (c - j - 1 >= 0 && c + j < n && value.charAt(c - j - 1) == value.charAt(c + j)) {
                    j++;
                }
                assertEquals(j, even[c], value + " even " + c);
            }
            Integer[] suffixes = new Integer[n];
            for (int s = 0; s < n; s++) {
                suffixes[s] = s;
            }
            Arrays.sort(suffixes, (x, y) -> value.substring(x).compareTo(value.substring(y)));
            int[] order = StringAlgorithms.suffixArray(value);
            int[] expectedOrder = Arrays.stream(suffixes).mapToInt(Integer::intValue).toArray();
            assertArrayEquals(expectedOrder, order, value);
            int[] lcp = StringAlgorithms.lcpArray(value, order);
            int best = 0;
            for (int s = 0; s + 1 < n; s++) {
                int l = 0;
                while (order[s] + l < n && order[s + 1] + l < n
                        && value.charAt(order[s] + l) == value.charAt(order[s + 1] + l)) {
                    l++;
                }
                assertEquals(l, lcp[s], value + " lcp " + s);
                best = Math.max(best, l);
            }
            String repeated = StringAlgorithms.longestRepeatedSubstring(value);
            assertEquals(best, repeated.length(), value);
            assertTrue(repeated.isEmpty() || value.indexOf(repeated) != value.lastIndexOf(repeated), value);
        }
        report("string", "manacherOdd+manacherEven", 2L * STRING_CASES, 2L * STRING_CASES);
        report("string", "suffixArray+lcpArray+longestRepeatedSubstring", 3L * STRING_CASES, 3L * STRING_CASES);
    }

    @Test
    void searchAndWindowMembersMatchBruteForce() {
        SplittableRandom random = new SplittableRandom(SEED + 4);
        for (int i = 0; i < STRING_CASES; i++) {
            String haystack = text(random, random.nextInt(0, 60), "abc");
            String pattern = random.nextInt(3) == 0 && haystack.length() > 1
                    ? haystack.substring(random.nextInt(haystack.length() / 2), haystack.length() / 2 + 1)
                    : text(random, random.nextInt(0, 5), "abc");
            int expected = haystack.indexOf(pattern);
            assertEquals(expected, StringAlgorithms.kmpIndexOf(haystack, pattern), haystack + "/" + pattern);
            assertEquals(expected, StringAlgorithms.kmpIndexOf(haystack, pattern,
                    ProgressMonitor.none()));
            assertEquals(expected, StringAlgorithms.rabinKarpIndexOf(haystack, pattern));
            int[] prefix = StringAlgorithms.prefixFunction(pattern);
            for (int q = 0; q < pattern.length(); q++) {
                int border = 0;
                for (int l = q; l > 0 && border == 0; l--) {
                    border = pattern.substring(0, l).equals(pattern.substring(q + 1 - l, q + 1)) ? l : 0;
                }
                assertEquals(border, prefix[q], pattern + " " + q);
            }
            String required = text(random, random.nextInt(0, 4), "abc");
            assertEquals(minimumWindow(haystack, required), StringAlgorithms.minimumWindow(haystack,
                    required), haystack + "/" + required);
            assertEquals(minimumWindow(haystack, required), StringAlgorithms.minimumWindow(haystack,
                    required, ProgressMonitor.none()));
        }
        report("string", "kmpIndexOf(x2)+rabinKarpIndexOf+prefixFunction+minimumWindow(x2)", 6L * STRING_CASES,
                6L * STRING_CASES);
        for (int i = 0; i < STRING_CASES; i++) {
            String left = text(random, random.nextInt(0, 12), "aAb1 ,éÉ");
            String right = random.nextBoolean() ? new StringBuilder(left).reverse().toString()
                    : text(random, left.length(), "aAb1 ,éÉ");
            char[] l = left.toCharArray();
            char[] r = right.toCharArray();
            Arrays.sort(l);
            Arrays.sort(r);
            assertEquals(Arrays.equals(l, r), StringAlgorithms.isAnagram(left, right));
            StringBuilder kept = new StringBuilder();
            for (char ch : left.toCharArray()) {
                if (Character.isLetterOrDigit(ch)) {
                    kept.append(Character.toLowerCase(ch));
                }
            }
            assertEquals(kept.toString().contentEquals(new StringBuilder(kept).reverse()),
                    StringAlgorithms.isPalindromeIgnoringNonAlphanumeric(left), left);
            String[] words = left.trim().isEmpty() ? new String[0] : left.trim().split(" +");
            Collections.reverse(Arrays.asList(words));
            assertEquals(String.join(" ", words), StringAlgorithms.reverseWords(left), left);
            String[] group = {left, left + right, left.isEmpty() ? right : left.substring(0, left.length() - 1)};
            int common = group[0].length();
            for (String other : group) {
                int k = 0;
                while (k < Math.min(common, other.length()) && other.charAt(k) == group[0].charAt(k)) {
                    k++;
                }
                common = k;
            }
            assertEquals(group[0].substring(0, common), StringAlgorithms.longestCommonPrefix(group));
        }
        report("string", "isAnagram+isPalindromeIgnoringNonAlphanumeric+reverseWords+longestCommonPrefix",
                4L * STRING_CASES, 4L * STRING_CASES);
    }

    /** The shortest, then leftmost, window of {@code source} holding the multiset of {@code required} (ASCII). */
    private static String minimumWindow(String source, String required) {
        if (required.isEmpty()) {
            return "";
        }
        int[] need = new int[128];
        for (char ch : required.toCharArray()) {
            need[ch]++;
        }
        for (int length = required.length(); length <= source.length(); length++) {
            for (int start = 0; start + length <= source.length(); start++) {
                int[] have = new int[128];
                for (int k = start; k < start + length; k++) {
                    have[source.charAt(k)]++;
                }
                boolean covers = true;
                for (char ch : required.toCharArray()) {
                    covers &= have[ch] >= need[ch];
                }
                if (covers) {
                    return source.substring(start, start + length);
                }
            }
        }
        return "";
    }
}
