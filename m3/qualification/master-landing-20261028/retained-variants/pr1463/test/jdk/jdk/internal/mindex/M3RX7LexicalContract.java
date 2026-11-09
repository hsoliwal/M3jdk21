/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *
 * @test
 * @summary Lexical predicates, epsilon NFA, token partitions and original UTF-16 ranges
 * @modules java.base/jdk.internal.mindex
 * @run main/othervm -Xmx96m M3RX7LexicalContract
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import jdk.internal.mindex.M3RX7;

public final class M3RX7LexicalContract {
    private static final String ROOT = "0123456789abcdef".repeat(4);
    private static long checks;

    private record Edge(int source, int target, int kind, long spelling, int must, int not) { }

    private static final class Graph {
        final int states;
        final int start;
        final byte[] accepting;
        final List<Edge> edges = new ArrayList<>();

        Graph(int states, int start, int... finals) {
            this.states = states;
            this.start = start;
            accepting = new byte[states];
            for (int state : finals) accepting[state] = 1;
        }

        Graph edge(int source, int target, int kind, long spelling, int must, int not) {
            edges.add(new Edge(source, target, kind, spelling, must, not));
            return this;
        }

        Graph epsilon(int source, int target) {
            return edge(source, target, -2, -1, 0, 0);
        }

        M3RX7.LexicalPlan compile() {
            int[] offsets = new int[states + 1];
            int[] targets = new int[edges.size()];
            int[] kinds = new int[edges.size()];
            long[] spellings = new long[edges.size()];
            int[] must = new int[edges.size()];
            int[] not = new int[edges.size()];
            int cursor = 0;
            for (int state = 0; state < states; state++) {
                offsets[state] = cursor;
                for (Edge edge : edges) {
                    if (edge.source() != state) continue;
                    targets[cursor] = edge.target();
                    kinds[cursor] = edge.kind();
                    spellings[cursor] = edge.spelling();
                    must[cursor] = edge.must();
                    not[cursor++] = edge.not();
                }
            }
            offsets[states] = cursor;
            return M3RX7.fromLexicalNfa(ROOT, start, offsets, targets, kinds,
                    spellings, must, not, accepting);
        }

        // Independent oracle: a token-position/state matrix and repeated global epsilon
        // relaxation. It shares no compiled offsets, masks or candidate traversal helpers.
        boolean oracle(int[] kinds, long[] spellings, int[] flags, int from, int to) {
            boolean[][] seen = new boolean[to - from + 1][states];
            seen[0][start] = true;
            for (int position = from; position <= to; position++) {
                boolean[] current = seen[position - from];
                boolean changed;
                do {
                    changed = false;
                    for (Edge edge : edges) {
                        if (edge.kind() == -2 && current[edge.source()]
                                && !current[edge.target()]) {
                            current[edge.target()] = true;
                            changed = true;
                        }
                    }
                } while (changed);
                if (position == to) {
                    for (int state = 0; state < states; state++) {
                        if (current[state] && accepting[state] != 0) return true;
                    }
                    return false;
                }
                for (Edge edge : edges) {
                    if (edge.kind() == -2 || !current[edge.source()]) continue;
                    if (edge.kind() >= 0 && edge.kind() != kinds[position]) continue;
                    if (edge.spelling() >= 0 && edge.spelling() != spellings[position]) continue;
                    if ((flags[position] & edge.must()) != edge.must()) continue;
                    if ((flags[position] & edge.not()) != 0) continue;
                    seen[position - from + 1][edge.target()] = true;
                }
            }
            throw new AssertionError("unreachable oracle tail");
        }

        long findOracle(int[] kinds, long[] spellings, int[] flags, int from, int to) {
            for (int first = from; first <= to; first++) {
                for (int last = to; last >= first; last--) {
                    if (oracle(kinds, spellings, flags, first, last)) return range(first, last);
                }
            }
            return -1L;
        }
    }

    public static void main(String[] args) {
        lexicalMeaning();
        epsilonAndWideMasks();
        generatedGraphs();
        snapshotsAndRefusals();
        sourceCoordinates();
        System.out.println("M3RX7_LEXICAL_CONTRACT_PASS checks=" + checks);
    }

    private static void lexicalMeaning() {
        // (identifier[42] '(' ')' | identifier[7])+; CODE is required and literal/comment
        // channels are forbidden. Equal spelling IDs alone never override lexical context.
        Graph graph = new Graph(8, 0, 7).epsilon(0, 1).epsilon(0, 4)
                .edge(1, 2, 1, 42, 4, 3).edge(2, 3, 2, -1, 4, 3)
                .edge(3, 7, 3, -1, 4, 3).edge(4, 7, 1, 7, 4, 3)
                .epsilon(7, 0);
        M3RX7.LexicalPlan plan = graph.compile();
        int[] kinds = {1, 1, 1, 2, 3};
        long[] spelling = {42, 7, 42, 999, 999};
        int[] flags = {1, 4, 4, 4, 4};
        equal(false, plan.matches(kinds, spelling, flags));
        equal(true, plan.matches(kinds, spelling, flags, 1, 5));
        equal(range(1, 5), plan.find(kinds, spelling, flags));
        equal(false, plan.matches(new int[]{1}, new long[]{42}, new int[]{2}));
        equal(false, plan.matches(new int[]{9}, new long[]{7}, new int[]{4}));
        equal(false, plan.matches(new int[]{1}, new long[]{7}, new int[]{0}));
        equal(false, plan.matches(new int[0], new long[0], new int[0]));
        equal(ROOT, plan.programRoot());
        equal(8, plan.stateCount());
        equal(7, plan.edgeCount());
        check(plan.retainedPrimitiveBytes() > 0, "primitive byte accounting");
        // Same token spelling identity with a different kind does not match.
        M3RX7.LexicalPlan exact = new Graph(2, 0, 1)
                .edge(0, 1, 17, Long.MAX_VALUE, Integer.MIN_VALUE, 8).compile();
        equal(true, exact.matches(new int[]{17}, new long[]{Long.MAX_VALUE},
                new int[]{Integer.MIN_VALUE}));
        equal(false, exact.matches(new int[]{17}, new long[]{Long.MAX_VALUE},
                new int[]{Integer.MIN_VALUE | 8}));
    }

    private static void epsilonAndWideMasks() {
        M3RX7.LexicalPlan empty = new Graph(3, 0, 2).epsilon(0, 1)
                .epsilon(1, 0).epsilon(1, 2).compile();
        equal(true, empty.matches(new int[0], new long[0], new int[0]));
        equal(range(0, 0), empty.find(new int[]{1}, new long[]{2}, new int[]{0}));
        equal(true, empty.accepts(new long[]{1}));
        equal(false, empty.accepts(new long[]{0}));

        for (int states : new int[]{1, 63, 64, 65, 127, 128, 129, 255, 256}) {
            Graph chain = new Graph(states, 0, states - 1);
            Graph epsilon = new Graph(states, 0, states - 1);
            for (int state = 0; state < states - 1; state++) {
                chain.edge(state, state + 1, 6, -1, 0, 0);
                epsilon.epsilon(state, state + 1);
            }
            int[] kinds = new int[states - 1];
            Arrays.fill(kinds, 6);
            long[] spelling = new long[kinds.length];
            int[] flags = new int[kinds.length];
            M3RX7.LexicalPlan plan = chain.compile();
            equal(true, plan.matches(kinds, spelling, flags));
            equal(range(0, kinds.length), plan.find(kinds, spelling, flags));
            equal(true, epsilon.compile().matches(new int[0], new long[0], new int[0]));
            long[] frontier = plan.initialStates();
            for (int token = 0; token < kinds.length; token++) {
                plan.advance(frontier, kinds, spelling, flags, token, token + 1);
            }
            equal(true, plan.accepts(frontier));
        }
    }

    private static void generatedGraphs() {
        Random random = new Random(0x4c45584943414cL);
        for (int trial = 0; trial < 640; trial++) {
            int states = 1 + random.nextInt(8);
            Graph graph = new Graph(states, random.nextInt(states));
            for (int state = 0; state < states; state++) {
                graph.accepting[state] = (byte) (random.nextInt(3) == 0 ? 1 : 0);
                for (int edge = random.nextInt(5); edge > 0; edge--) {
                    int kind = random.nextInt(6) - 2;
                    int must = random.nextInt(8);
                    int not = random.nextInt(8) & ~must;
                    graph.edge(state, random.nextInt(states), kind,
                            kind == -2 ? -1 : random.nextInt(5) - 1,
                            kind == -2 ? 0 : must, kind == -2 ? 0 : not);
                }
            }
            M3RX7.LexicalPlan plan = graph.compile();
            for (int sample = 0; sample < 16; sample++) {
                int length = random.nextInt(11);
                int[] kinds = new int[length];
                long[] spelling = new long[length];
                int[] flags = new int[length];
                for (int token = 0; token < length; token++) {
                    kinds[token] = random.nextInt(4);
                    spelling[token] = random.nextInt(4);
                    flags[token] = random.nextInt(8);
                }
                boolean expected = graph.oracle(kinds, spelling, flags, 0, length);
                equal(expected, plan.matches(kinds, spelling, flags));
                equal(graph.findOracle(kinds, spelling, flags, 0, length),
                        plan.find(kinds, spelling, flags));
                int from = random.nextInt(length + 1);
                int to = from + random.nextInt(length - from + 1);
                equal(graph.oracle(kinds, spelling, flags, from, to),
                        plan.matches(kinds, spelling, flags, from, to));
                equal(graph.findOracle(kinds, spelling, flags, from, to),
                        plan.find(kinds, spelling, flags, from, to));
                int split = random.nextInt(length + 1);
                long[] frontier = plan.initialStates();
                plan.advance(frontier, kinds, spelling, flags, 0, split);
                plan.advance(frontier, kinds, spelling, flags, split, split);
                plan.advance(frontier, kinds, spelling, flags, split, length);
                equal(expected, plan.accepts(frontier));
            }
        }
    }

    private static void snapshotsAndRefusals() {
        int[] offsets = {0, 1, 1};
        int[] targets = {1};
        int[] kinds = {1};
        long[] spelling = {2};
        int[] must = {4};
        int[] not = {8};
        byte[] accepting = {0, 1};
        M3RX7.LexicalPlan plan = M3RX7.fromLexicalNfa(ROOT, 0, offsets, targets,
                kinds, spelling, must, not, accepting);
        Arrays.fill(offsets, 0);
        Arrays.fill(targets, 0);
        Arrays.fill(kinds, 9);
        Arrays.fill(spelling, 9);
        Arrays.fill(must, 0);
        Arrays.fill(not, 4);
        Arrays.fill(accepting, (byte) 0);
        equal(true, plan.matches(new int[]{1}, new long[]{2}, new int[]{4}));
        long[] frontier = plan.initialStates();
        long[] before = frontier.clone();
        expect(IllegalArgumentException.class, () -> plan.advance(frontier,
                new int[]{1, -1}, new long[]{2, 2}, new int[]{4, 4}, 0, 2));
        check(Arrays.equals(before, frontier), "failure leaves frontier unchanged");
        expect(IllegalArgumentException.class, () -> plan.advance(frontier,
                new int[]{1}, frontier, new int[]{4}, 0, 1));
        expect(IllegalArgumentException.class, () -> plan.accepts(new long[]{8}));
        expect(IllegalArgumentException.class, () -> plan.matches(new int[]{1}, new long[0], new int[]{4}));
        expect(IllegalArgumentException.class, () -> plan.matches(new int[]{1}, new long[]{-1}, new int[]{4}));
        expect(IndexOutOfBoundsException.class, () -> plan.find(new int[]{1}, new long[]{2}, new int[]{4}, -1, 1));
        expect(NullPointerException.class, () -> plan.matches(null, new long[0], new int[0]));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa("bad", 0,
                new int[]{0, 0}, new int[0], new int[0], new long[0], new int[0], new int[0], new byte[]{1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{0, 1}, new int[]{1}, new int[]{0}, new long[]{-1}, new int[]{0}, new int[]{0}, new byte[]{1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{0, 1}, new int[]{0}, new int[]{-2}, new long[]{0}, new int[]{0}, new int[]{0}, new byte[]{1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{0, 1}, new int[]{0}, new int[]{0}, new long[]{-1}, new int[]{1}, new int[]{1}, new byte[]{1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{1, 0}, new int[0], new int[0], new long[0], new int[0], new int[0], new byte[]{1}));
        expect(IllegalArgumentException.class, () -> M3RX7.fromLexicalNfa(ROOT, 0,
                new int[]{0, 0}, new int[0], new int[0], new long[0], new int[0], new int[0], new byte[]{2}));
        Graph dense = new Graph(256, 0, 255);
        for (int source = 0; source < 256; source++) {
            for (int target = 0; target < 16; target++) dense.edge(source, target, -1, -1, 0, 0);
        }
        M3RX7.LexicalPlan bounded = dense.compile();
        expect(IllegalArgumentException.class, () -> bounded.find(new int[64], new long[64], new int[64]));
        expect(IllegalArgumentException.class, () -> bounded.matches(new int[4096], new long[4096], new int[4096]));
        dense.edge(0, 0, -1, -1, 0, 0);
        expect(IllegalArgumentException.class, dense::compile);
    }

    private static void sourceCoordinates() {
        int[] offsets = {0, 5, 9, 12};
        int[] lengths = {3, 2, 1, 2};
        equal(range(5, 14), M3RX7.utf16Range(range(1, 4), offsets, lengths, 17));
        equal(range(9, 9), M3RX7.utf16Range(range(2, 2), offsets, lengths, 17));
        equal(range(17, 17), M3RX7.utf16Range(range(4, 4), offsets, lengths, 17));
        equal(-1L, M3RX7.utf16Range(-1L, offsets, lengths, 17));
        equal(range(0, 0), M3RX7.utf16Range(range(0, 0), new int[0], new int[0], 0));
        equal(5, M3RX7.rangeStart(range(5, 14)));
        equal(14, M3RX7.rangeEnd(range(5, 14)));
        // UTF-16 coordinates may select supplementary pairs or individual surrogate units.
        equal(range(1, 3), M3RX7.utf16Range(range(0, 1), new int[]{1}, new int[]{2}, 4));
        expect(IllegalArgumentException.class, () -> M3RX7.rangeStart(-1));
        expect(IllegalArgumentException.class, () -> M3RX7.rangeEnd(range(5, 4)));
        expect(IllegalArgumentException.class, () -> M3RX7.rangeStart(0xffff_ffffL));
        expect(IllegalArgumentException.class, () -> M3RX7.utf16Range(range(0, 1),
                new int[]{2, 1}, new int[]{1, 1}, 4));
        expect(IllegalArgumentException.class, () -> M3RX7.utf16Range(range(0, 1),
                new int[]{0, 1}, new int[]{2, 1}, 4));
        expect(IndexOutOfBoundsException.class, () -> M3RX7.utf16Range(range(0, 5), offsets, lengths, 17));
        expect(IndexOutOfBoundsException.class, () -> M3RX7.utf16Range(range(0, 1),
                new int[]{Integer.MAX_VALUE}, new int[]{1}, Integer.MAX_VALUE));
    }

    private static long range(int first, int last) {
        return ((long) first << 32) | Integer.toUnsignedLong(last);
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual + ", check=" + checks);
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (!type.isInstance(failure)) throw new AssertionError("wrong exception", failure);
            checks++;
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }
}
