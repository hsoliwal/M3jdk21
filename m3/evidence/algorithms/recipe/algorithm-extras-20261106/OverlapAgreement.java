// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import DONOR_PACKAGE.AdvancedGraphAlgorithms;
import DONOR_PACKAGE.AdvancedStringAlgorithms;
import DONOR_PACKAGE.AlgorithmCatalog;
import DONOR_PACKAGE.AlgorithmShape;
import DONOR_PACKAGE.BacktrackingAlgorithms;
import DONOR_PACKAGE.BitAlgorithms;
import DONOR_PACKAGE.DynamicProgrammingAlgorithms;
import DONOR_PACKAGE.GraphAlgorithms;
import DONOR_PACKAGE.IntAlgorithms;
import DONOR_PACKAGE.LinkedListAlgorithms;
import DONOR_PACKAGE.MatrixAlgorithms;
import DONOR_PACKAGE.PrimitiveTrie;
import DONOR_PACKAGE.RangeQueryStructures;
import DONOR_PACKAGE.StringAlgorithms;
import DONOR_PACKAGE.TreeAlgorithms;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import jdk.m3.algorithm.M3AlgorithmShapeCatalog;
import jdk.m3.algorithm.M3ArrayAlgorithms;
import jdk.m3.algorithm.M3BacktrackingAlgorithms;
import jdk.m3.algorithm.M3BitAlgorithms;
import jdk.m3.algorithm.M3DynamicProgrammingAlgorithms;
import jdk.m3.algorithm.M3GraphAlgorithms;
import jdk.m3.algorithm.M3HeapAlgorithms;
import jdk.m3.algorithm.M3IntervalGreedyAlgorithms;
import jdk.m3.algorithm.M3ListAlgorithms;
import jdk.m3.algorithm.M3MatrixAlgorithms;
import jdk.m3.algorithm.M3ProgressMonitor;
import jdk.m3.algorithm.M3StackAlgorithms;
import jdk.m3.algorithm.M3StringAlgorithms;
import jdk.m3.algorithm.M3StringTrie;
import jdk.m3.algorithm.M3TreeAlgorithms;
import jdk.m3.collections.range.LongFenwickTree;
import jdk.m3.tools.cpu.M3ArrayGraph;
import jdk.m3.tools.cpu.M3BipartiteMatching;
import jdk.m3.tools.cpu.M3KnapsackSelection;
import jdk.m3.tools.cpu.M3SuffixArray;

//FLLDCIM: NEC - overlap agreement of recipe/algorithm-extras-20261106 (overlap.sh); evidence for the DROP rows, never a port; a template: overlap.sh replaces DONOR_PACKAGE in the imports by the donor package of RELOCATION_MAP.tsv, so no donor package name is spelled here (jdk-naming fixed point)
/**
 * Overlap agreement driver (gate 4): every donor member the census marks OVERLAP_LANDED or OVERLAP_OTHER_OWNER runs
 * next to its owner on the same seeded fixtures in one JVM (the donor package of the PACKAGE row, owners
 * jdk.m3.algorithm, jdk.m3.collections, jdk.m3.tools.cpu and java.base jdk.internal.m3). Results are mapped to one
 * representation (int[] vs List, CSR vs Map, array tree vs Node) and compared: AGREE when equal (or both throw the same
 * exception class), EQUIVALENT when they differ but both are valid answers of the problem (ties in top-k, palindromes,
 * topological orders), DISAGREE otherwise. Prints one TSV row per donor member with the first disagreement.
 */
public final class OverlapAgreement {
    static final long SEED = 20261106L;
    private static final int CASES = 2_000;
    private static final M3ProgressMonitor NONE = M3ProgressMonitor.none();

    private record Outcome(String donor, String owner, boolean equivalent) {
    }

    private static final StringBuilder OUT = new StringBuilder();

    public static void main(String[] args) {
        OUT.append("donor_member\towner\tcases\tagree\tequivalent\tdisagree\tfirst_difference\n");
        arrays();
        graphsAndStrings();
        dynamicProgrammingAndBacktracking();
        listsTreesMatrices();
        otherOwners();
        catalog();
        System.out.print(OUT);
    }

    private static String run(Supplier<Object> body) {
        try {
            return show(body.get());
        } catch (RuntimeException | StackOverflowError e) {
            return "EXC " + e.getClass().getSimpleName();
        }
    }

    private static String show(Object value) {
        if (value instanceof int[] a) {
            return Arrays.toString(a);
        }
        if (value instanceof long[] a) {
            return Arrays.toString(a);
        }
        if (value instanceof int[][] a) {
            return Arrays.deepToString(a);
        }
        return String.valueOf(value);
    }

    private static void compare(String member, String owner, int cases, Function<SplittableRandom, Outcome> body) {
        SplittableRandom random = new SplittableRandom(SEED ^ member.hashCode());
        int agree = 0;
        int equivalent = 0;
        int disagree = 0;
        String first = "-";
        for (int i = 0; i < cases; i++) {
            Outcome outcome = body.apply(random);
            if (outcome.donor().equals(outcome.owner())) {
                agree++;
            } else if (outcome.equivalent()) {
                equivalent++;
            } else {
                disagree++;
                if (first.equals("-")) {
                    first = ("case " + i + " donor=" + outcome.donor() + " owner=" + outcome.owner());
                    first = first.length() > 300 ? first.substring(0, 300) + "..." : first;
                }
            }
        }
        OUT.append(member).append('\t').append(owner).append('\t').append(cases).append('\t').append(agree)
                .append('\t').append(equivalent).append('\t').append(disagree).append('\t')
                .append(first.replace('\t', ' ')).append('\n');
    }

    private static Outcome same(String donor, String owner) {
        return new Outcome(donor, owner, false);
    }

    private static int[] ints(SplittableRandom random, int n, int bound) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = random.nextInt(12) == 0 ? (random.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                    : random.nextInt(-bound, bound + 1);
        }
        return values;
    }

    private static int[] small(SplittableRandom random, int n, int bound) {
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = random.nextInt(-bound, bound + 1);
        }
        return values;
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    private static List<Integer> list(int[] values) {
        List<Integer> out = new ArrayList<>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }

    private static String canonicalRows(int[][] rows) {
        List<String> out = new ArrayList<>();
        for (int[] row : rows) {
            out.add(Arrays.toString(row));
        }
        out.sort(null);
        return out.toString();
    }

    private static String canonicalLists(List<List<Integer>> rows) {
        List<String> out = new ArrayList<>();
        for (List<Integer> row : rows) {
            out.add(row.toString());
        }
        out.sort(null);
        return out.toString();
    }

    // ---------------------------------------------------------------- IntAlgorithms vs the landed array families

    private static void arrays() {
        compare("IntAlgorithms.lowerBound(int[],int)+(..,M3ProgressMonitor)", "M3ArrayAlgorithms.lowerBound", CASES,
                r -> {
                    int[] a = sorted(ints(r, r.nextInt(0, 30), 20));
                    int t = r.nextInt(5) == 0 ? (r.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                            : r.nextInt(-25, 26);
                    return same(run(() -> IntAlgorithms.lowerBound(a, t)) + "/"
                            + run(() -> IntAlgorithms.lowerBound(a, t, null)),
                            run(() -> M3ArrayAlgorithms.lowerBound(a, t, NONE)) + "/"
                            + run(() -> M3ArrayAlgorithms.lowerBound(a, t, NONE)));
                });
        compare("IntAlgorithms.upperBound(int[],int)+(..,M3ProgressMonitor)", "M3ArrayAlgorithms.upperBound", CASES,
                r -> {
                    int[] a = sorted(ints(r, r.nextInt(0, 30), 20));
                    int t = r.nextInt(5) == 0 ? (r.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                            : r.nextInt(-25, 26);
                    return same(run(() -> IntAlgorithms.upperBound(a, t)) + "/"
                            + run(() -> IntAlgorithms.upperBound(a, t, null)),
                            run(() -> M3ArrayAlgorithms.upperBound(a, t, NONE)) + "/"
                            + run(() -> M3ArrayAlgorithms.upperBound(a, t, NONE)));
                });
        compare("IntAlgorithms.twoSumIndices(int[],int)+(..,M3ProgressMonitor)", "M3ArrayAlgorithms.twoSum", CASES,
                r -> {
                    int[] a = ints(r, r.nextInt(0, 12), 20);
                    int t = r.nextInt(4) == 0 ? (r.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE)
                            : r.nextInt(-40, 41);
                    return same(run(() -> IntAlgorithms.twoSumIndices(a, t)) + "/"
                            + run(() -> IntAlgorithms.twoSumIndices(a, t, null)),
                            run(() -> M3ArrayAlgorithms.twoSum(a, t, NONE)) + "/"
                            + run(() -> M3ArrayAlgorithms.twoSum(a, t, NONE)));
                });
        compare("IntAlgorithms.sortedTwoSumIndices(int[],int)", "M3ArrayAlgorithms.sortedTwoSum", CASES, r -> {
            int[] a = sorted(ints(r, r.nextInt(0, 12), 20));
            int t = r.nextInt(-40, 41);
            return same(run(() -> IntAlgorithms.sortedTwoSumIndices(a, t)),
                    run(() -> M3ArrayAlgorithms.sortedTwoSum(a, t, NONE)));
        });
        compare("IntAlgorithms.slidingWindowMaximum(int[],int)+(..,M3ProgressMonitor)",
                "M3ArrayAlgorithms.maxSlidingWindow", CASES, r -> {
                    int[] a = ints(r, r.nextInt(0, 20), 20);
                    int w = r.nextInt(-1, 22);
                    return same(run(() -> IntAlgorithms.slidingWindowMaximum(a, w)) + "/"
                            + run(() -> IntAlgorithms.slidingWindowMaximum(a, w, null)),
                            run(() -> M3ArrayAlgorithms.maxSlidingWindow(a, w, NONE)) + "/"
                            + run(() -> M3ArrayAlgorithms.maxSlidingWindow(a, w, NONE)));
                });
        compare("IntAlgorithms.dailyTemperatures(int[])", "M3StackAlgorithms.dailyTemperatures", CASES, r -> {
            int[] a = ints(r, r.nextInt(0, 20), 20);
            return same(run(() -> IntAlgorithms.dailyTemperatures(a)),
                    run(() -> M3StackAlgorithms.dailyTemperatures(a, NONE)));
        });
        compare("IntAlgorithms.prefixSums(int[])", "M3ArrayAlgorithms.prefixSums", CASES, r -> {
            int[] a = ints(r, r.nextInt(0, 20), 1_000);
            return same(run(() -> IntAlgorithms.prefixSums(a)), run(() -> M3ArrayAlgorithms.prefixSums(a, NONE)));
        });
        compare("IntAlgorithms.rangeSum(long[],int,int)", "M3ArrayAlgorithms.rangeSum", CASES, r -> {
            long[] p = M3ArrayAlgorithms.prefixSums(ints(r, r.nextInt(0, 20), 1_000), NONE);
            int a = r.nextInt(-1, p.length + 1);
            int b = r.nextInt(-1, p.length + 1);
            return same(run(() -> IntAlgorithms.rangeSum(p, a, b)), run(() -> M3ArrayAlgorithms.rangeSum(p, a, b,
                    NONE)));
        });
        compare("IntAlgorithms.maxSubarraySum(int[])", "M3ArrayAlgorithms.maxSubarray", CASES, r -> {
            int[] a = ints(r, r.nextInt(0, 20), 1_000);
            return same(run(() -> IntAlgorithms.maxSubarraySum(a)), run(() -> M3ArrayAlgorithms.maxSubarray(a, NONE)));
        });
        compare("IntAlgorithms.productExceptSelf(int[])", "M3ArrayAlgorithms.productExceptSelf", CASES, r -> {
            int[] a = r.nextInt(5) == 0 ? ints(r, r.nextInt(0, 8), 1_000) : small(r, r.nextInt(0, 10), 9);
            return same(run(() -> IntAlgorithms.productExceptSelf(a)),
                    run(() -> M3ArrayAlgorithms.productExceptSelf(a, NONE)));
        });
        compare("IntAlgorithms.missingNumber(int[])", "M3BitAlgorithms.missingNumber", CASES, r -> {
            int n = r.nextInt(0, 20);
            int[] a = r.ints(0, n + 1).distinct().limit(n).toArray();
            int[] input = r.nextInt(10) == 0 ? ints(r, n, 30) : a;
            return same(run(() -> IntAlgorithms.missingNumber(input)),
                    run(() -> M3BitAlgorithms.missingNumber(input, NONE)));
        });
        compare("IntAlgorithms.searchRotated(int[],int)", "M3ArrayAlgorithms.searchRotated", CASES, r -> {
            int[] base = r.ints(-100, 100).distinct().limit(r.nextInt(0, 20)).sorted().toArray();
            int shift = base.length == 0 ? 0 : r.nextInt(base.length);
            int[] a = new int[base.length];
            for (int i = 0; i < a.length; i++) {
                a[i] = base[(i + shift) % a.length];
            }
            int t = r.nextInt(-110, 110);
            return same(run(() -> IntAlgorithms.searchRotated(a, t)),
                    run(() -> M3ArrayAlgorithms.searchRotated(a, t, NONE)));
        });
        compare("IntAlgorithms.canJump(int[])", "M3IntervalGreedyAlgorithms.canReachEnd", CASES, r -> {
            int[] a = r.nextInt(5) == 0 ? ints(r, r.nextInt(0, 10), 5) : small(r, r.nextInt(0, 12), 3);
            return same(run(() -> IntAlgorithms.canJump(a)), run(() -> M3IntervalGreedyAlgorithms.canReachEnd(a,
                    NONE)));
        });
        compare("IntAlgorithms.minJumps(int[])", "M3IntervalGreedyAlgorithms.minimumJumps", CASES, r -> {
            int[] a = r.nextInt(5) == 0 ? ints(r, r.nextInt(0, 10), 5) : small(r, r.nextInt(0, 12), 3);
            return same(run(() -> IntAlgorithms.minJumps(a)), run(() -> M3IntervalGreedyAlgorithms.minimumJumps(a,
                    NONE)));
        });
        compare("IntAlgorithms.trapRainWater(int[])", "M3ArrayAlgorithms.trapRainWater", CASES, r -> {
            int[] a = r.nextInt(8) == 0 ? ints(r, r.nextInt(0, 10), 50) : small(r, r.nextInt(0, 20), 9);
            return same(run(() -> IntAlgorithms.trapRainWater(a)), run(() -> M3ArrayAlgorithms.trapRainWater(a,
                    NONE)));
        });
        compare("IntAlgorithms.kthLargest(int[],int)", "M3HeapAlgorithms.kthLargest", CASES, r -> {
            int[] a = ints(r, r.nextInt(0, 20), 30);
            int k = r.nextInt(-1, a.length + 2);
            return same(run(() -> IntAlgorithms.kthLargest(a, k)), run(() -> M3HeapAlgorithms.kthLargest(a, k, NONE)));
        });
        compare("IntAlgorithms.topKFrequent(int[],int)", "M3ArrayAlgorithms.topKFrequent", CASES, r -> {
            int[] a = small(r, r.nextInt(0, 25), 4);
            int k = r.nextInt(-1, 6);
            String donor = run(() -> sorted(IntAlgorithms.topKFrequent(a, k)));
            String owner = run(() -> sorted(M3ArrayAlgorithms.topKFrequent(a, k, NONE).stream()
                    .mapToInt(Integer::intValue).toArray()));
            return new Outcome(donor, owner, validTopK(a, k, donor) && validTopK(a, k, owner));
        });
        compare("IntAlgorithms.mergeIntervals(int[][])", "M3IntervalGreedyAlgorithms.merge", CASES, r -> {
            int[][] rows = intervals(r);
            return same(run(() -> IntAlgorithms.mergeIntervals(rows)), run(() -> toRows(
                    M3IntervalGreedyAlgorithms.merge(toIntervals(rows), NONE))));
        });
        compare("IntAlgorithms.insertInterval(int[][],int[])", "M3IntervalGreedyAlgorithms.insert", CASES, r -> {
            int[][] rows = IntAlgorithms.mergeIntervals(Arrays.stream(intervals(r)).filter(x -> x[0] <= x[1])
                    .toArray(int[][]::new));
            int start = r.nextInt(-5, 40);
            int[] add = {start, start + r.nextInt(-2, 10)};
            return same(run(() -> IntAlgorithms.insertInterval(rows, add)), run(() -> toRows(
                    M3IntervalGreedyAlgorithms.insert(toIntervals(rows), new M3IntervalGreedyAlgorithms.Interval(add[0],
                            add[1]), NONE))));
        });
        compare("IntAlgorithms.threeSumZero(int[])", "M3ArrayAlgorithms.threeSum", CASES, r -> {
            int[] a = r.nextInt(8) == 0 ? ints(r, r.nextInt(0, 10), 5) : small(r, r.nextInt(0, 16), 6);
            return same(run(() -> canonicalRows(IntAlgorithms.threeSumZero(a))),
                    run(() -> canonicalLists(M3ArrayAlgorithms.threeSum(a, NONE))));
        });
        compare("IntAlgorithms.sortColors012(int[])", "M3ArrayAlgorithms.sortColors", CASES, r -> {
            int[] a = small(r, r.nextInt(0, 20), 1);
            for (int i = 0; i < a.length; i++) {
                a[i] = r.nextInt(30) == 0 ? 3 : Math.abs(a[i]) + (r.nextBoolean() ? 1 : 0);
            }
            int[] b = a.clone();
            return same(run(() -> {
                IntAlgorithms.sortColors012(a);
                return a;
            }), run(() -> {
                M3ArrayAlgorithms.sortColors(b, NONE);
                return b;
            }));
        });
        compare("IntAlgorithms.singleNumber(int[])", "jdk.internal.m3.M3IntAlgorithms.singleNumberOfPairs (java.base)",
                CASES, r -> {
                    int pairs = r.nextInt(0, 8);
                    int[] a = new int[pairs * 2 + (r.nextInt(6) == 0 ? 0 : 1)];
                    for (int i = 0; i < pairs; i++) {
                        a[2 * i] = r.nextInt();
                        a[2 * i + 1] = a[2 * i];
                    }
                    if ((a.length & 1) == 1) {
                        a[a.length - 1] = r.nextInt();
                    }
                    return same(run(() -> IntAlgorithms.singleNumber(a)), run(() -> singleNumberOfPairs(a)));
                });
    }

    private static Object singleNumberOfPairs(int[] values) {
        try {
            return Class.forName("jdk.internal.m3.M3IntAlgorithms").getMethod("singleNumberOfPairs", int[].class)
                    .invoke(null, (Object) values);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("java.base owner not patched in: " + e);
        }
    }

    private static boolean validTopK(int[] values, int k, String answer) {
        if (answer.startsWith("EXC")) {
            return false;
        }
        Map<Integer, Integer> count = new HashMap<>();
        for (int v : values) {
            count.merge(v, 1, Integer::sum);
        }
        List<Integer> frequencies = new ArrayList<>(count.values());
        frequencies.sort(Comparator.reverseOrder());
        String body = answer.substring(1, answer.length() - 1);
        int[] chosen = body.isEmpty() ? new int[0] : Arrays.stream(body.split(", ")).mapToInt(Integer::parseInt)
                .toArray();
        if (chosen.length != k || k > frequencies.size()) {
            return false;
        }
        int[] got = Arrays.stream(chosen).map(v -> count.getOrDefault(v, 0)).sorted().toArray();
        int[] best = frequencies.subList(0, k).stream().mapToInt(Integer::intValue).sorted().toArray();
        return Arrays.equals(got, best) && Arrays.stream(chosen).distinct().count() == k;
    }

    private static int[][] intervals(SplittableRandom r) {
        int[][] rows = new int[r.nextInt(0, 8)][];
        for (int i = 0; i < rows.length; i++) {
            int start = r.nextInt(0, 30);
            rows[i] = new int[] {start, start + r.nextInt(r.nextInt(15) == 0 ? -2 : 0, 8)};
        }
        return rows;
    }

    private static List<M3IntervalGreedyAlgorithms.Interval> toIntervals(int[][] rows) {
        List<M3IntervalGreedyAlgorithms.Interval> out = new ArrayList<>();
        for (int[] row : rows) {
            out.add(new M3IntervalGreedyAlgorithms.Interval(row[0], row[1]));
        }
        return out;
    }

    private static int[][] toRows(List<M3IntervalGreedyAlgorithms.Interval> intervals) {
        return intervals.stream().map(i -> new int[] {i.start(), i.end()}).toArray(int[][]::new);
    }

    // ---------------------------------------------------------------- graphs and strings

    private static Map<Integer, List<Integer>> adjacency(int n, int[] offsets, int[] targets) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int v = 0; v < n; v++) {
            List<Integer> out = new ArrayList<>();
            for (int k = offsets[v]; k < offsets[v + 1]; k++) {
                out.add(targets[k]);
            }
            map.put(v, out);
        }
        return map;
    }

    private static boolean isTopological(int n, int[] from, int[] to, String order) {
        if (order.startsWith("EXC")) {
            return false;
        }
        String body = order.substring(1, order.length() - 1);
        int[] seq = body.isEmpty() ? new int[0] : Arrays.stream(body.split(", ")).mapToInt(Integer::parseInt).toArray();
        int[] position = new int[n];
        Arrays.fill(position, -1);
        for (int i = 0; i < seq.length; i++) {
            position[seq[i]] = i;
        }
        for (int k = 0; k < from.length; k++) {
            if (position[from[k]] < 0 || position[from[k]] >= position[to[k]]) {
                return false;
            }
        }
        return seq.length == n;
    }

    private static void graphsAndStrings() {
        compare("GraphAlgorithms.bfsDistances(CsrGraph,int)+(..,M3ProgressMonitor)", "M3GraphAlgorithms.shortestPaths",
                CASES, r -> {
                    int n = r.nextInt(1, 15);
                    int[][] e = edges(r, n, 30);
                    GraphAlgorithms.CsrGraph g = GraphAlgorithms.CsrGraph.directed(n, e[0], e[1]);
                    int s = r.nextInt(n);
                    return same(run(() -> IntAlgorithmsView.reachable(GraphAlgorithms.bfsDistances(g, s))) + "/"
                            + run(() -> IntAlgorithmsView.reachable(GraphAlgorithms.bfsDistances(g, s, null))),
                            run(() -> new TreeMap<>(M3GraphAlgorithms.shortestPaths(adjacency(n, g.offsets(),
                                    g.edges()), s, NONE))) + "/" + run(() -> new TreeMap<>(
                                    M3GraphAlgorithms.shortestPaths(adjacency(n, g.offsets(), g.edges()), s, NONE))));
                });
        compare("GraphAlgorithms.topologicalSort(CsrGraph)", "M3GraphAlgorithms.topologicalOrder", CASES, r -> {
            int n = r.nextInt(1, 12);
            int[][] e = edges(r, n, 14);
            if (r.nextInt(4) != 0) {
                for (int k = 0; k < e[0].length; k++) {
                    int a = Math.min(e[0][k], e[1][k]);
                    int b = Math.max(e[0][k], e[1][k]);
                    e[0][k] = a;
                    e[1][k] = b == a ? Math.min(n - 1, b + 1) : b;
                }
            }
            GraphAlgorithms.CsrGraph g = GraphAlgorithms.CsrGraph.directed(n, e[0], e[1]);
            String donor = run(() -> GraphAlgorithms.topologicalSort(g));
            String owner = run(() -> M3GraphAlgorithms.topologicalOrder(adjacency(n, g.offsets(), g.edges()),
                    Comparator.<Integer>naturalOrder(), NONE));
            return new Outcome(donor, owner, isTopological(n, e[0], e[1], donor)
                    && isTopological(n, e[0], e[1], owner));
        });
        compare("GraphAlgorithms.dijkstra(WeightedCsrGraph,int)+(..,M3ProgressMonitor)", "M3GraphAlgorithms.dijkstra",
                CASES, r -> {
                    int n = r.nextInt(1, 12);
                    int[][] e = edges(r, n, 24);
                    long[] w = new long[e[0].length];
                    for (int k = 0; k < w.length; k++) {
                        w[k] = r.nextInt(10) == 0 ? Long.MAX_VALUE - r.nextInt(3) : r.nextInt(8) == 0
                                ? Long.MAX_VALUE / 2 : r.nextLong(0, 100);
                    }
                    GraphAlgorithms.WeightedCsrGraph g = GraphAlgorithms.WeightedCsrGraph.directed(n, e[0], e[1], w);
                    Map<Integer, List<M3GraphAlgorithms.WeightedEdge<Integer>>> map = new HashMap<>();
                    for (int v = 0; v < n; v++) {
                        map.put(v, new ArrayList<>());
                    }
                    for (int k = 0; k < w.length; k++) {
                        map.get(e[0][k]).add(new M3GraphAlgorithms.WeightedEdge<>(e[1][k], w[k]));
                    }
                    int s = r.nextInt(n);
                    return same(run(() -> IntAlgorithmsView.reachable(GraphAlgorithms.dijkstra(g, s))) + "/"
                            + run(() -> IntAlgorithmsView.reachable(GraphAlgorithms.dijkstra(g, s, null))),
                            run(() -> new TreeMap<>(M3GraphAlgorithms.dijkstra(map, s, NONE))) + "/"
                            + run(() -> new TreeMap<>(M3GraphAlgorithms.dijkstra(map, s, NONE))));
                });
        compare("AdvancedGraphAlgorithms.stronglyConnectedComponents(CsrGraph)",
                "M3GraphAlgorithms.stronglyConnectedComponents", CASES, r -> {
                    int n = r.nextInt(0, 14);
                    int[][] e = edges(r, n, 30);
                    GraphAlgorithms.CsrGraph g = GraphAlgorithms.CsrGraph.directed(n, e[0], e[1]);
                    return same(run(() -> partition(AdvancedGraphAlgorithms.stronglyConnectedComponents(g)
                            .componentOf())), run(() -> partitionLists(M3GraphAlgorithms.stronglyConnectedComponents(
                            adjacency(n, g.offsets(), g.edges()), Comparator.<Integer>naturalOrder(), NONE))));
                });
        compare("AdvancedStringAlgorithms.zFunction(String)", "M3StringAlgorithms.zFunction", CASES, r -> {
            String s = text(r, r.nextInt(0, 20), "ab");
            return same(run(() -> AdvancedStringAlgorithms.zFunction(s)), run(() -> M3StringAlgorithms.zFunction(s,
                    NONE)));
        });
        compare("AdvancedStringAlgorithms.longestPalindromeRange(String)", "M3StringAlgorithms.longestPalindrome",
                CASES, r -> {
                    String s = text(r, r.nextInt(0, 20), "abc");
                    String donor = run(() -> {
                        int[] range = AdvancedStringAlgorithms.longestPalindromeRange(s);
                        return s.substring(range[0], range[1]);
                    });
                    String owner = run(() -> M3StringAlgorithms.longestPalindrome(s, NONE));
                    return new Outcome(donor, owner, donor.length() == owner.length() && palindrome(donor)
                            && palindrome(owner) && s.contains(donor) && s.contains(owner));
                });
        compare("StringAlgorithms.longestSubstringWithoutRepeating(String)+(..,M3ProgressMonitor)",
                "M3ArrayAlgorithms.longestDistinctSubstring", CASES, r -> {
                    String s = text(r, r.nextInt(0, 20), r.nextInt(4) == 0 ? "ab😀" : "abcd");
                    return same(run(() -> StringAlgorithms.longestSubstringWithoutRepeating(s)) + "/"
                            + run(() -> StringAlgorithms.longestSubstringWithoutRepeating(s, null)),
                            run(() -> M3ArrayAlgorithms.longestDistinctSubstring(s, NONE)) + "/"
                            + run(() -> M3ArrayAlgorithms.longestDistinctSubstring(s, NONE)));
                });
        compare("StringAlgorithms.prefixFunction(String)", "M3StringAlgorithms.prefixFunction", CASES, r -> {
            String s = text(r, r.nextInt(0, 20), "ab");
            return same(run(() -> StringAlgorithms.prefixFunction(s)), run(() -> M3StringAlgorithms.prefixFunction(s,
                    NONE)));
        });
        compare("StringAlgorithms.longestPalindromicSubstring(String)", "M3StringAlgorithms.longestPalindrome", CASES,
                r -> {
                    String s = text(r, r.nextInt(0, 20), "abc");
                    String donor = run(() -> StringAlgorithms.longestPalindromicSubstring(s));
                    String owner = run(() -> M3StringAlgorithms.longestPalindrome(s, NONE));
                    return new Outcome(donor, owner, donor.length() == owner.length() && palindrome(donor)
                            && palindrome(owner));
                });
        compare("StringAlgorithms.validParentheses(String)", "M3StackAlgorithms.validBrackets", CASES, r -> {
            String s = text(r, r.nextInt(0, 12), r.nextInt(5) == 0 ? "()[]{}a" : "()[]{}");
            return same(run(() -> StringAlgorithms.validParentheses(s)), run(() -> M3StackAlgorithms.validBrackets(s,
                    NONE)));
        });
    }

    /** Distances as a map of the reachable vertices (int[] with -1 or long[] with the MAX sentinel). */
    private static final class IntAlgorithmsView {
        static TreeMap<Integer, Object> reachable(Object distances) {
            TreeMap<Integer, Object> map = new TreeMap<>();
            if (distances instanceof int[] d) {
                for (int v = 0; v < d.length; v++) {
                    if (d[v] >= 0) {
                        map.put(v, d[v]);
                    }
                }
            } else {
                long[] d = (long[]) distances;
                for (int v = 0; v < d.length; v++) {
                    if (d[v] != Long.MAX_VALUE) {
                        map.put(v, d[v]);
                    }
                }
            }
            return map;
        }
    }

    private static int[][] edges(SplittableRandom r, int n, int max) {
        int m = n == 0 ? 0 : r.nextInt(0, max + 1);
        int[] from = new int[m];
        int[] to = new int[m];
        for (int k = 0; k < m; k++) {
            from[k] = r.nextInt(n);
            to[k] = r.nextInt(n);
        }
        return new int[][] {from, to};
    }

    private static String partition(int[] componentOf) {
        Map<Integer, List<Integer>> groups = new TreeMap<>();
        for (int v = 0; v < componentOf.length; v++) {
            groups.computeIfAbsent(componentOf[v], k -> new ArrayList<>()).add(v);
        }
        List<String> out = new ArrayList<>();
        for (List<Integer> group : groups.values()) {
            out.add(group.toString());
        }
        out.sort(null);
        return out.toString();
    }

    private static String partitionLists(List<List<Integer>> components) {
        List<String> out = new ArrayList<>();
        for (List<Integer> component : components) {
            List<Integer> copy = new ArrayList<>(component);
            copy.sort(null);
            out.add(copy.toString());
        }
        out.sort(null);
        return out.toString();
    }

    private static String text(SplittableRandom r, int n, String alphabet) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            out.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return out.toString();
    }

    private static boolean palindrome(String s) {
        return !s.startsWith("EXC") && new StringBuilder(s).reverse().toString().equals(s);
    }

    // ---------------------------------------------------------------- dynamic programming, backtracking, bits

    private static void dynamicProgrammingAndBacktracking() {
        compare("DynamicProgrammingAlgorithms.houseRobber(int[])", "M3DynamicProgrammingAlgorithms.houseRobber", CASES,
                r -> {
                    int[] a = r.nextInt(6) == 0 ? ints(r, r.nextInt(0, 8), 50) : small(r, r.nextInt(0, 15), 50);
                    return same(run(() -> DynamicProgrammingAlgorithms.houseRobber(a)),
                            run(() -> M3DynamicProgrammingAlgorithms.houseRobber(a, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.coinChangeMin(int[],int)", "M3DynamicProgrammingAlgorithms.coinChange",
                CASES, r -> {
                    int[] coins = small(r, r.nextInt(0, 5), 12);
                    int amount = r.nextInt(-1, 60);
                    return same(run(() -> DynamicProgrammingAlgorithms.coinChangeMin(coins, amount)),
                            run(() -> M3DynamicProgrammingAlgorithms.coinChange(coins, amount, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.knapsack01(int[],long[],int)", "M3DynamicProgrammingAlgorithms"
                + ".knapsack01(int[],int[],int,M3ProgressMonitor)", CASES, r -> {
                    int n = r.nextInt(0, 10);
                    int[] weights = small(r, n, 10);
                    int[] values = small(r, n, 100);
                    long[] longValues = Arrays.stream(values).asLongStream().toArray();
                    int capacity = r.nextInt(-1, 40);
                    return same(run(() -> DynamicProgrammingAlgorithms.knapsack01(weights, longValues, capacity)),
                            run(() -> M3DynamicProgrammingAlgorithms.knapsack01(weights, values, capacity, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.knapsack01(int[],long[],int)", "jdk.m3.tools.cpu.M3KnapsackSelection"
                + ".maximize(int[],long[],int).value (cpu-tools, same erased signature)", CASES, r -> {
                    int n = r.nextInt(0, 10);
                    int[] weights = small(r, n, 10);
                    long[] values = Arrays.stream(small(r, n, 100)).asLongStream().toArray();
                    int capacity = r.nextInt(-1, 40);
                    return same(run(() -> DynamicProgrammingAlgorithms.knapsack01(weights, values, capacity)),
                            run(() -> M3KnapsackSelection.maximize(weights, values, capacity).value()));
                });
        compare("DynamicProgrammingAlgorithms.longestIncreasingSubsequenceLength(int[])",
                "M3DynamicProgrammingAlgorithms.longestIncreasingSubsequence", CASES, r -> {
                    int[] a = ints(r, r.nextInt(0, 20), 10);
                    return same(run(() -> DynamicProgrammingAlgorithms.longestIncreasingSubsequenceLength(a)),
                            run(() -> M3DynamicProgrammingAlgorithms.longestIncreasingSubsequence(a, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.longestCommonSubsequenceLength(String,String)",
                "M3DynamicProgrammingAlgorithms.longestCommonSubsequence", CASES, r -> {
                    String a = text(r, r.nextInt(0, 15), "abc");
                    String b = text(r, r.nextInt(0, 15), "abc");
                    return same(run(() -> DynamicProgrammingAlgorithms.longestCommonSubsequenceLength(a, b)),
                            run(() -> M3DynamicProgrammingAlgorithms.longestCommonSubsequence(a, b, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.editDistance(String,String)",
                "M3DynamicProgrammingAlgorithms.editDistance", CASES, r -> {
                    String a = text(r, r.nextInt(0, 12), "abc");
                    String b = text(r, r.nextInt(0, 12), "abc");
                    return same(run(() -> DynamicProgrammingAlgorithms.editDistance(a, b)),
                            run(() -> M3DynamicProgrammingAlgorithms.editDistance(a, b, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.uniquePaths(int,int)", "M3DynamicProgrammingAlgorithms.uniquePaths",
                CASES, r -> {
                    int rows = r.nextInt(-1, 40);
                    int columns = r.nextInt(-1, 40);
                    return same(run(() -> DynamicProgrammingAlgorithms.uniquePaths(rows, columns)),
                            run(() -> M3DynamicProgrammingAlgorithms.uniquePaths(rows, columns, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.canPartitionEqualSubset(int[])",
                "M3DynamicProgrammingAlgorithms.canPartition", CASES, r -> {
                    int[] a = small(r, r.nextInt(0, 12), 20);
                    for (int i = 0; i < a.length; i++) {
                        a[i] = r.nextInt(15) == 0 ? a[i] : Math.abs(a[i]);
                    }
                    return same(run(() -> DynamicProgrammingAlgorithms.canPartitionEqualSubset(a)),
                            run(() -> M3DynamicProgrammingAlgorithms.canPartition(a, NONE)));
                });
        compare("DynamicProgrammingAlgorithms.wordBreak(String,String[])", "M3DynamicProgrammingAlgorithms.wordBreak",
                CASES, r -> {
                    String s = text(r, r.nextInt(0, 14), "ab");
                    String[] words = new String[r.nextInt(0, 5)];
                    for (int i = 0; i < words.length; i++) {
                        words[i] = text(r, r.nextInt(0, 4), "ab");
                    }
                    return same(run(() -> DynamicProgrammingAlgorithms.wordBreak(s, words)),
                            run(() -> M3DynamicProgrammingAlgorithms.wordBreak(s, Arrays.asList(words), NONE)));
                });
        compare("BacktrackingAlgorithms.permutations(int[])", "M3BacktrackingAlgorithms.permutations", CASES, r -> {
            int[] a = small(r, r.nextInt(0, 6), r.nextBoolean() ? 2 : 50);
            return same(run(() -> canonicalRows(BacktrackingAlgorithms.permutations(a))),
                    run(() -> canonicalLists(M3BacktrackingAlgorithms.permutations(a, NONE))));
        });
        compare("BacktrackingAlgorithms.subsets(int[])", "M3BacktrackingAlgorithms.subsets", CASES, r -> {
            int[] a = small(r, r.nextInt(0, 8), r.nextBoolean() ? 2 : 50);
            return same(run(() -> canonicalRows(BacktrackingAlgorithms.subsets(a))),
                    run(() -> canonicalLists(M3BacktrackingAlgorithms.subsets(a, NONE))));
        });
        compare("BacktrackingAlgorithms.nQueensSolutions(int,int,M3ProgressMonitor)", "M3BacktrackingAlgorithms.nQueens",
                12, r -> {
                    int n = r.nextInt(0, 9);
                    String donor = run(() -> {
                        List<String> boards = new ArrayList<>();
                        for (int[] columns : BacktrackingAlgorithms.nQueensSolutions(n, Integer.MAX_VALUE, null)) {
                            List<String> rows = new ArrayList<>();
                            for (int column : columns) {
                                char[] row = new char[n];
                                Arrays.fill(row, '.');
                                row[column] = 'Q';
                                rows.add(new String(row));
                            }
                            boards.add(rows.toString());
                        }
                        boards.sort(null);
                        return n + ":" + boards;
                    });
                    String owner = run(() -> {
                        List<String> boards = new ArrayList<>();
                        for (List<String> rows : M3BacktrackingAlgorithms.nQueens(n, NONE)) {
                            boards.add(rows.toString());
                        }
                        boards.sort(null);
                        return n + ":" + boards;
                    });
                    return same(donor, owner);
                });
        compare("BitAlgorithms.hammingWeight(int)+reverseBits(int)", "M3BitAlgorithms.hammingWeight+reverseBits", CASES,
                r -> {
                    int v = r.nextInt();
                    return same(BitAlgorithms.hammingWeight(v) + "/" + BitAlgorithms.reverseBits(v),
                            M3BitAlgorithms.hammingWeight(v, NONE) + "/" + M3BitAlgorithms.reverseBits(v, NONE));
                });
    }

    // ---------------------------------------------------------------- lists, trees, matrices, the trie

    private static M3ListAlgorithms.Node<Integer>[] nodes(int[] values, int[] next) {
        @SuppressWarnings("unchecked")
        M3ListAlgorithms.Node<Integer>[] nodes = (M3ListAlgorithms.Node<Integer>[]) new M3ListAlgorithms.Node<?>[
                values.length];
        for (int i = 0; i < values.length; i++) {
            nodes[i] = new M3ListAlgorithms.Node<>(values[i]);
        }
        for (int i = 0; i < values.length; i++) {
            nodes[i].next(next[i] < 0 ? null : nodes[next[i]]);
        }
        return nodes;
    }

    private static String walk(M3ListAlgorithms.Node<Integer> head) {
        List<Integer> out = new ArrayList<>();
        for (M3ListAlgorithms.Node<Integer> node = head; node != null && out.size() < 1_000; node = node.next()) {
            out.add(node.value());
        }
        return out.toString();
    }

    private static String walk(LinkedListAlgorithms.IntLinkedList list) {
        return list(LinkedListAlgorithms.toArray(list)).toString();
    }

    private static M3TreeAlgorithms.Node<Integer> tree(int[] values, int[] left, int[] right, int node) {
        if (node < 0) {
            return null;
        }
        M3TreeAlgorithms.Node<Integer> out = new M3TreeAlgorithms.Node<>(values[node]);
        out.left(tree(values, left, right, left[node]));
        out.right(tree(values, left, right, right[node]));
        return out;
    }

    private static void listsTreesMatrices() {
        compare("LinkedListAlgorithms.cycleStartIndex(IntLinkedList)", "M3ListAlgorithms.cycleStart", CASES, r -> {
            int n = r.nextInt(0, 12);
            int[] values = small(r, n, 9);
            int[] next = new int[n];
            for (int i = 0; i < n; i++) {
                next[i] = r.nextInt(3) == 0 ? r.nextInt(-1, n) : i + 1 < n ? i + 1 : -1;
            }
            int head = n == 0 ? -1 : r.nextInt(n);
            M3ListAlgorithms.Node<Integer>[] nodes = nodes(values, next);
            IdentityHashMap<Object, Integer> index = new IdentityHashMap<>();
            for (int i = 0; i < n; i++) {
                index.put(nodes[i], i);
            }
            return same(run(() -> LinkedListAlgorithms.cycleStartIndex(new LinkedListAlgorithms.IntLinkedList(values,
                    next, head))), run(() -> {
                        M3ListAlgorithms.Node<Integer> start = M3ListAlgorithms.cycleStart(head < 0 ? null
                                : nodes[head], NONE);
                        return start == null ? -1 : index.get(start);
                    }));
        });
        compare("LinkedListAlgorithms.middleIndex+reversed+removeNthFromEnd(IntLinkedList..)",
                "M3ListAlgorithms.middle+reverse+removeNthFromEnd", CASES, r -> {
                    int[] values = small(r, r.nextInt(0, 12), 9);
                    int nth = r.nextInt(0, values.length + 2);
                    LinkedListAlgorithms.IntLinkedList list = LinkedListAlgorithms.IntLinkedList.linear(values);
                    int[] linear = new int[values.length];
                    for (int i = 0; i < linear.length; i++) {
                        linear[i] = i + 1 < linear.length ? i + 1 : -1;
                    }
                    String donor = run(() -> {
                        int middle = LinkedListAlgorithms.middleIndex(list);
                        return (middle < 0 ? "null" : values[middle]) + "/" + walk(LinkedListAlgorithms.reversed(list))
                                + "/" + run(() -> walk(LinkedListAlgorithms.removeNthFromEnd(list, nth)));
                    });
                    String owner = run(() -> {
                        M3ListAlgorithms.Node<Integer> head = values.length == 0 ? null : nodes(values, linear)[0];
                        M3ListAlgorithms.Node<Integer> middle = M3ListAlgorithms.middle(head, NONE);
                        String reversed = walk(M3ListAlgorithms.reverse(values.length == 0 ? null
                                : nodes(values, linear)[0], NONE));
                        return (middle == null ? "null" : middle.value()) + "/" + reversed + "/" + run(() -> walk(
                                M3ListAlgorithms.removeNthFromEnd(values.length == 0 ? null : nodes(values, linear)[0],
                                        nth, NONE)));
                    });
                    return same(donor, owner);
                });
        compare("LinkedListAlgorithms.mergeSorted(IntLinkedList,IntLinkedList)", "M3ListAlgorithms.mergeSorted", CASES,
                r -> {
                    int[] a = sorted(small(r, r.nextInt(0, 8), 20));
                    int[] b = sorted(small(r, r.nextInt(0, 8), 20));
                    int[] la = new int[a.length];
                    int[] lb = new int[b.length];
                    for (int i = 0; i < la.length; i++) {
                        la[i] = i + 1 < la.length ? i + 1 : -1;
                    }
                    for (int i = 0; i < lb.length; i++) {
                        lb[i] = i + 1 < lb.length ? i + 1 : -1;
                    }
                    return same(run(() -> walk(LinkedListAlgorithms.mergeSorted(
                            LinkedListAlgorithms.IntLinkedList.linear(a), LinkedListAlgorithms.IntLinkedList.linear(b)))),
                            run(() -> walk(M3ListAlgorithms.mergeSorted(a.length == 0 ? null : nodes(a, la)[0],
                                    b.length == 0 ? null : nodes(b, lb)[0], Comparator.<Integer>naturalOrder(),
                                    NONE))));
                });
        compare("TreeAlgorithms.inorder+levelOrder+maxDepth+isBalanced+isValidBst+kthSmallestBst(BinaryTree..)",
                "M3TreeAlgorithms.inorder+levelOrder+maxDepth+isBalanced+isValidBst+kthSmallest", CASES, r -> {
                    int[][] t = randomTree(r, r.nextInt(0, 20), r.nextBoolean());
                    int k = r.nextInt(0, t[0].length + 2);
                    TreeAlgorithms.BinaryTree donorTree = new TreeAlgorithms.BinaryTree(t[0], t[1], t[2], t[3][0]);
                    M3TreeAlgorithms.Node<Integer> root = tree(t[0], t[1], t[2], t[3][0]);
                    String donor = run(() -> list(TreeAlgorithms.inorder(donorTree)))
                            + "/" + run(() -> list(TreeAlgorithms.levelOrder(donorTree)))
                            + "/" + run(() -> TreeAlgorithms.maxDepth(donorTree))
                            + "/" + run(() -> TreeAlgorithms.isBalanced(donorTree))
                            + "/" + run(() -> TreeAlgorithms.isValidBst(donorTree))
                            + "/" + run(() -> TreeAlgorithms.kthSmallestBst(donorTree, k));
                    String owner = run(() -> M3TreeAlgorithms.inorder(root, NONE))
                            + "/" + run(() -> M3TreeAlgorithms.levelOrder(root, NONE).stream().flatMap(List::stream)
                            .toList())
                            + "/" + run(() -> M3TreeAlgorithms.maxDepth(root, NONE))
                            + "/" + run(() -> M3TreeAlgorithms.isBalanced(root, NONE))
                            + "/" + run(() -> M3TreeAlgorithms.isValidBst(root, Comparator.<Integer>naturalOrder(),
                            NONE))
                            + "/" + run(() -> M3TreeAlgorithms.kthSmallest(root, k, NONE));
                    return same(donor, owner);
                });
        compare("MatrixAlgorithms.countIslands(int[][])", "M3MatrixAlgorithms.countIslands(char[][],..)", CASES, r -> {
            int[][] grid = grid(r, 2);
            char[][] chars = new char[grid.length][];
            for (int i = 0; i < grid.length; i++) {
                chars[i] = new char[grid[i].length];
                for (int j = 0; j < grid[i].length; j++) {
                    chars[i][j] = grid[i][j] != 0 ? '1' : '0';
                }
            }
            return same(run(() -> MatrixAlgorithms.countIslands(grid)), run(() -> M3MatrixAlgorithms.countIslands(chars,
                    NONE)));
        });
        compare("MatrixAlgorithms.rotateSquareClockwise+spiralOrder+setZeroes(int[][])",
                "M3MatrixAlgorithms.rotateClockwise+spiralOrder+setZeroes", CASES, r -> {
                    int[][] grid = grid(r, 3);
                    int[][] a = deep(grid);
                    int[][] b = deep(grid);
                    int[][] c = deep(grid);
                    int[][] d = deep(grid);
                    String donor = run(() -> {
                        MatrixAlgorithms.rotateSquareClockwise(a);
                        return a;
                    }) + "/" + run(() -> list(MatrixAlgorithms.spiralOrder(grid))) + "/" + run(() -> {
                        MatrixAlgorithms.setZeroes(c);
                        return c;
                    });
                    String owner = run(() -> {
                        M3MatrixAlgorithms.rotateClockwise(b, NONE);
                        return b;
                    }) + "/" + run(() -> M3MatrixAlgorithms.spiralOrder(grid, NONE)) + "/" + run(() -> {
                        M3MatrixAlgorithms.setZeroes(d, NONE);
                        return d;
                    });
                    return same(donor, owner);
                });
        compare("MatrixAlgorithms.searchSortedMatrix(int[][],int)", "M3MatrixAlgorithms.searchSorted", CASES, r -> {
            int rows = r.nextInt(1, 6);
            int columns = r.nextInt(1, 6);
            int[][] grid = new int[rows][columns];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    grid[i][j] = (i > 0 ? grid[i - 1][j] : 0) + (j > 0 ? grid[i][j - 1] : 0) / 2 + r.nextInt(0, 4);
                    grid[i][j] = Math.max(grid[i][j], Math.max(i > 0 ? grid[i - 1][j] : 0, j > 0 ? grid[i][j - 1] : 0));
                }
            }
            int target = r.nextInt(0, 40);
            return same(run(() -> MatrixAlgorithms.searchSortedMatrix(grid, target)),
                    run(() -> M3MatrixAlgorithms.searchSorted(grid, target, NONE)));
        });
        compare("PrimitiveTrie.add+contains+hasPrefix(String) (kept: structure unit)",
                "M3StringTrie.insert+contains+startsWith", CASES, r -> {
                    PrimitiveTrie primitive = new PrimitiveTrie();
                    M3StringTrie trie = new M3StringTrie();
                    StringBuilder donor = new StringBuilder();
                    StringBuilder owner = new StringBuilder();
                    for (int op = 0; op < 20; op++) {
                        String word = text(r, r.nextInt(0, 5), r.nextInt(4) == 0 ? "a😀" : "abé");
                        if (r.nextBoolean()) {
                            primitive.add(word);
                            trie.insert(word, NONE);
                        }
                        String probe = r.nextInt(5) == 0 && !word.isEmpty() ? word.substring(0, word.length() - 1)
                                : word;
                        donor.append(primitive.contains(probe)).append(primitive.hasPrefix(probe)).append(' ');
                        owner.append(trie.contains(probe, NONE)).append(trie.startsWith(probe, NONE)).append(' ');
                    }
                    return same(donor.toString(), owner.toString());
                });
    }

    private static int[][] deep(int[][] grid) {
        return Arrays.stream(grid).map(int[]::clone).toArray(int[][]::new);
    }

    private static int[][] grid(SplittableRandom r, int bound) {
        int rows = r.nextInt(1, 7);
        int columns = r.nextInt(5) == 0 ? rows : r.nextInt(1, 7);
        int[][] grid = new int[rows][columns];
        for (int[] row : grid) {
            for (int j = 0; j < columns; j++) {
                row[j] = r.nextInt(bound);
            }
        }
        return grid;
    }

    private static int[][] randomTree(SplittableRandom r, int n, boolean bst) {
        int[] values = bst ? r.ints(-50, 50).distinct().limit(n).toArray() : small(r, n, 9);
        int[] left = new int[n];
        int[] right = new int[n];
        Arrays.fill(left, -1);
        Arrays.fill(right, -1);
        for (int i = 1; i < n; i++) {
            int at = 0;
            while (true) {
                boolean goLeft = bst ? values[i] < values[at] : r.nextBoolean();
                int[] side = goLeft ? left : right;
                if (side[at] < 0) {
                    side[at] = i;
                    break;
                }
                at = side[at];
            }
        }
        return new int[][] {values, left, right, {n == 0 ? -1 : 0}};
    }

    // ---------------------------------------------------------------- other owners

    private static void otherOwners() {
        compare("RangeQueryStructures.FenwickTree (DROP_TYPE)", "jdk.m3.collections.range.LongFenwickTree", CASES, r -> {
            int n = r.nextInt(0, 20);
            long[] values = new long[n];
            for (int i = 0; i < n; i++) {
                values[i] = r.nextInt(10) == 0 ? (r.nextBoolean() ? Long.MAX_VALUE : Long.MIN_VALUE)
                        : r.nextLong(-1_000, 1_000);
            }
            boolean fromValues = r.nextBoolean();
            StringBuilder donor = new StringBuilder();
            StringBuilder owner = new StringBuilder();
            RangeQueryStructures.FenwickTree[] d = new RangeQueryStructures.FenwickTree[1];
            LongFenwickTree[] o = new LongFenwickTree[1];
            donor.append(run(() -> (d[0] = fromValues ? new RangeQueryStructures.FenwickTree(values)
                    : new RangeQueryStructures.FenwickTree(n)).size()));
            owner.append(run(() -> (o[0] = fromValues ? new LongFenwickTree(values) : new LongFenwickTree(n))
                    .size()));
            for (int op = 0; op < 15 && d[0] != null && o[0] != null; op++) {
                int a = r.nextInt(-1, n + 1);
                int b = r.nextInt(-1, n + 1);
                long delta = r.nextInt(8) == 0 ? Long.MAX_VALUE : r.nextLong(-1_000, 1_000);
                int kind = r.nextInt(3);
                donor.append(' ').append(run(() -> kind == 0 ? fenwickAdd(d[0], a, delta) : kind == 1
                        ? d[0].prefixSum(a) : d[0].rangeSum(a, b)));
                owner.append(' ').append(run(() -> kind == 0 ? fenwickAdd(o[0], a, delta) : kind == 1
                        ? o[0].prefixSum(a) : o[0].rangeSum(a, b)));
            }
            return same(donor.toString(), owner.toString());
        });
        compare("AdvancedGraphAlgorithms.dinicMaxFlow(int,int[],int[],long[],int,int)",
                "jdk.m3.tools.cpu.M3ArrayGraph.maximumFlow", CASES, r -> {
                    int n = r.nextInt(0, 12);
                    int[][] e = edges(r, n, 30);
                    long[] capacity = new long[e[0].length];
                    for (int k = 0; k < capacity.length; k++) {
                        capacity[k] = r.nextInt(15) == 0 ? -1 : r.nextLong(0, 50);
                    }
                    int s = n == 0 ? 0 : r.nextInt(n);
                    int t = n == 0 ? 0 : r.nextInt(n);
                    return same(run(() -> AdvancedGraphAlgorithms.dinicMaxFlow(n, e[0], e[1], capacity, s, t)),
                            run(() -> M3ArrayGraph.maximumFlow(n, e[0], e[1], capacity, s, t)));
                });
        compare("AdvancedGraphAlgorithms.hopcroftKarp(int,int,int[],int[])",
                "jdk.m3.tools.cpu.M3BipartiteMatching.maximum (CSR)", CASES, r -> {
                    int left = r.nextInt(0, 9);
                    int right = r.nextInt(0, 9);
                    int m = left == 0 || right == 0 ? 0 : r.nextInt(0, 20);
                    int[] el = new int[m];
                    int[] er = new int[m];
                    for (int k = 0; k < m; k++) {
                        el[k] = r.nextInt(left);
                        er[k] = r.nextInt(right);
                    }
                    int[] offsets = new int[left + 1];
                    for (int v : el) {
                        offsets[v + 1]++;
                    }
                    for (int i = 1; i <= left; i++) {
                        offsets[i] += offsets[i - 1];
                    }
                    int[] cursor = offsets.clone();
                    int[] targets = new int[m];
                    for (int k = 0; k < m; k++) {
                        targets[cursor[el[k]]++] = er[k];
                    }
                    String donor = run(() -> AdvancedGraphAlgorithms.hopcroftKarp(left, right, el, er).size());
                    String owner = run(() -> M3BipartiteMatching.maximum(left, right, offsets, targets).size());
                    return same(donor, owner);
                });
        compare("AdvancedStringAlgorithms.suffixArray+lcpArray(String) (kept: closure)",
                "jdk.m3.tools.cpu.M3SuffixArray.suffixOrder+longestCommonPrefixes", CASES, r -> {
                    String s = text(r, r.nextInt(0, 30), r.nextBoolean() ? "ab" : "abc😀");
                    String donor = run(() -> {
                        int[] order = AdvancedStringAlgorithms.suffixArray(s);
                        return Arrays.toString(order) + Arrays.toString(AdvancedStringAlgorithms.lcpArray(s, order));
                    });
                    String owner = run(() -> {
                        M3SuffixArray index = M3SuffixArray.index(s);
                        int[] lcp = index.longestCommonPrefixes();
                        return Arrays.toString(index.suffixOrder()) + Arrays.toString(lcp.length == 0 ? lcp
                                : Arrays.copyOfRange(lcp, 1, lcp.length));
                    });
                    return same(donor, owner);
                });
    }

    private static String fenwickAdd(Object tree, int index, long delta) {
        if (tree instanceof RangeQueryStructures.FenwickTree donor) {
            donor.add(index, delta);
        } else {
            ((LongFenwickTree) tree).add(index, delta);
        }
        return "ok";
    }

    // ---------------------------------------------------------------- catalog

    private static void catalog() {
        Set<String> families = new HashSet<>();
        for (M3AlgorithmShapeCatalog.Family family : M3AlgorithmShapeCatalog.Family.values()) {
            families.add(family.name());
        }
        Set<String> shapes = new HashSet<>();
        for (AlgorithmShape shape : AlgorithmShape.values()) {
            shapes.add(shape.name());
        }
        Set<String> common = new HashSet<>(shapes);
        common.retainAll(families);
        Set<String> described = new HashSet<>();
        AlgorithmCatalog.descriptors().forEach(d -> described.add(d.shape().name()));
        OUT.append("AlgorithmCatalog.descriptors()+AlgorithmShape.values()\tM3AlgorithmShapeCatalog.shapes()+Family"
                + ".values()\t1\t0\t0\t1\tdonor descriptors=" + AlgorithmCatalog.descriptors().size()
                + " covering shapes=" + described.size() + " of " + shapes.size() + "; landed shapes="
                + M3AlgorithmShapeCatalog.shapes().size() + " in families=" + families.size()
                + "; enum names in common=" + new java.util.TreeSet<>(common) + " (plan oracle (e): counts differ, "
                + "not ported)\n");
    }
}
