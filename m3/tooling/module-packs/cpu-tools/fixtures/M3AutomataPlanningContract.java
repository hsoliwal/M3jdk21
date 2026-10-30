// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.M3DfaPartitions;
import com.m3.tools.cpu.M3KnapsackSelection;
import com.m3.tools.cpu.M3DominatorLanes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Independent truth tables, path-exclusion BFS and exhaustive subset oracle. */
public final class M3AutomataPlanningContract {
    private M3AutomataPlanningContract() { }
    private static long checks;
    private static void require(boolean condition, String category) {
        if (!condition) throw new AssertionError(category + " check=" + checks);
        checks++;
    }
    private static void rejects(Runnable task) {
        try { task.run(); throw new AssertionError("expected bounded refusal"); }
        catch (IllegalArgumentException | IndexOutOfBoundsException expected) { checks++; }
    }
    private static boolean finalAt(long[] bits, int state) {
        return (bits[state >>> 6] & (1L << (state & 63))) != 0;
    }
    private static boolean[][] distinguish(int states, int alpha, int[] edges, long[] finals) {
        boolean[][] different = new boolean[states][states];
        for (int a = 0; a < states; a++) for (int b = 0; b < states; b++)
            different[a][b] = finalAt(finals, a) != finalAt(finals, b);
        for (int cycle = 0; cycle < states * states + 1; cycle++) {
            boolean changed = false;
            for (int a = 0; a < states; a++) for (int b = 0; b < states; b++) {
                if (different[a][b]) continue;
                for (int symbol = 0; symbol < alpha; symbol++) {
                    if (different[edges[a * alpha + symbol]][edges[b * alpha + symbol]]) {
                        different[a][b] = true; changed = true; break;
                    }
                }
            }
            if (!changed) return different;
        }
        throw new AssertionError("reference DFA fixed point");
    }
    private static boolean accept(int state, int[] word, int alpha, int[] edges, long[] finals) {
        for (int symbol : word) state = edges[state * alpha + symbol];
        return finalAt(finals, state);
    }

    private static void automata() {
        Random rand = new Random(0xDFA42L);
        for (int round = 0; round < 950; round++) {
            int states = 1 + rand.nextInt(20), alphabet = 1 + rand.nextInt(4);
            int[] edges = new int[states * alphabet];
            long[] finals = new long[(states + 63) >>> 6];
            for (int i = 0; i < edges.length; i++) edges[i] = rand.nextInt(states);
            for (int i = 0; i < states; i++) if (rand.nextBoolean()) finals[i >>> 6] |= 1L << (i & 63);
            var value = M3DfaPartitions.minimize(states, alphabet, edges, finals);
            int[] classes = value.originalClass();
            boolean[][] reference = distinguish(states, alphabet, edges, finals);
            for (int a = 0; a < states; a++) for (int b = 0; b < states; b++)
                require((classes[a] == classes[b]) == !reference[a][b], "DFA pair equivalence");
            for (int i = 0; i < 100; i++) {
                int[] word = new int[rand.nextInt(10)];
                for (int k = 0; k < word.length; k++) word[k] = rand.nextInt(alphabet);
                int start = rand.nextInt(states);
                require(accept(start, word, alphabet, edges, finals) == value.accepts(start, word), "DFA language");
            }
            if (classes.length > 0) {
                classes[0] = -100;
                require(value.originalClass()[0] >= 0, "detached DFA output");
            }
        }
        for (int n : new int[]{63, 64, 65, 127, 128}) {
            int[] edges = new int[n * 2];
            long[] finals = new long[(n + 63) >>> 6];
            for (int i = 0; i < n; i++) { edges[2 * i] = (i + 1) % n; edges[2 * i + 1] = i; }
            finals[(n - 1) >>> 6] |= 1L << ((n - 1) & 63);
            var value = M3DfaPartitions.minimize(n, 2, edges, finals);
            require(value.classCount() == n, "large DFA partition");
        }
        rejects(() -> M3DfaPartitions.minimize(2, 1, new int[]{0, 2}, new long[]{0}));
        rejects(() -> M3DfaPartitions.minimize(129, 1, new int[129], new long[3]));
        rejects(() -> M3DfaPartitions.minimize(1, 1, new int[]{0}, new long[]{-1}));
    }

    private static void knapsack() {
        Random random = new Random(0x4e41505341434bL);
        for (int trial = 0; trial < 2400; trial++) {
            int count = random.nextInt(14), capacity = random.nextInt(45);
            int[] weights = new int[count]; long[] values = new long[count];
            for (int i = 0; i < count; i++) { weights[i] = random.nextInt(13); values[i] = random.nextInt(200); }
            long best = 0;
            for (int mask = 0; mask < (1 << count); mask++) {
                long value = 0; int weight = 0;
                for (int item = 0; item < count; item++) if ((mask & (1 << item)) != 0) {
                    value += values[item]; weight += weights[item];
                }
                if (weight <= capacity) best = Math.max(best, value);
            }
            var plan = M3KnapsackSelection.maximize(weights, values, capacity);
            require(plan.value() == best, "exhaustive knapsack optimum");
            int weight = 0; long value = 0; int previous = -1;
            for (int item : plan.chosen()) {
                require(item > previous && item < count, "knapsack unique indices");
                previous = item; weight += weights[item]; value += values[item];
            }
            require(weight <= capacity && weight == plan.weight() && value == best, "knapsack witness");
        }
        int[] weights = new int[128];
        long[] values = new long[128];
        Arrays.fill(weights, 1); Arrays.fill(values, 1);
        var large = M3KnapsackSelection.maximize(weights, values, 4096);
        require(large.chosen().length == 128 && large.value() == 128, "bounded knapsack maximum");
        rejects(() -> M3KnapsackSelection.maximize(new int[]{-1}, new long[]{2}, 3));
        rejects(() -> M3KnapsackSelection.maximize(new int[]{0}, new long[]{-1}, 0));
        try { M3KnapsackSelection.maximize(new int[]{0, 0}, new long[]{Long.MAX_VALUE, 1}, 0);
            throw new AssertionError("expected overflow");
        } catch (ArithmeticException expected) { checks++; }
    }

    private static boolean reachableRemoving(int n, int[] offsets, int[] target,
                                             int source, int destination, int removed) {
        if (source == removed) return false;
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(source); seen[source] = true;
        while (!queue.isEmpty()) {
            int v = queue.removeFirst();
            if (v == destination) return true;
            for (int p = offsets[v]; p < offsets[v + 1]; p++) {
                int next = target[p];
                if (next != removed && !seen[next]) { seen[next] = true; queue.add(next); }
            }
        }
        return false;
    }

    private static void dominators() {
        Random random = new Random(0xD0A17A7L);
        for (int trial = 0; trial < 1300; trial++) {
            int nodes = 1 + random.nextInt(13), source = random.nextInt(nodes);
            List<Integer> edges = new ArrayList<>();
            int[] starts = new int[nodes + 1];
            for (int i = 0; i < nodes; i++) {
                for (int j = 0; j < nodes; j++) if (random.nextInt(5) == 0) edges.add(j);
                starts[i + 1] = edges.size();
            }
            int[] target = edges.stream().mapToInt(Integer::intValue).toArray();
            var result = M3DominatorLanes.compute(starts, target, source);
            for (int dest = 0; dest < nodes; dest++) {
                boolean reached = reachableRemoving(nodes, starts, target, source, dest, -1);
                for (int dominator = 0; dominator < nodes; dominator++) {
                    boolean expected = reached && (dest == dominator
                            || !reachableRemoving(nodes, starts, target, source, dest, dominator));
                    require(result.dominates(dominator, dest) == expected, "path-removal dominator");
                }
            }
        }
        for (int n : new int[]{64, 65, 127, 128, 255, 256}) {
            int[] offsets = new int[n + 1], to = new int[n - 1];
            for (int i = 0; i < n - 1; i++) { offsets[i + 1] = i + 1; to[i] = i + 1; }
            offsets[n] = n - 1;
            var result = M3DominatorLanes.compute(offsets, to, 0);
            require(result.dominates(0, n - 1), "entry dominates chain");
            require(result.dominates(n - 1, n - 1), "self dominates chain");
            require(!result.dominates(n - 1, 0), "terminal does not dominate entry");
        }
        rejects(() -> M3DominatorLanes.compute(new int[]{0, 2}, new int[]{0}, 0));
        rejects(() -> M3DominatorLanes.compute(new int[]{0, 1}, new int[]{1}, 0));
    }

    public static void main(String[] ignored) {
        automata(); knapsack(); dominators();
        System.out.println("M3_WAVE4_AUTOMATA_PLANNING_PASS checks=" + checks);
    }
}
