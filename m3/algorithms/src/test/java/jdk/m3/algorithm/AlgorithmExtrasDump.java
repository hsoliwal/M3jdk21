// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.algorithm;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.SplittableRandom;
import java.util.function.Function;

//FLLDCIM: DIFFERENTIAL - driver of recipe/algorithm-extras-20261106/differential.sh; one source, two compiles (the donor variant is this file with the TYPE rows of RELOCATION_MAP.tsv reversed)
/**
 * Dump driver of the algorithm-extras differential gate (not a test): replays seeded inputs through every member the
 * recipe relocated (ABSORB rows and their closure in METHOD_CENSUS.tsv) and prints one line per case: method, case
 * number, a digest of the input and the result or the exception (class and message). Monitor overloads run with a
 * monitor that cancels after a seeded number of ticks and print the progress trace. Input sizes are bounded by
 * construction (largest: 4,096-element arrays, 2,000-vertex graphs, 9x9 Sudoku boards, n-queens up to 10), so a run
 * ends in seconds.
 *
 * <p>differential.sh compiles this file against target/classes (relocated names) and, after reversing the TYPE rows
 * and the package line, against the donor classes; the two dumps must be byte-identical.
 *
 * <p>usage: {@code M3AlgorithmExtrasDump [--seed S] [--cases N]} (N cases per member, default 2,000).
 */
public final class AlgorithmExtrasDump {
    /** The seed of the dump; differential.sh passes it explicitly. */
    static final long SEED = 20261106L;
    private static final int DEFAULT_CASES = 2_000;
    private static final int LONG_TEXT = 240;
    private static final int[] INT_EXTREMES = {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0, 1,
        Integer.MAX_VALUE - 1, Integer.MAX_VALUE};
    private static final long[] LONG_EXTREMES = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L, 0L, 1L,
        Long.MAX_VALUE - 1, Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE};
    private static final char[] TEXT_ALPHABET = {'a', 'b', 'c', 'a', 'b', 'A', ' ', '1', ',', 'é', '\ud83d',
        '\ude00', '(', ')', '[', ']'};

    private final PrintWriter out;
    private final long seed;
    private final int cases;
    private long lines;
    private static long thrown;

    private AlgorithmExtrasDump(PrintWriter out, long seed, int cases) {
        this.out = out;
        this.seed = seed;
        this.cases = cases;
    }

    public static void main(String[] args) {
        long seed = SEED;
        int cases = DEFAULT_CASES;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--cases" -> cases = Integer.parseInt(args[++i]);
                default -> throw new IllegalArgumentException("unknown argument " + args[i]);
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), 1 << 20));
        AlgorithmExtrasDump dump = new AlgorithmExtrasDump(out, seed, cases);
        dump.arrays();
        dump.sorting();
        dump.rangeQueries();
        dump.streaming();
        dump.numeric();
        dump.graphs();
        dump.strings();
        dump.lists();
        dump.trie();
        dump.trees();
        dump.dynamicProgramming();
        dump.backtracking();
        dump.matrices();
        dump.bits();
        out.print("TOTAL seed=" + seed + " cases_per_member=" + cases + " lines=" + dump.lines + " throws="
                + thrown + "\n");
        out.flush();
    }

    // ---------------------------------------------------------------- framework

    private SplittableRandom random(String name) {
        return new SplittableRandom(seed ^ ((long) name.hashCode() << 32) ^ name.length());
    }

    private void each(String name, int count, Function<SplittableRandom, String> body) {
        SplittableRandom random = random(name);
        for (int i = 0; i < count; i++) {
            String line;
            try {
                line = body.apply(random);
            } catch (RuntimeException | StackOverflowError e) {
                thrown++;
                line = "EXC " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
            out.print(name + "#" + i + " " + shorten(line) + "\n");
            lines++;
        }
    }

    /** The case's input is described before the call so that a throwing call still names its input. */
    private static String call(String input, java.util.function.Supplier<String> result) {
        try {
            return input + " -> " + result.get();
        } catch (RuntimeException | StackOverflowError e) {
            thrown++;
            return input + " -> EXC " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    static String shorten(String text) {
        if (text.length() <= LONG_TEXT) {
            return text;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return "len=" + text.length() + " sha256=" + HexFormat.of().formatHex(digest, 0, 16) + " head="
                    + text.substring(0, 80);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int[] ints(SplittableRandom random, int length, int bound) {
        int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            int pick = random.nextInt(16);
            values[i] = pick == 0 ? INT_EXTREMES[random.nextInt(INT_EXTREMES.length)]
                    : bound <= 0 ? random.nextInt() : random.nextInt(-bound, bound + 1);
        }
        return values;
    }

    private static int length(SplittableRandom random) {
        int tier = random.nextInt(100);
        return tier < 70 ? random.nextInt(9) : tier < 97 ? random.nextInt(64) : random.nextInt(4_097);
    }

    private static int[] sorted(int[] values) {
        int[] copy = values.clone();
        Arrays.sort(copy);
        return copy;
    }

    private static String text(SplittableRandom random, int length, int alphabet) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = TEXT_ALPHABET[random.nextInt(Math.min(alphabet, TEXT_ALPHABET.length))];
        }
        return new String(chars);
    }

    private static String s(int[] values) {
        return values == null ? "null" : Arrays.toString(values);
    }

    private static String s(long[] values) {
        return values == null ? "null" : Arrays.toString(values);
    }

    private static String s(int[][] values) {
        return values == null ? "null" : Arrays.deepToString(values);
    }

    private static String s(char[][] values) {
        StringBuilder text = new StringBuilder();
        for (char[] row : values) {
            text.append(row).append('/');
        }
        return text.toString();
    }

    /** A monitor that records its trace and cancels after {@code cancelAfter} worked ticks (never when negative). */
    private static final class TraceMonitor implements ProgressMonitor {
        private final int cancelAfter;
        private final StringBuilder trace = new StringBuilder();
        private int worked;
        private boolean cancelled;

        TraceMonitor(int cancelAfter) {
            this.cancelAfter = cancelAfter;
        }

        @Override public void beginTask(String name, int totalWork) {
            trace.append("begin(").append(name).append(',').append(totalWork).append(')');
        }

        @Override public void worked(int work) {
            worked += work;
            if (cancelAfter >= 0 && worked >= cancelAfter) {
                cancelled = true;
            }
        }

        @Override public boolean isCanceled() {
            return cancelled;
        }

        @Override public void done() {
            trace.append("done");
        }

        String trace() {
            return trace + " worked=" + worked + " cancelled=" + cancelled;
        }
    }

    private static TraceMonitor monitor(SplittableRandom random) {
        return new TraceMonitor(random.nextInt(4) == 0 ? random.nextInt(8) : -1);
    }

    // ---------------------------------------------------------------- arrays (M3PrimitiveArrayAlgorithms)

    private void arrays() {
        each("binarySearch", cases, random -> {
            int[] values = sorted(ints(random, length(random), random.nextBoolean() ? 50 : 0));
            int target = values.length > 0 && random.nextBoolean() ? values[random.nextInt(values.length)]
                    : random.nextInt(-60, 61);
            return call(s(values) + " t=" + target, () -> "" + ArrayAlgorithms.binarySearch(values, target));
        });
        each("binarySearchMonitor", cases, random -> {
            int[] values = sorted(ints(random, length(random), 50));
            int target = random.nextInt(-60, 61);
            TraceMonitor monitor = monitor(random);
            ProgressMonitor passed = random.nextInt(10) == 0 ? null : monitor;
            return call(s(values) + " t=" + target, () -> ArrayAlgorithms.binarySearch(values, target,
                    passed) + "") + " trace=" + monitor.trace();
        });
        each("nextGreaterCircular", cases, random -> {
            int[] values = ints(random, length(random), random.nextBoolean() ? 5 : 0);
            return call(s(values), () -> s(ArrayAlgorithms.nextGreaterCircular(values)));
        });
        each("maxProductSubarray", cases, random -> {
            int[] values = ints(random, length(random), random.nextInt(4) == 0 ? 0 : 9);
            return call(s(values), () -> "" + ArrayAlgorithms.maxProductSubarray(values));
        });
        each("maxProfitOneTransaction", cases, random -> {
            int[] values = ints(random, length(random), random.nextBoolean() ? 100 : 0);
            return call(s(values), () -> "" + ArrayAlgorithms.maxProfitOneTransaction(values));
        });
        each("majorityElement", cases, random -> {
            int[] values = ints(random, length(random), 3);
            if (values.length > 0 && random.nextBoolean()) {
                int major = values[0];
                for (int i = 0; i < values.length; i++) {
                    values[i] = random.nextInt(3) != 0 ? major : values[i];
                }
            }
            return call(s(values), () -> "" + ArrayAlgorithms.majorityElement(values));
        });
        each("countBits", cases, random -> {
            int n = random.nextInt(10) == 0 ? random.nextInt(-3, 0) : random.nextInt(300);
            return call("n=" + n, () -> s(ArrayAlgorithms.countBits(n)));
        });
        each("findMinRotated", cases, random -> {
            int[] values = rotatedDistinct(random);
            return call(s(values), () -> "" + ArrayAlgorithms.findMinRotated(values));
        });
        each("findDuplicateFloyd", cases, random -> {
            int n = random.nextInt(1, 40);
            int[] values = new int[n + 1];
            for (int i = 0; i < values.length; i++) {
                values[i] = random.nextInt(20) == 0 ? random.nextInt(-1, n + 2) : random.nextInt(1, n + 1);
            }
            return call(s(values), () -> "" + ArrayAlgorithms.findDuplicateFloyd(values));
        });
        each("containerMostWater", cases, random -> {
            int[] values = ints(random, length(random), random.nextBoolean() ? 1_000 : 0);
            return call(s(values), () -> "" + ArrayAlgorithms.containerMostWater(values));
        });
        each("moveZeroes", cases, random -> {
            int[] values = ints(random, length(random), 2);
            return call(s(values), () -> {
                ArrayAlgorithms.moveZeroes(values);
                return s(values);
            });
        });
        each("rotateRight", cases, random -> {
            int[] values = ints(random, length(random), 100);
            int steps = random.nextInt(8) == 0 ? INT_EXTREMES[random.nextInt(INT_EXTREMES.length)]
                    : random.nextInt(-20, 21);
            return call(s(values) + " k=" + steps, () -> {
                ArrayAlgorithms.rotateRight(values, steps);
                return s(values);
            });
        });
    }

    private static int[] rotatedDistinct(SplittableRandom random) {
        int n = random.nextInt(20) == 0 ? 0 : random.nextInt(1, 40);
        int[] base = random.ints(0, 1_000).distinct().limit(n).sorted().toArray();
        int shift = n == 0 ? 0 : random.nextInt(n);
        int[] rotated = new int[n];
        for (int i = 0; i < n; i++) {
            rotated[i] = base[(i + shift) % n];
        }
        return rotated;
    }

    // ---------------------------------------------------------------- sorting (M3SortingAlgorithms)

    private void sorting() {
        String[] names = {"mergeSort", "heapSort", "radixSortSigned"};
        for (String name : names) {
            each(name, cases, random -> {
                int[] values = ints(random, length(random), random.nextBoolean() ? 20 : 0);
                return call(s(values), () -> {
                    switch (name) {
                        case "mergeSort" -> SortingAlgorithms.mergeSort(values);
                        case "heapSort" -> SortingAlgorithms.heapSort(values);
                        default -> SortingAlgorithms.radixSortSigned(values);
                    }
                    return s(values);
                });
            });
        }
        each("countingSort", cases, random -> {
            int[] values = ints(random, length(random), 30);
            int low = random.nextInt(8) == 0 ? Integer.MIN_VALUE : random.nextInt(-40, 5);
            int high = random.nextInt(8) == 0 ? Integer.MAX_VALUE : random.nextInt(-5, 40);
            return call(s(values) + " [" + low + "," + high + "]", () -> {
                SortingAlgorithms.countingSort(values, low, high);
                return s(values);
            });
        });
    }

    // ---------------------------------------------------------------- range queries (M3RangeQueryStructures)

    private void rangeQueries() {
        each("SegmentTreeSum", cases, random -> {
            long[] values = new long[random.nextInt(0, 40)];
            for (int i = 0; i < values.length; i++) {
                values[i] = random.nextInt(10) == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                        : random.nextLong(-1_000, 1_001);
            }
            StringBuilder trace = new StringBuilder(s(values));
            trace.append(call(" new", () -> {
                RangeQueryStructures.SegmentTreeSum tree = new RangeQueryStructures.SegmentTreeSum(values);
                StringBuilder ops = new StringBuilder("size=" + tree.size());
                for (int op = 0; op < 12; op++) {
                    int a = random.nextInt(-1, values.length + 2);
                    int b = random.nextInt(-1, values.length + 2);
                    long v = random.nextInt(6) == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                            : random.nextLong(-1_000, 1_001);
                    int kind = random.nextInt(3);
                    ops.append(call(" " + kind + ":" + a + "," + b + "," + v, () -> switch (kind) {
                        case 0 -> "" + tree.get(a);
                        case 1 -> {
                            tree.set(a, v);
                            yield "set";
                        }
                        default -> "" + tree.sum(a, b);
                    }));
                }
                return ops.toString();
            }));
            return trace.toString();
        });
        each("SparseTableMin", cases, random -> {
            int[] values = ints(random, random.nextInt(10) == 0 ? 0 : random.nextInt(1, 70), 1_000);
            return call(s(values), () -> {
                RangeQueryStructures.SparseTableMin table = new RangeQueryStructures.SparseTableMin(values);
                StringBuilder ops = new StringBuilder("size=" + table.size());
                for (int op = 0; op < 12; op++) {
                    int a = random.nextInt(-1, values.length + 1);
                    int b = random.nextInt(-1, values.length + 2);
                    ops.append(call(" " + a + "," + b, () -> "" + table.min(a, b)));
                }
                return ops.toString();
            });
        });
        each("RangeAdder", cases, random -> {
            int size = random.nextInt(-1, 30);
            return call("size=" + size, () -> {
                RangeQueryStructures.RangeAdder adder = new RangeQueryStructures.RangeAdder(size);
                StringBuilder ops = new StringBuilder("size=" + adder.size());
                for (int op = 0; op < 10; op++) {
                    int a = random.nextInt(-1, size + 2);
                    int b = random.nextInt(-1, size + 2);
                    long v = random.nextInt(8) == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                            : random.nextLong(-50, 51);
                    ops.append(call(" " + a + "," + b + "," + v, () -> {
                        adder.add(a, b, v);
                        return "ok";
                    }));
                }
                return ops + call(" materialize", () -> s(adder.materialize()));
            });
        });
    }

    // ---------------------------------------------------------------- streaming (M3StreamingStructures)

    private void streaming() {
        each("IntMinStack", cases, random -> {
            StreamingStructures.IntMinStack stack = new StreamingStructures.IntMinStack();
            StringBuilder ops = new StringBuilder();
            int count = random.nextInt(1, 60);
            for (int op = 0; op < count; op++) {
                int kind = random.nextInt(5);
                int value = ints(random, 1, 50)[0];
                ops.append(call(" " + kind + ":" + value, () -> switch (kind) {
                    case 0, 1 -> {
                        stack.push(value);
                        yield "push";
                    }
                    case 2 -> "" + stack.pop();
                    case 3 -> "" + stack.peek();
                    default -> stack.min() + "/" + stack.size() + "/" + stack.isEmpty();
                }));
            }
            return ops.toString();
        });
        each("MedianTracker", cases, random -> {
            StreamingStructures.MedianTracker tracker = new StreamingStructures.MedianTracker();
            StringBuilder ops = new StringBuilder(call("empty", () -> "" + tracker.median()));
            int count = random.nextInt(1, 60);
            for (int op = 0; op < count; op++) {
                int value = ints(random, 1, 100)[0];
                tracker.add(value);
                ops.append(' ').append(value).append(':').append(tracker.median()).append('/')
                        .append(tracker.size()).append('/').append(tracker.isEmpty());
            }
            return ops.toString();
        });
        each("MovingAverage", cases, random -> {
            int window = random.nextInt(-1, 9);
            return call("w=" + window, () -> {
                StreamingStructures.MovingAverage average = new StreamingStructures.MovingAverage(window);
                StringBuilder ops = new StringBuilder("window=" + average.window());
                int count = random.nextInt(1, 40);
                for (int op = 0; op < count; op++) {
                    long value = random.nextInt(12) == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                            : random.nextLong(-100, 101);
                    ops.append(call(" " + value, () -> average.add(value) + "/" + average.size()));
                }
                return ops.toString();
            });
        });
        each("KthLargestStream", cases, random -> {
            int k = random.nextInt(-1, 8);
            int[] initial = random.nextInt(8) == 0 ? null : ints(random, random.nextInt(10), 30);
            boolean seeded = random.nextBoolean();
            return call("k=" + k + " init=" + s(initial) + " seeded=" + seeded, () -> {
                StreamingStructures.KthLargestStream stream = seeded
                        ? new StreamingStructures.KthLargestStream(k, initial)
                        : new StreamingStructures.KthLargestStream(k);
                StringBuilder ops = new StringBuilder();
                int count = random.nextInt(1, 30);
                for (int op = 0; op < count; op++) {
                    int value = ints(random, 1, 30)[0];
                    stream.add(value);
                    ops.append(call(" " + value, () -> stream.observed() + "/" + stream.ready() + "/"
                            + stream.kthLargest()));
                }
                return ops.toString();
            });
        });
    }

    // ---------------------------------------------------------------- numeric (M3NumericAlgorithms)

    private static long anyLong(SplittableRandom random) {
        int tier = random.nextInt(10);
        return tier == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                : tier < 4 ? random.nextLong() : random.nextLong(-10_000, 10_001);
    }

    private void numeric() {
        each("gcd", cases, random -> {
            long a = anyLong(random);
            long b = anyLong(random);
            return call(a + "," + b, () -> "" + NumericAlgorithms.gcd(a, b));
        });
        each("lcm", cases, random -> {
            long a = anyLong(random);
            long b = anyLong(random);
            return call(a + "," + b, () -> "" + NumericAlgorithms.lcm(a, b));
        });
        each("modPow", cases, random -> {
            long base = anyLong(random);
            long exponent = random.nextInt(10) == 0 ? -1 : Math.abs(anyLong(random) % 1_000_000_007L);
            long modulus = random.nextInt(10) == 0 ? random.nextLong(-2, 2) : Math.max(1, Math.abs(anyLong(random)));
            return call(base + "^" + exponent + "%" + modulus, () -> "" + NumericAlgorithms.modPow(base, exponent,
                    modulus));
        });
        each("modInverse", cases, random -> {
            long value = anyLong(random);
            long modulus = random.nextInt(10) == 0 ? random.nextLong(-2, 3) : random.nextLong(2, Long.MAX_VALUE);
            return call(value + "%" + modulus, () -> "" + NumericAlgorithms.modInverse(value, modulus));
        });
        each("isPrime", cases, random -> {
            int value = random.nextInt(10) == 0 ? INT_EXTREMES[random.nextInt(INT_EXTREMES.length)]
                    : random.nextBoolean() ? random.nextInt(-5, 10_000) : random.nextInt();
            return call("" + value, () -> "" + NumericAlgorithms.isPrime(value));
        });
        each("sievePrimes", cases / 10, random -> {
            int limit = random.nextInt(20) == 0 ? -1 : random.nextInt(0, 3_000);
            return call("" + limit, () -> s(NumericAlgorithms.sievePrimes(limit)));
        });
        each("smallestPrimeFactors", cases / 10, random -> {
            int limit = random.nextInt(20) == 0 ? -1 : random.nextInt(0, 3_000);
            return call("" + limit, () -> s(NumericAlgorithms.smallestPrimeFactors(limit)));
        });
        each("primeFactorization", cases, random -> {
            long value = random.nextInt(10) == 0 ? random.nextLong(-3, 2)
                    : random.nextInt(10) != 0 ? random.nextLong(1, 1_000_000) : random.nextLong(1, 1_000_000_000_000L);
            return call("" + value, () -> s(NumericAlgorithms.primeFactorization(value)));
        });
        each("binomial", cases, random -> {
            int n = random.nextInt(10) == 0 ? random.nextInt(-2, 3) : random.nextInt(0, 400);
            int k = random.nextInt(-2, Math.max(1, n + 3));
            return call(n + "C" + k, () -> "" + NumericAlgorithms.binomial(n, k));
        });
        each("fibonacci", cases, random -> {
            int n = random.nextInt(10) == 0 ? -1 : random.nextInt(0, 2_000);
            return call("" + n, () -> "" + NumericAlgorithms.fibonacci(n));
        });
        each("catalan", cases, random -> {
            int n = random.nextInt(10) == 0 ? -1 : random.nextInt(0, 300);
            return call("" + n, () -> "" + NumericAlgorithms.catalan(n));
        });
    }

    // ---------------------------------------------------------------- graphs (PrimitiveGraphAlgorithms)

    private static int vertices(SplittableRandom random) {
        int tier = random.nextInt(100);
        return tier < 5 ? 0 : tier < 85 ? random.nextInt(1, 10) : tier < 99 ? random.nextInt(10, 200)
                : random.nextInt(200, 2_001);
    }

    private static int[][] edges(SplittableRandom random, int n, boolean allowBad) {
        int m = n == 0 ? random.nextInt(2) : random.nextInt(0, Math.min(4 * n + 3, 8_000));
        int[] from = new int[m];
        int[] to = new int[m];
        for (int i = 0; i < m; i++) {
            from[i] = n == 0 ? 0 : random.nextInt(n);
            to[i] = n == 0 ? 0 : random.nextInt(n);
            if (allowBad && random.nextInt(400) == 0) {
                to[i] = random.nextBoolean() ? -1 : n;
            }
        }
        return new int[][] {from, to};
    }

    private static long[] weights(SplittableRandom random, int m, boolean signed, boolean zeroOne) {
        long[] weights = new long[m];
        for (int i = 0; i < m; i++) {
            weights[i] = zeroOne ? random.nextInt(random.nextInt(50) == 0 ? 3 : 2)
                    : random.nextInt(60) == 0 ? LONG_EXTREMES[random.nextInt(LONG_EXTREMES.length)]
                    : signed ? random.nextLong(-20, 60) : random.nextLong(0, 100);
        }
        return weights;
    }

    private static String edgeText(int n, int[][] edges) {
        return "n=" + n + " m=" + edges[0].length + " " + shorten(s(edges));
    }

    private void graphs() {
        each("CsrGraph", cases, random -> {
            int n = vertices(random);
            int[][] e = edges(random, n, true);
            boolean undirected = random.nextBoolean();
            return call(edgeText(n, e) + " u=" + undirected, () -> {
                PrimitiveGraphAlgorithms.CsrGraph graph = undirected
                        ? PrimitiveGraphAlgorithms.CsrGraph.undirected(n, e[0], e[1])
                        : PrimitiveGraphAlgorithms.CsrGraph.directed(n, e[0], e[1]);
                PrimitiveGraphAlgorithms.CsrGraph copy = new PrimitiveGraphAlgorithms.CsrGraph(graph.vertices(),
                        graph.offsets(), graph.edges());
                int source = n == 0 ? 0 : random.nextInt(n);
                return shorten(s(graph.offsets()) + s(graph.edges())) + " eq=" + graph.equals(copy)
                        + call(" dfs" + source, () -> s(PrimitiveGraphAlgorithms.dfsPreorder(graph, source)))
                        + call(" cc", () -> "" + PrimitiveGraphAlgorithms.connectedComponents(graph))
                        + call(" bip", () -> "" + PrimitiveGraphAlgorithms.isBipartite(graph));
            });
        });
        each("CsrGraphRaw", cases / 4, random -> {
            int n = random.nextInt(-1, 5);
            int[] offsets = ints(random, random.nextInt(0, 7), 6);
            int[] edgeIds = ints(random, random.nextInt(0, 6), 5);
            return call(n + s(offsets) + s(edgeIds), () -> new PrimitiveGraphAlgorithms.CsrGraph(n, offsets, edgeIds)
                    .vertices() + "");
        });
        each("WeightedCsrGraph", cases, random -> {
            int n = vertices(random);
            int[][] e = edges(random, n, true);
            boolean zeroOne = random.nextInt(4) != 0;
            long[] w = weights(random, e[0].length, random.nextInt(10) == 0, zeroOne);
            boolean undirected = random.nextBoolean();
            return call(edgeText(n, e) + " w=" + shorten(s(w)) + " u=" + undirected, () -> {
                PrimitiveGraphAlgorithms.WeightedCsrGraph graph = undirected
                        ? PrimitiveGraphAlgorithms.WeightedCsrGraph.undirected(n, e[0], e[1], w)
                        : PrimitiveGraphAlgorithms.WeightedCsrGraph.directed(n, e[0], e[1], w);
                PrimitiveGraphAlgorithms.WeightedCsrGraph copy = new PrimitiveGraphAlgorithms.WeightedCsrGraph(
                        graph.vertices(), graph.offsets(), graph.edges(), graph.weights());
                int source = random.nextInt(20) == 0 ? n : n == 0 ? 0 : random.nextInt(n);
                return "eq=" + graph.equals(copy) + call(" 01bfs" + source,
                        () -> shorten(s(PrimitiveGraphAlgorithms.zeroOneBfs(graph, source))));
            });
        });
        each("bellmanFord", cases, random -> {
            int n = vertices(random);
            int[][] e = edges(random, n, true);
            long[] w = weights(random, e[0].length, true, false);
            int source = random.nextInt(20) == 0 ? -1 : n == 0 ? 0 : random.nextInt(n);
            return call(edgeText(n, e) + " w=" + shorten(s(w)) + " s=" + source, () -> {
                PrimitiveGraphAlgorithms.EdgeList list = new PrimitiveGraphAlgorithms.EdgeList(n, e[0], e[1], w);
                return shorten(s(list.from()) + s(list.to()) + s(list.weights())) + " "
                        + shorten(s(PrimitiveGraphAlgorithms.bellmanFord(list, source)));
            });
        });
        each("unionFind", cases, random -> {
            int n = vertices(random);
            int[][] e = edges(random, n, true);
            long[] w = weights(random, e[0].length, true, false);
            return call(edgeText(n, e) + " w=" + shorten(s(w)),
                    () -> call("ufc", () -> "" + PrimitiveGraphAlgorithms.unionFindComponents(n, e[0], e[1]))
                            + call(" cyc", () -> "" + PrimitiveGraphAlgorithms.hasUndirectedCycle(n, e[0], e[1]))
                            + call(" mst", () -> {
                                PrimitiveGraphAlgorithms.MstResult mst = PrimitiveGraphAlgorithms.kruskalMst(n,
                                        e[0], e[1], w);
                                return mst.totalWeight() + "/" + mst.edgesUsed() + "/" + mst.connected();
                            }));
        });
    }

    // ---------------------------------------------------------------- strings (StringAlgorithms)

    private void strings() {
        each("minimumWindow", cases, random -> {
            String source = text(random, length(random) % 300, 6);
            String required = text(random, random.nextInt(5), 6);
            boolean withMonitor = random.nextBoolean();
            TraceMonitor monitor = monitor(random);
            return call("'" + source + "' '" + required + "'", () -> (withMonitor
                    ? StringAlgorithms.minimumWindow(source, required, monitor)
                    : StringAlgorithms.minimumWindow(source, required)))
                    + (withMonitor ? " trace=" + monitor.trace() : "");
        });
        each("kmpIndexOf", cases, random -> {
            String textValue = text(random, length(random) % 300, 3);
            String pattern = random.nextInt(4) == 0 && textValue.length() > 2
                    ? textValue.substring(random.nextInt(textValue.length() / 2), textValue.length() / 2 + 1)
                    : text(random, random.nextInt(5), 3);
            boolean withMonitor = random.nextBoolean();
            TraceMonitor monitor = monitor(random);
            return call("'" + textValue + "' '" + pattern + "'", () -> (withMonitor
                    ? StringAlgorithms.kmpIndexOf(textValue, pattern, monitor)
                    : StringAlgorithms.kmpIndexOf(textValue, pattern))
                    + "/rk=" + StringAlgorithms.rabinKarpIndexOf(textValue, pattern)
                    + "/pf=" + s(StringAlgorithms.prefixFunction(pattern)))
                    + (withMonitor ? " trace=" + monitor.trace() : "");
        });
        each("anagramPalindrome", cases, random -> {
            String left = text(random, random.nextInt(12), 16);
            String right = random.nextBoolean() ? shuffle(random, left) : text(random, left.length(), 16);
            return call("'" + left + "' '" + right + "'", () -> StringAlgorithms.isAnagram(left, right)
                    + "/" + StringAlgorithms.isPalindromeIgnoringNonAlphanumeric(left)
                    + "/" + StringAlgorithms.isPalindromeIgnoringNonAlphanumeric(left
                    + new StringBuilder(left).reverse()));
        });
        each("longestCommonPrefix", cases, random -> {
            String[] values = new String[random.nextInt(0, 6)];
            String stem = text(random, random.nextInt(6), 3);
            for (int i = 0; i < values.length; i++) {
                values[i] = random.nextInt(30) == 0 ? null : stem.substring(0, random.nextInt(stem.length() + 1))
                        + text(random, random.nextInt(4), 3);
            }
            return call(Arrays.toString(values), () -> StringAlgorithms.longestCommonPrefix(values));
        });
        each("reverseWords", cases, random -> {
            String value = text(random, random.nextInt(30), 8);
            return call("'" + value + "'", () -> "'" + StringAlgorithms.reverseWords(value) + "'");
        });
        each("manacher", cases, random -> {
            String value = text(random, length(random) % 200, random.nextBoolean() ? 2 : 6);
            return call("'" + value + "'", () -> s(StringAlgorithms.manacherOdd(value)) + "/"
                    + s(StringAlgorithms.manacherEven(value)));
        });
        each("suffixArray", cases, random -> {
            String value = text(random, length(random) % 400, random.nextBoolean() ? 2 : 12);
            return call("'" + value + "'", () -> {
                int[] order = StringAlgorithms.suffixArray(value);
                return s(order) + "/" + s(StringAlgorithms.lcpArray(value, order)) + "/'"
                        + StringAlgorithms.longestRepeatedSubstring(value) + "'";
            });
        });
        each("lcpArrayRefusal", cases / 10, random -> {
            String value = text(random, random.nextInt(6), 3);
            int[] order = ints(random, random.nextInt(7), 6);
            return call("'" + value + "' " + s(order), () -> s(StringAlgorithms.lcpArray(value, order)));
        });
    }

    private static String shuffle(SplittableRandom random, String value) {
        char[] chars = value.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char swap = chars[i];
            chars[i] = chars[j];
            chars[j] = swap;
        }
        return new String(chars);
    }

    // ---------------------------------------------------------------- linked lists (M3LinkedListAlgorithms)

    private void lists() {
        each("IntLinkedList", cases, random -> {
            int n = random.nextInt(0, 30);
            int[] values = ints(random, n, 50);
            int[] next = new int[n];
            int mode = random.nextInt(4);
            for (int i = 0; i < n; i++) {
                next[i] = mode == 0 ? i + 1 < n ? i + 1 : -1 : random.nextInt(-1, n + (random.nextInt(50) == 0 ? 1 : 0));
            }
            int head = n == 0 ? (random.nextInt(10) == 0 ? 0 : -1) : random.nextInt(-1, n);
            return call(s(values) + s(next) + " h=" + head, () -> {
                LinkedListAlgorithms.IntLinkedList list = mode == 1
                        ? LinkedListAlgorithms.IntLinkedList.linear(values)
                        : new LinkedListAlgorithms.IntLinkedList(values, next, head);
                return s(list.values()) + s(list.next()) + list.head()
                        + call(" cyc", () -> LinkedListAlgorithms.cycleStartIndex(list) + "/"
                        + LinkedListAlgorithms.hasCycle(list))
                        + call(" arr", () -> s(LinkedListAlgorithms.toArray(list)))
                        + call(" re", () -> s(LinkedListAlgorithms.toArray(LinkedListAlgorithms.reordered(list))));
            });
        });
    }

    // ---------------------------------------------------------------- trie (M3PrimitiveTrie)

    private void trie() {
        each("primitiveTrie", cases, random -> {
            PrimitiveTrie trie = new PrimitiveTrie();
            StringBuilder ops = new StringBuilder();
            int count = random.nextInt(1, 40);
            for (int op = 0; op < count; op++) {
                String word = random.nextInt(40) == 0 ? null : text(random, random.nextInt(6), 4);
                int kind = random.nextInt(4);
                ops.append(call(" " + kind + ":" + word, () -> switch (kind) {
                    case 0 -> {
                        trie.add(word);
                        yield trie.nodeCount() + "/" + trie.edgeCount();
                    }
                    case 1 -> "" + trie.contains(word);
                    case 2 -> "" + trie.hasPrefix(word);
                    default -> "" + trie.longestStoredPrefixLength(word);
                }));
            }
            return ops.toString();
        });
    }

    // ---------------------------------------------------------------- trees (M3PrimitiveTreeAlgorithms)

    /** A random binary tree over n nodes (index permutation, random shape); BST values when {@code bst}. */
    static int[][] randomTree(SplittableRandom random, int n, boolean bst) {
        int[] left = new int[n];
        int[] right = new int[n];
        Arrays.fill(left, -1);
        Arrays.fill(right, -1);
        int[] order = random.ints(0, Integer.MAX_VALUE).limit(n).toArray();
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = ids[i];
            ids[i] = ids[j];
            ids[j] = swap;
        }
        int[] values = new int[n];
        int[] keys = bst ? random.ints(-1_000, 1_000).distinct().limit(n).toArray() : null;
        for (int i = 1; i < n; i++) {
            int node = ids[i];
            int at = ids[0];
            while (true) {
                boolean goLeft = bst ? keys[i] < keys[indexOf(ids, at)] : random.nextBoolean();
                int[] side = goLeft ? left : right;
                if (side[at] < 0) {
                    side[at] = node;
                    break;
                }
                at = side[at];
            }
        }
        for (int i = 0; i < n; i++) {
            values[ids[i]] = bst ? keys[i] : random.nextInt(-50, 51) + (random.nextInt(30) == 0 ? order[i] : 0);
        }
        return new int[][] {values, left, right, {n == 0 ? -1 : ids[0]}};
    }

    private static int indexOf(int[] values, int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private void trees() {
        each("BinaryTree", cases, random -> {
            int n = random.nextInt(10) == 0 ? 0 : random.nextInt(1, 40);
            boolean bst = random.nextBoolean();
            int[][] t = randomTree(random, n, bst);
            if (random.nextInt(25) == 0 && n > 1) {
                t[1][random.nextInt(n)] = random.nextInt(n);
            }
            int a = n == 0 ? 0 : random.nextBoolean() ? t[0][random.nextInt(n)] : random.nextInt(-60, 61);
            int b = n == 0 ? 0 : t[0][random.nextInt(n)];
            return call(s(t) + " a=" + a + " b=" + b, () -> {
                PrimitiveTreeAlgorithms.BinaryTree tree = new PrimitiveTreeAlgorithms.BinaryTree(t[0], t[1], t[2],
                        t[3][0]);
                return s(tree.values()) + s(tree.left()) + s(tree.right()) + tree.root()
                        + call(" pre", () -> s(PrimitiveTreeAlgorithms.preorder(tree)))
                        + call(" post", () -> s(PrimitiveTreeAlgorithms.postorder(tree)))
                        + call(" dia", () -> "" + PrimitiveTreeAlgorithms.diameterEdges(tree))
                        + call(" lca", () -> "" + PrimitiveTreeAlgorithms.lowestCommonAncestorBstByValue(tree, a, b))
                        + call(" mps", () -> "" + PrimitiveTreeAlgorithms.maxPathSum(tree))
                        + call(" inv", () -> s(PrimitiveTreeAlgorithms.preorder(PrimitiveTreeAlgorithms.inverted(
                        tree))));
            });
        });
    }

    // ---------------------------------------------------------------- dynamic programming

    private void dynamicProgramming() {
        each("climbStairs", cases, random -> {
            int n = random.nextInt(10) == 0 ? random.nextInt(-2, 0) : random.nextInt(0, 100);
            return call("" + n, () -> "" + DynamicProgrammingAlgorithms.climbStairs(n));
        });
        each("minCostClimbingStairs", cases, random -> {
            int[] cost = ints(random, length(random) % 200, random.nextInt(6) == 0 ? 0 : 1_000);
            return call(s(cost), () -> "" + DynamicProgrammingAlgorithms.minCostClimbingStairs(cost));
        });
        each("coinChangeWays", cases, random -> {
            int[] coins = ints(random, random.nextInt(0, 6), 30);
            if (random.nextInt(4) != 0) {
                for (int i = 0; i < coins.length; i++) {
                    coins[i] = Math.floorMod(coins[i], 30) + 1;
                }
            }
            int amount = random.nextInt(15) == 0 ? -1 : random.nextInt(0, random.nextInt(10) == 0 ? 5_000 : 200);
            return call(s(coins) + " a=" + amount, () -> ""
                    + DynamicProgrammingAlgorithms.coinChangeWays(coins, amount));
        });
        each("minPathSum", cases, random -> {
            int[][] grid = grid(random, 1_000);
            return call(s(grid), () -> "" + DynamicProgrammingAlgorithms.minPathSum(grid));
        });
        each("targetSumWays", cases, random -> {
            int[] values = ints(random, random.nextInt(0, 20), 12);
            if (random.nextInt(5) != 0) {
                for (int i = 0; i < values.length; i++) {
                    values[i] = Math.floorMod(values[i], 12);
                }
            }
            int target = random.nextInt(-40, 41);
            return call(s(values) + " t=" + target, () -> ""
                    + DynamicProgrammingAlgorithms.targetSumWays(values, target));
        });
        each("decodeWays", cases, random -> {
            int n = random.nextInt(0, 60);
            char[] digits = new char[n];
            for (int i = 0; i < n; i++) {
                int pick = random.nextInt(40);
                digits[i] = pick == 0 ? 'x' : pick == 1 ? '١' : (char) ('0' + random.nextInt(random.nextBoolean()
                        ? 3 : 10));
            }
            String value = new String(digits);
            return call("'" + value + "'", () -> "" + DynamicProgrammingAlgorithms.decodeWays(value));
        });
    }

    private static int[][] grid(SplittableRandom random, int bound) {
        int rows = random.nextInt(15) == 0 ? 0 : random.nextInt(1, 12);
        int columns = random.nextInt(15) == 0 ? 0 : random.nextInt(1, 12);
        int[][] grid = new int[rows][];
        for (int r = 0; r < rows; r++) {
            grid[r] = ints(random, random.nextInt(30) == 0 ? columns + 1 : columns, bound);
            if (bound == 1) {
                for (int c = 0; c < grid[r].length; c++) {
                    grid[r][c] = random.nextInt(3) == 0 ? 1 : 0;
                }
            }
        }
        return grid;
    }

    // ---------------------------------------------------------------- backtracking

    private void backtracking() {
        each("combinations", cases, random -> {
            int n = random.nextInt(-1, 12);
            int k = random.nextInt(-1, 13);
            return call(n + "C" + k, () -> shorten(s(BacktrackingAlgorithms.combinations(n, k))));
        });
        each("combinationSum", cases, random -> {
            int[] candidates = ints(random, random.nextInt(0, 5), 12);
            if (random.nextInt(5) != 0) {
                for (int i = 0; i < candidates.length; i++) {
                    candidates[i] = Math.floorMod(candidates[i], 12) + 2;
                }
            }
            int target = random.nextInt(15) == 0 ? -1 : random.nextInt(0, 30);
            return call(s(candidates) + " t=" + target, () -> shorten(s(
                    BacktrackingAlgorithms.combinationSum(candidates, target))));
        });
        each("nQueensCount", 14, random -> {
            int n = random.nextInt(-1, 11);
            return call("" + n, () -> "" + BacktrackingAlgorithms.nQueensCount(n));
        });
        each("solveSudoku", Math.max(10, cases / 10), random -> {
            char[][] board = sudoku(random);
            return call(s(board), () -> BacktrackingAlgorithms.solveSudoku(board) + " " + s(board));
        });
    }

    private static final String SOLVED = "534678912672195348198342567859761423426853791713924856961537284287419635"
            + "345286179";

    /** A relabelled, row/column-shuffled solved grid with holes (always solvable) or a corrupted board. */
    static char[][] sudoku(SplittableRandom random) {
        int[] digit = random.ints(1, 10).distinct().limit(9).toArray();
        int band = random.nextInt(3);
        char[][] board = new char[9][9];
        for (int r = 0; r < 9; r++) {
            int row = r < 3 ? band * 3 + r : r >= band * 3 && r < band * 3 + 3 ? r - band * 3 : r;
            for (int c = 0; c < 9; c++) {
                char solved = SOLVED.charAt(row * 9 + c);
                board[r][c] = (char) ('0' + digit[solved - '1']);
            }
        }
        int holes = random.nextInt(20, 60);
        for (int i = 0; i < holes; i++) {
            board[random.nextInt(9)][random.nextInt(9)] = '.';
        }
        int corrupt = random.nextInt(12);
        if (corrupt == 0) {
            board[random.nextInt(9)][random.nextInt(9)] = 'x';
        } else if (corrupt == 1) {
            board[0][0] = board[0][1] == '.' ? '1' : board[0][1];
        } else if (corrupt == 2) {
            board[random.nextInt(9)] = new char[random.nextInt(8)];
        }
        return board;
    }

    // ---------------------------------------------------------------- matrices

    private void matrices() {
        each("floodFill", cases, random -> {
            int[][] image = grid(random, 2);
            int r = random.nextInt(-1, 13);
            int c = random.nextInt(-1, 13);
            int color = random.nextInt(-2, 3);
            return call(s(image) + " " + r + "," + c + "," + color, () -> s(MatrixAlgorithms.floodFill(image,
                    r, c, color)) + " src=" + s(image));
        });
        each("shortestPathBinaryMatrix", cases, random -> {
            int[][] grid = grid(random, 1);
            return call(s(grid), () -> "" + MatrixAlgorithms.shortestPathBinaryMatrix(grid));
        });
        each("maximalSquareArea", cases, random -> {
            int[][] grid = grid(random, 1);
            for (int[] row : grid) {
                for (int c = 0; c < row.length; c++) {
                    row[c] = random.nextInt(4) == 0 ? 0 : row[c] == 0 ? 1 : row[c];
                }
            }
            return call(s(grid), () -> "" + MatrixAlgorithms.maximalSquareArea(grid));
        });
        each("deepCopy", cases / 4, random -> {
            int[][] grid = random.nextInt(20) == 0 ? new int[][] {null} : grid(random, 9);
            return call(s(grid), () -> {
                int[][] copy = MatrixAlgorithms.deepCopy(grid);
                return s(copy) + " shared=" + (copy.length > 0 && copy[0] == grid[0]);
            });
        });
    }

    // ---------------------------------------------------------------- bits

    private void bits() {
        each("bitTricks", cases, random -> {
            int a = ints(random, 1, random.nextBoolean() ? 100 : 0)[0];
            int b = ints(random, 1, random.nextBoolean() ? 100 : 0)[0];
            return call(a + "," + b, () -> BitAlgorithms.isPowerOfTwo(a) + "/"
                    + BitAlgorithms.addWithoutPlus(a, b)
                    + call("/and", () -> "" + BitAlgorithms.rangeBitwiseAnd(Math.min(a, b), Math.max(a, b))));
        });
        each("twoSingleNumbers", cases, random -> {
            int pairs = random.nextInt(0, 10);
            int[] values = new int[pairs * 2 + 2];
            for (int i = 0; i < pairs; i++) {
                values[2 * i] = ints(random, 1, 50)[0];
                values[2 * i + 1] = values[2 * i];
            }
            values[values.length - 2] = ints(random, 1, 0)[0];
            values[values.length - 1] = random.nextInt(10) == 0 ? values[values.length - 2] : ints(random, 1, 0)[0];
            int[] input = random.nextInt(30) == 0 ? null : values;
            return call(s(input), () -> s(BitAlgorithms.twoSingleNumbers(input)));
        });
    }
}
