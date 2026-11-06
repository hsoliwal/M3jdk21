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
 * Differential dump of the backoff-word-ngrams leaf (gate 3, recipe/backoff-word-ngrams-20261106/differential.sh).
 * From the named seed {@link #SEED} it draws 2,000 corpora and replays each through the backoff word n-gram class
 * named by args[0], reached by reflection so that the donor class and the relocated class run the same driver:
 * <ul>
 *   <li>corpora of 0..8 documents with 0..20 tokens over a vocabulary with non-ASCII, astral, isolated-surrogate,
 *       NUL, equal-hash (Aa/BB) and 64-character tokens; orders 1..8; default limits, generous limits, or (one corpus
 *       in eight) a tight budget (documents, tokens, contexts, transitions, observations, token chars) that may
 *       refuse training; plus refusals of order 0 / above maxOrder, an empty token and a null document;</li>
 *   <li>per trained model: order, observations, contexts; 24 queries of rank(history, k) with k in
 *       {0, 1, 2, 3, 5, 17, Integer.MAX_VALUE} (histories cut from documents, random, with unknown tokens, longer
 *       than the order) printing every Prediction (token code units, count, observed, suffix length, raw probability
 *       bits) and exactCount of the history against six next tokens; every 25th corpus the refusals of a null
 *       history, a negative limit, an empty history token and an empty next token.</li>
 * </ul>
 * Two classes agree when their dumps are byte-identical; the final TOTAL line repeats the figures.
 * Usage: java -cp classes;test-classes jdk.m3.nlp.M3BackoffWordNgramsDifferentialDump OWNER_FQN
 */
public final class M3BackoffWordNgramsDifferentialDump {
    /** The named seed of the dump: M3BACKOFF_WORD_NGRAMS_SEED. */
    static final long SEED = 0x5eed_2026_1106_0005L;
    private static final int CORPORA = 2_000;
    private static final int QUERIES = 24;
    private static final String[] WORDS = {"a", "b", "c", "the", "Aa", "BB", "café", "猫", "😀",
        "\uD800", "\uDC00", "", "a b", "\0", "x".repeat(64)};
    private static final int[] LIMITS = {0, 1, 2, 3, 5, 17, Integer.MAX_VALUE};
    private final Method train;
    private final Constructor<?> limitsOf;
    private final Method order;
    private final Method observations;
    private final Method contexts;
    private final Method exactCount;
    private final Method rank;
    private final Method token;
    private final Method count;
    private final Method observed;
    private final Method suffixLength;
    private final Method probability;
    private final PrintStream out;
    private long state = SEED;
    private long models;
    private long refusals;
    private long queries;
    private long predictions;
    private long lines;

    private M3BackoffWordNgramsDifferentialDump(Class<?> owner, PrintStream out) throws ReflectiveOperationException {
        Class<?> limits = Class.forName(owner.getName() + "$Limits");
        Class<?> model = Class.forName(owner.getName() + "$Model");
        Class<?> prediction = Class.forName(owner.getName() + "$Prediction");
        train = owner.getMethod("train", List.class, int.class, limits);
        limitsOf = limits.getConstructor(int.class, long.class, int.class, int.class, long.class, int.class, int.class);
        order = model.getMethod("order");
        observations = model.getMethod("observations");
        contexts = model.getMethod("contexts");
        exactCount = model.getMethod("exactCount", List.class, String.class);
        rank = model.getMethod("rank", List.class, int.class);
        token = prediction.getMethod("token");
        count = prediction.getMethod("count");
        observed = prediction.getMethod("observed");
        suffixLength = prediction.getMethod("suffixLength");
        probability = prediction.getMethod("probability");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        new M3BackoffWordNgramsDifferentialDump(Class.forName(args[0]), out).run();
        out.flush();
    }

    private void run() throws ReflectiveOperationException {
        Object defaults = limitsOf.newInstance(10_000, 1_000_000L, 1_000_000, 2_000_000, 8_000_000L, 4, 4_096);
        Object wide = limitsOf.newInstance(100, 10_000L, 10_000, 10_000, 80_000L, 8, 4_096);
        emit("refuse order0=" + call(train, null, List.of(List.of("a")), 0, wide) + " order9="
                + call(train, null, List.of(List.of("a")), 9, wide) + " order5-default="
                + call(train, null, List.of(List.of("a")), 5, defaults) + " empty-token="
                + call(train, null, List.of(List.of("a", "")), 2, wide) + " null-document="
                + call(train, null, Arrays.asList(List.of("a"), null), 2, wide) + " limits="
                + construct(0, 1L, 1, 1, 1L, 1, 1) + construct(1, 1L, 1, 1, 1L, 9, 1));
        for (int corpus = 0; corpus < CORPORA; corpus++) {
            List<List<String>> documents = documents();
            int modelOrder = 1 + (int) next(8);
            Object limits = corpus % 8 == 7 ? tight() : modelOrder <= 4 && corpus % 2 == 0 ? defaults : wide;
            String head = "corpus " + corpus + " order=" + modelOrder + " documents=" + documents.size();
            Object model;
            try {
                model = train.invoke(null, documents, modelOrder, limits);
            } catch (InvocationTargetException refused) {
                refusals++;
                emit(head + " REFUSED " + refusal(refused));
                continue;
            }
            models++;
            emit(head + " model order=" + order.invoke(model) + " observations=" + observations.invoke(model)
                    + " contexts=" + contexts.invoke(model));
            query(corpus, model, documents);
        }
        emit("TOTAL corpora=" + CORPORA + " models=" + models + " refusals=" + refusals + " queries=" + queries
                + " predictions=" + predictions + " lines=" + (lines + 1));
    }

    private List<List<String>> documents() {
        List<List<String>> documents = new ArrayList<>();
        for (int d = 0, n = (int) next(9); d < n; d++) {
            List<String> document = new ArrayList<>();
            for (int i = 0, words = (int) next(21); i < words; i++) {
                document.add(WORDS[(int) next(WORDS.length)]);
            }
            documents.add(document);
        }
        return documents;
    }

    private Object tight() throws ReflectiveOperationException {
        return limitsOf.newInstance(1 + (int) next(8), 1 + next(60), 1 + (int) next(40), 1 + (int) next(60),
                1 + next(200), 8, 1 + (int) next(70));
    }

    private void query(int corpus, Object model, List<List<String>> documents) throws ReflectiveOperationException {
        for (int q = 0; q < QUERIES; q++) {
            List<String> history = history(documents);
            int k = LIMITS[(int) next(LIMITS.length)];
            StringBuilder line = new StringBuilder("rank ").append(codes(history)).append(" k=").append(k);
            line.append(rankLine(model, history, k)).append(" exact");
            for (int n = 0; n < 6; n++) {
                String next = next(7) == 0 ? "missing" : WORDS[(int) next(WORDS.length)];
                line.append(' ').append(call(exactCount, model, history, next));
            }
            emit(line.toString());
        }
        if (corpus % 25 == 0) {
            emit("refuse null-history=" + call(rank, model, null, 1) + " negative=" + call(rank, model, List.of(), -1)
                    + " empty-history-token=" + call(rank, model, List.of("a", ""), 3) + " empty-next="
                    + call(exactCount, model, List.of("a"), "") + " empty-exact-history="
                    + call(exactCount, model, List.of(""), "a"));
        }
    }

    private String rankLine(Object model, List<String> history, int k) throws ReflectiveOperationException {
        queries++;
        Object result;
        try {
            result = rank.invoke(model, history, k);
        } catch (InvocationTargetException refused) {
            refusals++;
            return " !" + refusal(refused);
        }
        StringBuilder line = new StringBuilder(" ->");
        for (Object p : (List<?>) result) {
            predictions++;
            line.append(' ').append(codes(List.of((String) token.invoke(p)))).append(':').append(count.invoke(p))
                    .append('/').append(observed.invoke(p)).append('@').append(suffixLength.invoke(p)).append('=')
                    .append(Long.toHexString(Double.doubleToRawLongBits((Double) probability.invoke(p))));
        }
        return line.toString();
    }

    private List<String> history(List<List<String>> documents) {
        List<String> history = new ArrayList<>();
        if (!documents.isEmpty() && next(3) != 0) {
            List<String> document = documents.get((int) next(documents.size()));
            int end = document.isEmpty() ? 0 : (int) next(document.size() + 1);
            history.addAll(document.subList((int) next(end + 1), end));
        } else {
            for (int i = 0, n = (int) next(10); i < n; i++) {
                history.add(next(4) == 0 ? "missing" : WORDS[(int) next(WORDS.length)]);
            }
        }
        return history;
    }

    private String construct(Object... values) throws ReflectiveOperationException {
        try {
            limitsOf.newInstance(values);
            return ":ok";
        } catch (InvocationTargetException refused) {
            refusals++;
            return "!" + refusal(refused);
        }
    }

    private String call(Method method, Object target, Object... args) throws ReflectiveOperationException {
        queries++;
        try {
            Object value = method.invoke(target, args);
            return value == null ? "" : ":" + (value instanceof Long || value instanceof Integer ? value : "model");
        } catch (InvocationTargetException refused) {
            refusals++;
            return "!" + refusal(refused);
        }
    }

    private static String refusal(InvocationTargetException refused) {
        Throwable cause = refused.getCause();
        return cause.getClass().getSimpleName() + "(" + cause.getMessage() + ")";
    }

    private static String codes(List<String> tokens) {
        StringBuilder text = new StringBuilder("[");
        for (String t : tokens) {
            if (text.length() > 1) {
                text.append(',');
            }
            t.chars().forEach(c -> text.append(String.format("%04x", c)));
        }
        return text.append(']').toString();
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
