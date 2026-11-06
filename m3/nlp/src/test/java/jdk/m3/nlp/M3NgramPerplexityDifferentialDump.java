// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Differential dump of the scoped-ngram-perplexity leaf (gate 3, recipe/scoped-ngram-perplexity-20261106/
 * differential.sh). From the named seed {@link #SEED} it draws 2,000 sentence sets and replays each through the
 * scoped n-gram producer and the perplexity scorer named by args[0] (scorer) and args[1] (producer), reached by
 * reflection so that the donor pair and the relocated pair run the same driver:
 * <ul>
 *   <li>kinds 0..6: observed corpora (1..10 sentences of 0..9 tokens over signed IDs incl. Long.MIN_VALUE and
 *       Long.MAX_VALUE, multiplicity 1..3 or 1,000,000,007, maxOrder 1..4); kind 7: a bigram of a missing unigram;
 *       kind 8: an outgoing bigram marginal larger than its unigram (inconsistent counts); kind 9: no unigram at all
 *       (empty snapshot, or bigrams only), plus producer refusals (count overflow);</li>
 *   <li>three (alpha, bigramWeight) candidates per set, one in five drawn from the invalid values (0, negative, NaN,
 *       infinities, Double.MAX_VALUE, weights outside [0,1]); valid values include Double.MIN_VALUE, 1e-9, 1e3 and
 *       the weights 0, 1, 1e-12 and 1 - 1e-12 (the short-cut and log1p branches);</li>
 *   <li>per admitted scorer: vocabularySize, observationCount, scope identity, the unigram probability of every
 *       alphabet ID and four OOV probes, 12 conditional probabilities, 4 sentence log-likelihoods and the perplexity
 *       of four evaluation sets (plain, with empty sentences, empty list and only-empty list refused), and every 50th
 *       set the null refusals of logLikelihood, perplexity and a null sentence inside the set.</li>
 * </ul>
 * One canonical line per result (doubles as raw IEEE-754 bits in hex, refusals as exception simple name and message);
 * two pairs agree when their dumps are byte-identical. The final TOTAL line repeats the figures.
 * Usage: java -cp classes;test-classes jdk.m3.nlp.M3NgramPerplexityDifferentialDump SCORER_FQN PRODUCER_FQN
 */
public final class M3NgramPerplexityDifferentialDump {
    /** The named seed of the dump: M3NGRAM_PERPLEXITY_SEED. */
    static final long SEED = 0x5eed_2026_1106_0003L;
    private static final int SETS = 2_000;
    private static final int CANDIDATES_PER_SET = 3;
    private static final long[] ALPHABET = {Long.MIN_VALUE, -55L, -1L, 0L, 1L, 2L, 3L, 7L, 42L, 1_000_003L,
        Long.MAX_VALUE};
    private static final long[] OOV = {999_999_937L, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, -4096L};
    private static final double[] ALPHA_VALID = {Double.MIN_VALUE, 1e-9, 0.01, 0.25, 0.5, 1.0, 2.0, 1e3};
    private static final double[] ALPHA_INVALID = {0.0, -0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY,
        Double.NEGATIVE_INFINITY, Double.MAX_VALUE};
    private static final double[] WEIGHT_VALID = {0.0, 1e-12, 0.25, 0.5, 0.75, 1.0 - 1e-12, 1.0};
    private static final double[] WEIGHT_INVALID = {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, -1e-300};
    private final Constructor<?> scopeOf;
    private final Constructor<?> producerOf;
    private final Method observe;
    private final Method add;
    private final Method freeze;
    private final Method of;
    private final Method scope;
    private final Method vocabularySize;
    private final Method observationCount;
    private final Method unigram;
    private final Method conditional;
    private final Method logLikelihood;
    private final Method perplexity;
    private final PrintStream out;
    private long state = SEED;
    private long models;
    private long refusals;
    private long queries;
    private long lines;

    private M3NgramPerplexityDifferentialDump(Class<?> scorer, Class<?> producer, PrintStream out)
            throws ReflectiveOperationException {
        Class<?> scopeType = Class.forName(producer.getName() + "$Scope");
        Class<?> snapshot = Class.forName(producer.getName() + "$Snapshot");
        scopeOf = scopeType.getConstructor(String.class, String.class, String.class, long.class, String.class);
        producerOf = producer.getConstructor(scopeType, int.class, int.class, int.class);
        observe = producer.getMethod("observe", scopeType, long[].class, long.class);
        add = producer.getMethod("add", scopeType, long[].class, long.class);
        freeze = producer.getMethod("freeze");
        of = scorer.getMethod("of", snapshot, double.class, double.class);
        scope = scorer.getMethod("scope");
        vocabularySize = scorer.getMethod("vocabularySize");
        observationCount = scorer.getMethod("observationCount");
        unigram = scorer.getMethod("unigramProbability", long.class);
        conditional = scorer.getMethod("conditionalProbability", long.class, long.class);
        logLikelihood = scorer.getMethod("logLikelihood", long[].class);
        perplexity = scorer.getMethod("perplexity", Iterable.class);
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        new M3NgramPerplexityDifferentialDump(Class.forName(args[0]), Class.forName(args[1]), out).run();
        out.flush();
    }

    private void run() throws ReflectiveOperationException {
        emit("null-snapshot " + call(of, null, null, 1.0, 1.0));
        for (int set = 0; set < SETS; set++) {
            Object expected = scopeOf.newInstance("corpus-" + set, "r" + (set % 7), set % 3 == 0 ? "en" : "hi",
                    (long) set, "word");
            Object snapshot = corpus(set, expected);
            if (snapshot == null) {
                continue;
            }
            for (int candidate = 0; candidate < CANDIDATES_PER_SET; candidate++) {
                double alpha = draw(ALPHA_VALID, ALPHA_INVALID);
                double weight = draw(WEIGHT_VALID, WEIGHT_INVALID);
                score(set, candidate, snapshot, expected, alpha, weight);
            }
        }
        emit("TOTAL sets=" + SETS + " models=" + models + " refusals=" + refusals + " queries=" + queries
                + " lines=" + (lines + 1));
    }

    private Object corpus(int set, Object expected) throws ReflectiveOperationException {
        int kind = set % 10;
        int maxOrder = kind < 7 ? 1 + (int) next(4) : 2;
        Object producer = producerOf.newInstance(expected, maxOrder, 4096, 64);
        StringBuilder log = new StringBuilder("set ").append(set).append(" kind=").append(kind)
                .append(" order=").append(maxOrder);
        if (kind < 7) {
            observed(producer, expected, log);
        } else {
            added(kind, producer, expected, log);
        }
        emit(log.toString());
        return freeze.invoke(producer);
    }

    private void observed(Object producer, Object expected, StringBuilder log) throws ReflectiveOperationException {
        int sentences = 1 + (int) next(10);
        for (int s = 0; s < sentences; s++) {
            long[] tokens = sentence(10, false);
            long multiplicity = next(40) == 0 ? 1_000_000_007L : 1 + next(3);
            log.append(' ').append(Arrays.toString(tokens)).append('x').append(multiplicity)
                    .append(call(observe, producer, expected, tokens, multiplicity));
        }
    }

    private void added(int kind, Object producer, Object expected, StringBuilder log)
            throws ReflectiveOperationException {
        long a = pick(ALPHABET);
        long b = pick(ALPHABET);
        long c = 1 + next(5);
        if (kind == 7) {
            log.append(call(add, producer, expected, new long[] {a}, c));
            log.append(call(add, producer, expected, new long[] {a, b == a ? OOV[0] : b}, c));
        } else if (kind == 8) {
            log.append(call(add, producer, expected, new long[] {a}, c));
            log.append(call(add, producer, expected, new long[] {b}, c));
            log.append(call(add, producer, expected, new long[] {a, b}, c + 1 + next(3)));
        } else if (next(2) == 0) {
            log.append(call(add, producer, expected, new long[] {a, b}, c));
        } else {
            log.append(call(add, producer, expected, new long[] {a}, Long.MAX_VALUE - 1));
            log.append(call(add, producer, expected, new long[] {b == a ? OOV[1] : b}, 2L));
        }
    }

    private void score(int set, int candidate, Object snapshot, Object expected, double alpha, double weight)
            throws ReflectiveOperationException {
        String head = "model " + set + "." + candidate + " alpha=" + bits(alpha) + " weight=" + bits(weight);
        Object scorer;
        try {
            scorer = of.invoke(null, snapshot, alpha, weight);
        } catch (InvocationTargetException refused) {
            refusals++;
            emit(head + " REFUSED " + refusal(refused));
            return;
        }
        models++;
        emit(head + " vocabulary=" + vocabularySize.invoke(scorer) + " observations=" + observationCount.invoke(scorer)
                + " scope=" + expected.equals(scope.invoke(scorer)));
        probabilities(scorer);
        sentences(set, scorer);
    }

    private void probabilities(Object scorer) throws ReflectiveOperationException {
        StringBuilder line = new StringBuilder("unigram");
        for (long id : ALPHABET) {
            line.append(' ').append(call(unigram, scorer, id));
        }
        for (long id : OOV) {
            line.append(' ').append(call(unigram, scorer, id));
        }
        emit(line.toString());
        line.setLength(0);
        line.append("conditional");
        for (int pair = 0; pair < 12; pair++) {
            long previous = next(5) == 0 ? pick(OOV) : pick(ALPHABET);
            long current = next(5) == 0 ? pick(OOV) : pick(ALPHABET);
            line.append(' ').append(previous).append('>').append(current).append('=')
                    .append(call(conditional, scorer, previous, current));
        }
        emit(line.toString());
    }

    private void sentences(int set, Object scorer) throws ReflectiveOperationException {
        List<long[]> plain = new ArrayList<>();
        StringBuilder line = new StringBuilder("loglikelihood");
        for (int s = 0; s < 4; s++) {
            long[] tokens = sentence(9, true);
            plain.add(tokens);
            line.append(' ').append(Arrays.toString(tokens)).append('=').append(call(logLikelihood, scorer, tokens));
        }
        emit(line.toString());
        List<long[]> withEmpty = new ArrayList<>(plain);
        withEmpty.add(1, new long[0]);
        withEmpty.add(new long[0]);
        emit("perplexity plain=" + call(perplexity, scorer, plain) + " with-empty=" + call(perplexity, scorer, withEmpty)
                + " none=" + call(perplexity, scorer, List.of()) + " only-empty="
                + call(perplexity, scorer, List.of(new long[0], new long[0])));
        if (set % 50 == 0) {
            emit("nulls loglikelihood=" + call(logLikelihood, scorer, (Object) null) + " perplexity="
                    + call(perplexity, scorer, (Object) null) + " null-sentence="
                    + call(perplexity, scorer, Arrays.asList(plain.get(0), null)));
        }
    }

    private String call(Method method, Object target, Object... args) throws ReflectiveOperationException {
        queries++;
        try {
            Object value = method.invoke(target, args);
            if (value instanceof Double number) {
                return bits(number);
            }
            return value == null ? "" : ":" + value;
        } catch (InvocationTargetException refused) {
            refusals++;
            return "!" + refusal(refused);
        }
    }

    private static String refusal(InvocationTargetException refused) {
        Throwable cause = refused.getCause();
        return cause.getClass().getSimpleName() + "(" + cause.getMessage() + ")";
    }

    private static String bits(double value) {
        return Long.toHexString(Double.doubleToRawLongBits(value));
    }

    private long[] sentence(int bound, boolean withOov) {
        long[] tokens = new long[(int) next(bound)];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = withOov && next(6) == 0 ? pick(OOV) : pick(ALPHABET);
        }
        return tokens;
    }

    private double draw(double[] valid, double[] invalid) {
        return next(5) == 0 ? invalid[(int) next(invalid.length)] : valid[(int) next(valid.length)];
    }

    private long pick(long[] values) {
        return values[(int) next(values.length)];
    }

    private long next(long bound) {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return Long.remainderUnsigned(state * 0x2545F4914F6CDD1DL, bound);
    }

    private void emit(String line) {
        out.println(line);
        lines++;
    }
}
