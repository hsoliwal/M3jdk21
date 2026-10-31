/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.internal.mindex.M3TokenIndex;
import jdk.internal.mindex.M3TokenPattern;
import jdk.internal.mindex.M3TokenPredicateCache;

/**
 * java.util.regex oracle of the token-sequence regex engine (TOKEN REGEX ORACLE v1).
 *
 * <p>A tape of one-letter word tokens maps to a character string with one character per significant
 * token, and a token pattern over word atoms maps to a Java regex by deleting the token separators and
 * spelling the any-token atom {@code []} as {@code .}. The span sets of the two engines agree for every
 * pattern; leftmost starts agree; full match/capture priority agrees only for priority-safe patterns
 * (no alternation inside a quantifier body whose maximum exceeds one), because the token engine
 * publishes deeper repetition levels first while java.util.regex backtracks choice by choice.</p>
 */
final class TokenRegexOracle {
    /** Word kind id of every generated token. */
    static final int WORD = 10;
    /** Letters of generated tapes and patterns. */
    static final String LETTERS = "abc";
    /** Trivia spelling inserted between words by {@link #tape(String, boolean)}. */
    static final String TRIVIA = "_";
    /** Generator limits: items per sequence, nesting depth, and quantifier bounds. */
    private static final int MAX_ITEMS = 3;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_BOUND = 3;
    private static final double GROUP_RATE = 0.3;
    private static final double QUANT_RATE = 0.4;
    private static final double ALT_RATE = 0.4;
    private static final double ANY_RATE = 0.15;
    private static final double LAZY_RATE = 0.25;
    private static final double NAMED_RATE = 0.5;

    private TokenRegexOracle() {
    }

    /** Sorted-word vocabulary: lexeme id = index + 1. */
    record Vocab(String[] words) implements M3TokenPredicateCache.Vocabulary {
        @Override
        public int lexemeOf(CharSequence spelling) {
            int at = Arrays.binarySearch(words, spelling.toString());
            return at < 0 ? 0 : at + 1;
        }

        @Override
        public int symbolOf(String key, String value) {
            return -1;
        }
    }

    /** Token tape: the index, its source text, the significant letters and the raw position of each. */
    record Tape(M3TokenIndex index, String source, String letters, int[] significant, Vocab vocab) {
    }

    /** Tape of one token per letter; with trivia, a trivia token precedes every word and ends the tape. */
    static Tape tape(String letters, boolean withTrivia) {
        List<String> words = new ArrayList<>();
        List<Boolean> trivia = new ArrayList<>();
        for (char c : letters.toCharArray()) {
            if (withTrivia) {
                words.add(TRIVIA);
                trivia.add(true);
            }
            words.add(String.valueOf(c));
            trivia.add(false);
        }
        if (withTrivia) {
            words.add(TRIVIA);
            trivia.add(true);
        }
        int n = words.size();
        String[] vocabulary = words.stream().distinct().sorted().toArray(String[]::new);
        Vocab vocab = new Vocab(vocabulary);
        int[] kinds = new int[n];
        int[] lexemes = new int[n];
        long[] starts = new long[n];
        long[] ends = new long[n];
        long[] flags = new long[n];
        int[] significant = new int[letters.length()];
        StringBuilder source = new StringBuilder();
        int k = 0;
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                source.append(' ');
            }
            kinds[i] = WORD;
            lexemes[i] = vocab.lexemeOf(words.get(i));
            starts[i] = source.length();
            source.append(words.get(i));
            ends[i] = source.length();
            flags[i] = trivia.get(i) ? M3TokenIndex.TRIVIA : 0L;
            if (!trivia.get(i)) {
                significant[k++] = i;
            }
        }
        M3TokenIndex index = M3TokenIndex.of(WORD, kinds, lexemes, starts, ends, flags);
        return new Tape(index, source.toString(), letters, significant, vocab);
    }

    static M3TokenPredicateCache bind(String pattern, Tape tape) {
        return M3TokenPredicateCache.bind(M3TokenPattern.compile(pattern), tape.index(), tape.source(), tape.vocab());
    }

    /** Java regex of a token pattern over word atoms. */
    static String javaRegex(String tokenPattern) {
        return tokenPattern.replace("[]", ".").replace(" ", "");
    }

    static Matcher javaMatcher(String tokenPattern, String letters) {
        Matcher m = Pattern.compile(javaRegex(tokenPattern)).matcher(letters);
        m.useAnchoringBounds(false);
        m.useTransparentBounds(true);
        return m;
    }

    /** All spans [s, e) over which the pattern matches exactly, as s * (n + 1) + e codes. */
    static TreeSet<Integer> javaSpans(String tokenPattern, String letters) {
        TreeSet<Integer> spans = new TreeSet<>();
        int n = letters.length();
        Matcher m = javaMatcher(tokenPattern, letters);
        for (int s = 0; s <= n; s++) {
            for (int e = s; e <= n; e++) {
                m.region(s, e);
                if (m.matches()) {
                    spans.add(s * (n + 1) + e);
                }
            }
        }
        return spans;
    }

    /** Random pattern; quantified bodies are never nullable, priority-safe patterns keep alternation out of loops. */
    static String pattern(SplittableRandom random, boolean prioritySafe) {
        int[] groups = {0};
        return sequence(random, 0, prioritySafe, false, groups);
    }

    /** Sequence of items; when {@code nonNullable}, the first item is a plain atom so the sequence consumes a token. */
    private static String sequence(SplittableRandom random, int depth, boolean safe, boolean inLoop, int[] groups,
            boolean nonNullable) {
        int items = random.nextInt(1, MAX_ITEMS + 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < items; i++) {
            if (i > 0) {
                out.append(' ');
            }
            out.append(i == 0 && nonNullable ? atom(random) : item(random, depth, safe, inLoop, groups));
        }
        return out.toString();
    }

    private static String sequence(SplittableRandom random, int depth, boolean safe, boolean inLoop, int[] groups) {
        return sequence(random, depth, safe, inLoop, groups, false);
    }

    private static String atom(SplittableRandom random) {
        return random.nextDouble() < ANY_RATE ? "[]" : String.valueOf(LETTERS.charAt(random.nextInt(LETTERS.length())));
    }

    private static String item(SplittableRandom random, int depth, boolean safe, boolean inLoop, int[] groups) {
        boolean quantified = random.nextDouble() < QUANT_RATE;
        int max = quantified ? (random.nextBoolean() ? 1 : MAX_BOUND) : 1;
        boolean loop = quantified && max > 1;
        String body;
        if (depth < MAX_DEPTH && random.nextDouble() < GROUP_RATE) {
            boolean allowAlt = !(safe && (loop || inLoop));
            String inner = sequence(random, depth + 1, safe, inLoop || loop, groups, quantified);
            if (allowAlt && random.nextDouble() < ALT_RATE) {
                inner = inner + " | " + sequence(random, depth + 1, safe, inLoop || loop, groups, quantified);
            }
            groups[0]++;
            body = random.nextDouble() < NAMED_RATE ? "(?<g" + groups[0] + "> " + inner + ")" : "(" + inner + ")";
        } else {
            body = atom(random);
        }
        if (!quantified) {
            return body;
        }
        String quant;
        if (max == 1) {
            quant = "?";
        } else {
            int choice = random.nextInt(3);
            int min = random.nextInt(0, MAX_BOUND);
            quant = choice == 0 ? "*" : choice == 1 ? "+" : "{" + min + "," + MAX_BOUND + "}";
        }
        return body + quant + (random.nextDouble() < LAZY_RATE ? "?" : "");
    }

    /** Random tape letters of length 0..maxLength. */
    static String letters(SplittableRandom random, int maxLength) {
        int n = random.nextInt(maxLength + 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            out.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        return out.toString();
    }
}
