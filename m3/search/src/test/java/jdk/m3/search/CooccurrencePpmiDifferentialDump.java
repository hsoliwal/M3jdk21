// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.search;

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
 * Differential dump of the cooccurrence-ppmi leaf (gate 3, recipe/cooccurrence-ppmi-20261106/differential.sh). From
 * the named seed {@link #SEED} it draws 2,000 corpora and replays each through the co-occurrence PPMI class named by
 * args[0], reached by reflection so that the donor class and the relocated class run the same driver:
 * <ul>
 *   <li>corpora of 0..9 documents with 0..24 tokens over a vocabulary with non-ASCII, astral, isolated-surrogate,
 *       NUL, equal-hash (Aa/BB) and 64-character tokens; windows 1..6 (one corpus in ten up to 32); default limits,
 *       generous limits or (one corpus in eight) a tight budget that may refuse; plus the refusals of window 0,
 *       window above maxWindow, an empty token, an oversized token, a null document and invalid Limits;</li>
 *   <li>per snapshot: vocabulary, events, window, distinctPairs, the first 40 pairCounts and their total, centerCount
 *       and contextCount of every vocabulary token and one missing token, 16 ppmi values (raw bits), and
 *       topPositiveContexts(center, k) for every center (and one missing) with k drawn from
 *       {0, 1, 2, 5, 20, Integer.MAX_VALUE}; every 25th corpus the refusals of a null center and a negative limit.</li>
 * </ul>
 * The Java selection path runs (the opt-in native property is not set). Two classes agree when their dumps are
 * byte-identical; the final TOTAL line repeats the figures.
 * Usage: java -cp classes;test-classes jdk.m3.search.CooccurrencePpmiDifferentialDump OWNER_FQN
 */
public final class CooccurrencePpmiDifferentialDump {
    /** The named seed of the dump: M3COOCCURRENCE_PPMI_SEED. */
    static final long SEED = 0x5eed_2026_1106_0006L;
    private static final int CORPORA = 2_000;
    private static final int PAIR_LINES = 40;
    private static final String[] WORDS = {"a", "b", "c", "the", "Aa", "BB", "café", "猫", "😀",
        "\uD800", "\uDC00", "", "a b", "\0", "x".repeat(64)};
    private static final int[] LIMITS = {0, 1, 2, 5, 20, Integer.MAX_VALUE};
    private final Method build;
    private final Constructor<?> limitsOf;
    private final Method vocabulary;
    private final Method events;
    private final Method window;
    private final Method distinctPairs;
    private final Method centerCount;
    private final Method contextCount;
    private final Method pairCount;
    private final Method ppmi;
    private final Method top;
    private final Method pairCounts;
    private final Method pairCenter;
    private final Method pairContext;
    private final Method pairCountValue;
    private final Method scoreContext;
    private final Method scoreCount;
    private final Method scorePpmi;
    private final PrintStream out;
    private long state = SEED;
    private long snapshots;
    private long refusals;
    private long queries;
    private long scores;
    private long lines;

    private CooccurrencePpmiDifferentialDump(Class<?> owner, PrintStream out) throws ReflectiveOperationException {
        Class<?> limits = Class.forName(owner.getName() + "$Limits");
        Class<?> snapshot = Class.forName(owner.getName() + "$Snapshot");
        Class<?> pair = Class.forName(owner.getName() + "$PairCount");
        Class<?> score = Class.forName(owner.getName() + "$ContextScore");
        build = owner.getMethod("build", List.class, int.class, limits);
        limitsOf = limits.getConstructor(int.class, long.class, int.class, int.class, long.class, int.class, int.class);
        vocabulary = snapshot.getMethod("vocabulary");
        events = snapshot.getMethod("events");
        window = snapshot.getMethod("window");
        distinctPairs = snapshot.getMethod("distinctPairs");
        centerCount = snapshot.getMethod("centerCount", String.class);
        contextCount = snapshot.getMethod("contextCount", String.class);
        pairCount = snapshot.getMethod("pairCount", String.class, String.class);
        ppmi = snapshot.getMethod("ppmi", String.class, String.class);
        top = snapshot.getMethod("topPositiveContexts", String.class, int.class);
        pairCounts = snapshot.getMethod("pairCounts");
        pairCenter = pair.getMethod("center");
        pairContext = pair.getMethod("context");
        pairCountValue = pair.getMethod("count");
        scoreContext = score.getMethod("context");
        scoreCount = score.getMethod("count");
        scorePpmi = score.getMethod("ppmi");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        new CooccurrencePpmiDifferentialDump(Class.forName(args[0]), out).run();
        out.flush();
    }

    private void run() throws ReflectiveOperationException {
        Object defaults = limitsOf.newInstance(10_000, 1_000_000L, 100_000, 1_000_000, 20_000_000L, 32, 4_096);
        Object wide = limitsOf.newInstance(100, 10_000L, 10_000, 100_000, 1_000_000L, 32, 4_096);
        Object narrow = limitsOf.newInstance(100, 10_000L, 10_000, 100_000, 1_000_000L, 4, 8);
        emit("refuse window0=" + call(build, null, List.of(List.of("a")), 0, wide) + " window33="
                + call(build, null, List.of(List.of("a")), 33, wide) + " window5-narrow="
                + call(build, null, List.of(List.of("a")), 5, narrow) + " empty-token="
                + call(build, null, List.of(List.of("a", "")), 1, wide) + " oversized-token="
                + call(build, null, List.of(List.of("a", "x".repeat(9))), 1, narrow) + " null-document="
                + call(build, null, Arrays.asList(List.of("a"), null), 1, wide) + " limits="
                + construct(0, 1L, 1, 1, 1L, 1, 1) + construct(1, 1L, 1, 1, 1L, 33, 1));
        for (int corpus = 0; corpus < CORPORA; corpus++) {
            List<List<String>> documents = documents();
            int span = corpus % 10 == 9 ? 1 + (int) next(32) : 1 + (int) next(6);
            Object limits = corpus % 8 == 7 ? tight() : corpus % 2 == 0 ? defaults : wide;
            String head = "corpus " + corpus + " window=" + span + " documents=" + documents.size();
            Object snapshot;
            try {
                snapshot = build.invoke(null, documents, span, limits);
            } catch (InvocationTargetException refused) {
                refusals++;
                emit(head + " REFUSED " + refusal(refused));
                continue;
            }
            snapshots++;
            describe(head, snapshot);
            rank(corpus, snapshot);
        }
        emit("TOTAL corpora=" + CORPORA + " snapshots=" + snapshots + " refusals=" + refusals + " queries=" + queries
                + " scores=" + scores + " lines=" + (lines + 1));
    }

    private List<List<String>> documents() {
        List<List<String>> documents = new ArrayList<>();
        for (int d = 0, n = (int) next(10); d < n; d++) {
            List<String> document = new ArrayList<>();
            for (int i = 0, words = (int) next(25); i < words; i++) {
                document.add(WORDS[(int) next(WORDS.length)]);
            }
            documents.add(document);
        }
        return documents;
    }

    private Object tight() throws ReflectiveOperationException {
        return limitsOf.newInstance(1 + (int) next(10), 1 + next(80), 1 + (int) next(16), 1 + (int) next(120),
                1 + next(400), 1 + (int) next(32), 1 + (int) next(70));
    }

    private void describe(String head, Object snapshot) throws ReflectiveOperationException {
        List<?> words = (List<?>) vocabulary.invoke(snapshot);
        List<?> pairs = (List<?>) pairCounts.invoke(snapshot);
        StringBuilder line = new StringBuilder(head).append(" vocabulary=").append(codes(words))
                .append(" events=").append(events.invoke(snapshot)).append(" window=").append(window.invoke(snapshot))
                .append(" distinctPairs=").append(distinctPairs.invoke(snapshot)).append(" pairs=").append(pairs.size());
        for (Object p : pairs.subList(0, Math.min(PAIR_LINES, pairs.size()))) {
            line.append(' ').append(codes(List.of(pairCenter.invoke(p), pairContext.invoke(p)))).append(':')
                    .append(pairCountValue.invoke(p));
        }
        emit(line.toString());
        line.setLength(0);
        line.append("marginals");
        List<Object> probes = new ArrayList<>(words);
        probes.add("missing");
        for (Object token : probes) {
            line.append(' ').append(call(centerCount, snapshot, token)).append('/').append(call(contextCount, snapshot, token));
        }
        line.append(" ppmi");
        for (int i = 0; i < 16; i++) {
            Object a = probes.get((int) next(probes.size()));
            Object b = probes.get((int) next(probes.size()));
            line.append(' ').append(call(pairCount, snapshot, a, b)).append('=').append(call(ppmi, snapshot, a, b));
        }
        emit(line.toString());
    }

    private void rank(int corpus, Object snapshot) throws ReflectiveOperationException {
        List<Object> centers = new ArrayList<>((List<?>) vocabulary.invoke(snapshot));
        centers.add("missing");
        for (Object center : centers) {
            int k = LIMITS[(int) next(LIMITS.length)];
            queries++;
            StringBuilder line = new StringBuilder("top ").append(codes(List.of(center))).append(" k=").append(k);
            try {
                for (Object s : (List<?>) top.invoke(snapshot, center, k)) {
                    scores++;
                    line.append(' ').append(codes(List.of(scoreContext.invoke(s)))).append(':')
                            .append(scoreCount.invoke(s)).append('=')
                            .append(Long.toHexString(Double.doubleToRawLongBits((Double) scorePpmi.invoke(s))));
                }
            } catch (InvocationTargetException refused) {
                refusals++;
                line.append(" !").append(refusal(refused));
            }
            emit(line.toString());
        }
        if (corpus % 25 == 0) {
            emit("refuse null-center=" + call(top, snapshot, null, 1) + " negative=" + call(top, snapshot, "a", -1)
                    + " null-ppmi=" + call(ppmi, snapshot, null, "a") + " null-count=" + call(centerCount, snapshot,
                    (Object) null));
        }
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
            if (value instanceof Double number) {
                return Long.toHexString(Double.doubleToRawLongBits(number));
            }
            return value instanceof Long || value instanceof Integer ? ":" + value : ":snapshot";
        } catch (InvocationTargetException refused) {
            refusals++;
            return "!" + refusal(refused);
        }
    }

    private static String refusal(InvocationTargetException refused) {
        Throwable cause = refused.getCause();
        return cause.getClass().getSimpleName() + "(" + cause.getMessage() + ")";
    }

    private static String codes(List<?> tokens) {
        StringBuilder text = new StringBuilder("[");
        for (Object t : tokens) {
            if (text.length() > 1) {
                text.append(',');
            }
            ((String) t).chars().forEach(c -> text.append(String.format("%04x", c)));
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
