// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Differential dump (gate 3 of the relocation, recipe/differential.sh). Replays the 384 seeded corpora of
 * M3ScopedNgramsOracle (same xorshift state, token alphabet, budgets and draw order: 46,080 ranked queries plus the
 * oracle's fixed probes of absent contexts, null prefix, k and prefix-length budgets and frozen-snapshot stability)
 * and then 10,000 further seeded scoped queries over 40 wider corpora (prefixes cut from observed documents or drawn
 * at random, k in {1,2,3,4,7,16,4096}, count and contextTotal of every prefix, producer refusals of scope, batch,
 * multiplicity, count and order) through the scoped n-gram class named by args[0], reached by reflection so that
 * the donor class and the relocated class run the same driver, and prints one canonical line per result (candidate
 * = nextId:count:raw double bits). Two classes agree when their dumps are byte-identical. The final TOTAL line
 * repeats the figures.
 * Usage: java -cp ngrams-classes;test-classes jdk.m3.nlp.M3ScopedNgramsDifferentialDump FQN
 */
public final class M3ScopedNgramsDifferentialDump {
    private static final long[] TOKENS = {Long.MIN_VALUE, -55L, -1L, 0L, 1L, 42L, Long.MAX_VALUE};
    private static final long[] WIDE = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -4096L, -55L, -2L, -1L, 0L, 1L, 2L, 7L,
        42L, 1_000_003L, Long.MAX_VALUE - 1, Long.MAX_VALUE};
    private static final int[] ORACLE_K = {1, 2, 3, 5, 11, 4096};
    private static final int[] RANDOM_K = {1, 2, 3, 4, 7, 16, 4096};
    private static final int ORACLE_CORPORA = 384;
    private static final int RANDOM_CORPORA = 40;
    private static final int RANDOM_QUERIES_PER_CORPUS = 250;
    private static final int EDGE = 5;
    private final Constructor<?> scopeOf;
    private final Constructor<?> ngramsOf;
    private final Method observe;
    private final Method add;
    private final Method freeze;
    private final Method top;
    private final Method count;
    private final Method contextTotal;
    private final Method distinctTuples;
    private final Method entries;
    private final Method nextId;
    private final Method candidateCount;
    private final Method probability;
    private final Method tupleIds;
    private final PrintStream out;
    private long state = 0x1234abdc57ff09e1L;
    private long queries;
    private long refusals;
    private long lines;

    private M3ScopedNgramsDifferentialDump(Class<?> ngrams, PrintStream out) throws ReflectiveOperationException {
        Class<?> scope = Class.forName(ngrams.getName() + "$Scope");
        Class<?> snapshot = Class.forName(ngrams.getName() + "$Snapshot");
        Class<?> candidate = Class.forName(ngrams.getName() + "$Candidate");
        Class<?> tuple = Class.forName(ngrams.getName() + "$Tuple");
        scopeOf = scope.getConstructor(String.class, String.class, String.class, long.class, String.class);
        ngramsOf = ngrams.getConstructor(scope, int.class, int.class, int.class);
        observe = ngrams.getMethod("observe", scope, long[].class, long.class);
        add = ngrams.getMethod("add", scope, long[].class, long.class);
        freeze = ngrams.getMethod("freeze");
        top = snapshot.getMethod("top", long[].class, int.class);
        count = snapshot.getMethod("count", long[].class);
        contextTotal = snapshot.getMethod("contextTotal", long[].class);
        distinctTuples = snapshot.getMethod("distinctTuples");
        entries = snapshot.getMethod("entries");
        nextId = candidate.getMethod("nextId");
        candidateCount = candidate.getMethod("count");
        probability = candidate.getMethod("conditionalProbability");
        tupleIds = tuple.getMethod("ids");
        this.out = out;
    }

    public static void main(String[] args) throws ReflectiveOperationException {
        PrintStream out = new PrintStream(new BufferedOutputStream(System.out, 1 << 16), false, StandardCharsets.UTF_8);
        M3ScopedNgramsDifferentialDump dump = new M3ScopedNgramsDifferentialDump(Class.forName(args[0]), out);
        dump.oracleCorpora();
        dump.randomCorpora();
        dump.line("TOTAL oracle_corpora=" + ORACLE_CORPORA + " random_corpora=" + RANDOM_CORPORA
                + " random_queries=" + (RANDOM_CORPORA * RANDOM_QUERIES_PER_CORPUS) + " queries=" + dump.queries
                + " refusals=" + dump.refusals + " lines=" + dump.lines);
        out.flush();
    }

    /** Same generator as M3ScopedNgramsOracle: state, alphabet, corpus budgets, document and query draw order. */
    private void oracleCorpora() throws ReflectiveOperationException {
        for (int corpus = 0; corpus < ORACLE_CORPORA; corpus++) {
            Object scope = scopeOf.newInstance("oracle", "v1", "unorm", (long) corpus, "id");
            int order = 1 + corpus % 5;
            Object subject = ngramsOf.newInstance(scope, order, 30_000, 80);
            for (int d = 0; d < 4 + corpus % 9; d++) {
                int length = 3 + (int) (next() & 15L);
                long[] tokens = new long[length];
                for (int i = 0; i < length; i++) {
                    tokens[i] = TOKENS[(int) Long.remainderUnsigned(next(), TOKENS.length)];
                }
                long multiplicity = 1 + Long.remainderUnsigned(next(), 4);
                line("O " + corpus + " observe " + Arrays.toString(tokens) + " x" + multiplicity + " "
                        + call(observe, subject, scope, tokens, multiplicity));
            }
            Object snapshot = freeze.invoke(subject);
            line("O " + corpus + " distinct=" + distinctTuples.invoke(snapshot) + " " + entriesOf(snapshot, Integer.MAX_VALUE));
            for (int query = 0; query < 20; query++) {
                int length = query % (order + 2);
                long[] prefix = new long[length];
                for (int j = 0; j < length; j++) {
                    prefix[j] = TOKENS[(int) Long.remainderUnsigned(next(), TOKENS.length)];
                }
                if (query % 4 == 0 && prefix.length > 0) {
                    prefix[0] = Long.MIN_VALUE;
                }
                for (int k : ORACLE_K) {
                    line("O " + corpus + " q" + query + " k=" + k + " " + Arrays.toString(prefix) + " total="
                            + call(contextTotal, snapshot, (Object) prefix) + " " + topOf(snapshot, prefix, k));
                }
            }
            oracleProbes(corpus, scope, subject, snapshot);
        }
    }

    /** The oracle's fixed probes: absent denominator, null prefix, k and prefix-length budgets, frozen snapshot. */
    private void oracleProbes(int corpus, Object scope, Object subject, Object snapshot)
            throws ReflectiveOperationException {
        line("O " + corpus + " absent " + topOf(snapshot, new long[]{Long.MAX_VALUE, 77}, 1));
        line("O " + corpus + " null " + topOf(snapshot, null, 1));
        for (int k : new int[]{0, -1, 4097, Integer.MAX_VALUE}) {
            line("O " + corpus + " budget k=" + k + " " + topOf(snapshot, new long[0], k));
        }
        line("O " + corpus + " len64 " + topOf(snapshot, new long[64], 1));
        String before = topOf(snapshot, new long[0], 7);
        line("O " + corpus + " add " + call(add, subject, scope, new long[]{TOKENS[corpus % TOKENS.length]}, 3L));
        line("O " + corpus + " frozen=" + before.equals(topOf(snapshot, new long[0], 7)) + " refrozen "
                + topOf(freeze.invoke(subject), new long[0], 7));
    }

    /** Wider alphabet, longer documents, prefixes cut from observed documents, every k of RANDOM_K, refusals. */
    private void randomCorpora() throws ReflectiveOperationException {
        for (int corpus = 0; corpus < RANDOM_CORPORA; corpus++) {
            int order = 1 + (int) Long.remainderUnsigned(next(), 5);
            Object scope = scopeOf.newInstance("differential", "r" + corpus, "wide", 1000L + corpus, "signed-id");
            Object other = scopeOf.newInstance("differential", "r" + corpus, "wide", 1000L + corpus, "other");
            Object subject = ngramsOf.newInstance(scope, order, 60_000, 32);
            int documents = 20 + (int) Long.remainderUnsigned(next(), 31);
            long[][] corpusDocuments = new long[documents][];
            for (int d = 0; d < documents; d++) {
                int length = 1 + (int) Long.remainderUnsigned(next(), 32);
                long[] tokens = new long[length];
                for (int i = 0; i < length; i++) {
                    tokens[i] = WIDE[(int) Long.remainderUnsigned(next(), WIDE.length)];
                }
                corpusDocuments[d] = tokens;
                long multiplicity = 1 + Long.remainderUnsigned(next(), 3);
                line("D " + corpus + " observe " + Arrays.toString(tokens) + " x" + multiplicity + " "
                        + call(observe, subject, scope, tokens, multiplicity));
            }
            line("D " + corpus + " refuse scope " + call(observe, subject, other, new long[]{1}, 1L));
            line("D " + corpus + " refuse batch " + call(observe, subject, scope, new long[33], 1L));
            line("D " + corpus + " refuse multiplicity " + call(observe, subject, scope, new long[]{1}, 0L));
            line("D " + corpus + " refuse count " + call(add, subject, scope, new long[]{1}, 0L));
            line("D " + corpus + " refuse order " + call(add, subject, scope, new long[order + 1], 1L));
            line("D " + corpus + " refuse empty " + call(add, subject, scope, new long[0], 1L));
            Object snapshot = freeze.invoke(subject);
            line("D " + corpus + " order=" + order + " documents=" + documents + " distinct="
                    + distinctTuples.invoke(snapshot) + " " + entriesOf(snapshot, EDGE));
            for (int query = 0; query < RANDOM_QUERIES_PER_CORPUS; query++) {
                long[] prefix = randomPrefix(corpusDocuments, order);
                int k = RANDOM_K[(int) Long.remainderUnsigned(next(), RANDOM_K.length)];
                line("D " + corpus + " q" + query + " k=" + k + " " + Arrays.toString(prefix) + " total="
                        + call(contextTotal, snapshot, (Object) prefix) + " count=" + call(count, snapshot, (Object) prefix)
                        + " " + topOf(snapshot, prefix, k));
            }
            line("D " + corpus + " len64 " + topOf(snapshot, new long[64], 1));
            line("D " + corpus + " budget k=4097 " + topOf(snapshot, new long[0], 4097));
        }
    }

    /** Length 0..order (order itself has no continuation); half the prefixes are cut from an observed document. */
    private long[] randomPrefix(long[][] documents, int order) {
        long draw = next();
        int length = (int) Long.remainderUnsigned(draw, order + 1);
        if ((draw & (1L << 40)) != 0) {
            long[] document = documents[(int) Long.remainderUnsigned(next(), documents.length)];
            length = Math.min(length, document.length);
            int start = (int) Long.remainderUnsigned(next(), document.length - length + 1);
            return Arrays.copyOfRange(document, start, start + length);
        }
        long[] prefix = new long[length];
        for (int i = 0; i < length; i++) {
            prefix[i] = WIDE[(int) Long.remainderUnsigned(next(), WIDE.length)];
        }
        return prefix;
    }

    private String topOf(Object snapshot, long[] prefix, int k) throws ReflectiveOperationException {
        queries++;
        try {
            List<?> result = (List<?>) top.invoke(snapshot, new Object[]{prefix, k});
            StringBuilder text = new StringBuilder("T").append(result.size());
            for (Object candidate : result) {
                text.append(' ').append(nextId.invoke(candidate)).append(':').append(candidateCount.invoke(candidate))
                        .append(':').append(Long.toHexString(Double.doubleToRawLongBits((Double) probability.invoke(candidate))));
            }
            return text.toString();
        } catch (InvocationTargetException refused) {
            return refusal(refused);
        }
    }

    /** Deterministic export order: the first and last {@code edge} entries, every entry when edge is unbounded. */
    private String entriesOf(Object snapshot, int edge) throws ReflectiveOperationException {
        List<?> list = (List<?>) entries.invoke(snapshot);
        StringBuilder text = new StringBuilder("E").append(list.size());
        for (int i = 0; i < list.size(); i++) {
            if (i >= edge && i < list.size() - edge) {
                continue;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) list.get(i);
            text.append(' ').append(Arrays.toString((long[]) tupleIds.invoke(entry.getKey()))).append('=').append(entry.getValue());
        }
        return text.toString();
    }

    private String call(Method method, Object target, Object... arguments) throws ReflectiveOperationException {
        try {
            Object result = method.invoke(target, arguments);
            return result == null ? "OK" : String.valueOf(result);
        } catch (InvocationTargetException refused) {
            return refusal(refused);
        }
    }

    private String refusal(InvocationTargetException refused) {
        refusals++;
        Throwable cause = refused.getCause();
        return "R " + cause.getClass().getSimpleName() + " " + cause.getMessage();
    }

    private void line(String text) {
        out.print(text);
        out.print('\n');
        lines++;
    }

    private long next() {
        state ^= state << 13;
        state ^= state >>> 7;
        state ^= state << 17;
        return state;
    }
}
