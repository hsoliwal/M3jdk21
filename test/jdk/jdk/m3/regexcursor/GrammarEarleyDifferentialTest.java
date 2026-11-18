/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Independent Earley CFG recognizer differential against the M3 known-grammar LL(1) images
 * @modules java.base/jdk.m3.regexcursor java.base/jdk.internal.mindex
 * @run main/othervm GrammarEarleyDifferentialTest
 */
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.regexcursor.GrammarSpecification;
import jdk.m3.regexcursor.StaticGrammarLexicon;

/** CFG membership oracle; does not use the grammar prediction table. */
public final class GrammarEarleyDifferentialTest {
    private GrammarEarleyDifferentialTest() { }
    private static long assertions;
    private record Item(int production, int dot, int origin) { }

    private static boolean earley(int start, List<GrammarSpecification.Production> rules,
            int[] tokens) {
        @SuppressWarnings("unchecked")
        Set<Item>[] chart = (Set<Item>[]) new Set<?>[tokens.length + 1];
        for (int i = 0; i < chart.length; i++) chart[i] = new HashSet<>();
        for (int p = 0; p < rules.size(); p++) {
            if (rules.get(p).lhs() == start) chart[0].add(new Item(p, 0, 0));
        }
        for (int position = 0; position <= tokens.length; position++) {
            boolean changed;
            do {
                changed = false;
                for (Item item : List.copyOf(chart[position])) {
                    var rule = rules.get(item.production());
                    int[] rhs = rule.rhs();
                    if (item.dot() == rhs.length) {
                        for (Item previous : List.copyOf(chart[item.origin()])) {
                            int[] prev = rules.get(previous.production()).rhs();
                            if (previous.dot() < prev.length
                                    && prev[previous.dot()] == rule.lhs()) {
                                changed |= chart[position].add(new Item(
                                        previous.production(), previous.dot() + 1,
                                        previous.origin()));
                            }
                        }
                    } else if (rhs[item.dot()] < 0) {
                        for (int p = 0; p < rules.size(); p++) {
                            if (rules.get(p).lhs() == rhs[item.dot()]) {
                                changed |= chart[position].add(new Item(p, 0, position));
                            }
                        }
                    } else if (position < tokens.length && rhs[item.dot()] == tokens[position]) {
                        chart[position + 1].add(new Item(item.production(), item.dot() + 1,
                                item.origin()));
                    }
                }
            } while (changed);
        }
        for (Item item : chart[tokens.length]) {
            if (item.origin() == 0 && rules.get(item.production()).lhs() == start
                    && item.dot() == rules.get(item.production()).rhs().length) return true;
        }
        return false;
    }

    /** Generate some language members independently of the LL(1) prediction table. */
    private static int[] generatePositive(Random rng, List<GrammarSpecification.Production> rules) {
        ArrayDeque<Integer> expansion = new ArrayDeque<>();
        expansion.push(-1);
        List<Integer> output = new ArrayList<>();
        int expansions = 0;
        while (!expansion.isEmpty() && expansions++ < 45 && output.size() <= 8) {
            int symbol = expansion.pop();
            if (symbol > 0) {
                output.add(symbol);
                continue;
            }
            List<int[]> options = new ArrayList<>();
            for (var rule : rules) if (rule.lhs() == symbol) options.add(rule.rhs());
            if (options.isEmpty()) return null;
            int[] chosen = options.get(rng.nextInt(options.size()));
            for (int i = chosen.length - 1; i >= 0; i--) expansion.push(chosen[i]);
        }
        if (!expansion.isEmpty() || output.size() > 8) return null;
        return output.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void agree(boolean expected, boolean observed, String what, int seed,
            int[] tokens) {
        if (expected != observed) {
            throw new AssertionError(what + " seed=" + seed + " tokens="
                    + Arrays.toString(tokens) + " expected=" + expected + " got=" + observed);
        }
        assertions++;
    }

    public static void main(String[] args) {
        Random rng = new Random(0x20261118feedL);
        int admitted = 0;
        int refused = 0;
        int positive = 0;
        for (int gram = 0; gram < 400; gram++) {
            int ntCount = 1 + rng.nextInt(5);
            int termCount = ntCount * 4 + 1;
            List<StaticGrammarLexicon.Literal> fixed = new ArrayList<>();
            int[] terminals = new int[termCount];
            for (int i = 0; i < termCount; i++) {
                terminals[i] = 101 + i * 7;
                fixed.add(new StaticGrammarLexicon.Literal("t" + i, terminals[i], false));
            }
            List<GrammarSpecification.Production> productions = new ArrayList<>();
            for (int i = 0; i < ntCount; i++) {
                int nt = -(i + 1);
                int alt = 1 + rng.nextInt(3);
                for (int a = 0; a < alt; a++) {
                    int t = terminals[i * 4 + a];
                    List<Integer> rhs = new ArrayList<>();
                    rhs.add(t);
                    if (rng.nextBoolean() && i + 1 < ntCount) {
                        rhs.add(-(i + 2 + rng.nextInt(ntCount - i - 1)));
                    }
                    if (rng.nextBoolean()) rhs.add(terminals[termCount - 1]);
                    if (rng.nextBoolean()) rhs.add(nt); // right recursion, never left recursion
                    productions.add(new GrammarSpecification.Production(nt,
                            rhs.stream().mapToInt(Integer::intValue).toArray()));
                }
                if (rng.nextBoolean()) {
                    productions.add(new GrammarSpecification.Production(nt, new int[0]));
                }
            }
            // Deliberate non-LL(1) duplicate leading terminal in every tenth grammar.
            // This also exercises fail-closed conflict admission, not merely positive paths.
            if (gram % 10 == 0) {
                productions.add(new GrammarSpecification.Production(-1,
                        new int[] {terminals[0], terminals[termCount - 1]}));
            }
            GrammarSpecification compiled;
            try {
                compiled = GrammarSpecification.compile("earley", "rev" + gram, fixed,
                        List.of(), -1, productions);
            } catch (IllegalArgumentException conflict) {
                refused++;
                continue;
            }
            admitted++;
            GrammarSpecification image = GrammarSpecification.fromImage(compiled.image(),
                    GrammarSpecification.imageSha256(compiled.image()));
            GrammarSpecification syntax = GrammarSpecification.compileWithSyntaxImage(
                    "earley", "rev" + gram, fixed, List.of(), -1, productions,
                    compiled.syntaxImage());
            for (int test = 0; test < 160; test++) {
                int[] tokens = test % 5 == 0 ? generatePositive(rng, productions) : null;
                if (tokens == null) {
                    tokens = new int[rng.nextInt(7)];
                    for (int i = 0; i < tokens.length; i++) {
                        tokens[i] = rng.nextInt(14) == 0 ? 919191
                                : terminals[rng.nextInt(terminals.length)];
                    }
                }
                final int[] input = tokens;
                boolean expected = earley(-1, productions, input);
                if (expected) positive++;
                agree(expected, compiled.accepts(tokens), "compile", gram, tokens);
                agree(expected, image.accepts(tokens), "image", gram, tokens);
                agree(expected, syntax.accepts(tokens), "syntax image", gram, tokens);
                agree(expected, image.accepts(IntBuffer.wrap(tokens).asReadOnlyBuffer()),
                        "IntBuffer", gram, tokens);
                agree(expected, image.accepts((long) tokens.length,
                        i -> input[(int) i]), "long-index", gram, tokens);
                AtomicInteger pos = new AtomicInteger();
                boolean stream = image.accepts(() -> {
                    int at = pos.getAndIncrement();
                    return at >= input.length ? 0 : input[at];
                });
                agree(expected, stream, "stream", gram, tokens);
            }
        }
        if (admitted < 10 || refused < 1 || positive < 500) throw new AssertionError(
                "grammar corpus insufficient admitted=" + admitted + " refused=" + refused
                + " positive=" + positive);
        System.out.println("GrammarEarleyDifferentialTest PASS assertions=" + assertions
                + " grammars=" + admitted + " conflictsRefused=" + refused
                + " positiveCases=" + positive);
    }
}
