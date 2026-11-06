// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.nlp;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Independent counted-tuple ranking oracle: never calls Snapshot.top to derive expectations. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-openrewrite-recipes/crates/m3-ngram-topk-admission-20261104/proof/M3NgramTopKOracle.java blob de4e7fabe240; recipe m3/nlp/recipe/relocate.py v1
public final class M3ScopedNgramsOracle {
    private static final long[] TOKENS = {Long.MIN_VALUE, -55L, -1L, 0L, 1L, 42L, Long.MAX_VALUE};
    private static final Comparator<M3ScopedNgrams.Candidate> RANK =
            Comparator.comparingLong(M3ScopedNgrams.Candidate::count).reversed()
                    .thenComparingLong(M3ScopedNgrams.Candidate::nextId);
    private static long checks = 0;
    private static long state = 0x1234abdc57ff09e1L;
    private static final MessageDigest DIGEST = digest();

    private M3ScopedNgramsOracle() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].equals("verify")) throw new IllegalArgumentException("verify required");
        for (int corpus = 0; corpus < 384; corpus++) {
            M3ScopedNgrams.Scope scope = new M3ScopedNgrams.Scope("oracle", "v1", "unorm", corpus, "id");
            int order = 1 + corpus % 5;
            var subject = new M3ScopedNgrams(scope, order, 30_000, 80);
            Map<List<Long>, Long> exact = new HashMap<>();
            Map<List<Long>, Long> totals = new HashMap<>();
            for (int d = 0; d < 4 + corpus % 9; d++) {
                int length = 3 + (int) (next() & 15L);
                long[] tokens = new long[length];
                for (int i = 0; i < length; i++) tokens[i] = TOKENS[(int) Long.remainderUnsigned(next(), TOKENS.length)];
                long multiplicity = 1 + Long.remainderUnsigned(next(), 4);
                subject.observe(scope, tokens, multiplicity);
                for (int i = 0; i < tokens.length; i++) {
                    for (int n = 1; n <= Math.min(order, tokens.length - i); n++) {
                        List<Long> gram = new ArrayList<>(n);
                        for (int j = 0; j < n; j++) gram.add(tokens[i + j]);
                        exact.merge(List.copyOf(gram), multiplicity, Math::addExact);
                        totals.merge(List.copyOf(gram.subList(0, n - 1)), multiplicity, Math::addExact);
                    }
                }
            }
            M3ScopedNgrams.Snapshot snapshot = subject.freeze();
            check(snapshot.distinctTuples() == exact.size(), "distinct count");
            for (var entry : exact.entrySet()) {
                long[] tuple = entry.getKey().stream().mapToLong(Long::longValue).toArray();
                check(snapshot.count(tuple) == entry.getValue(), "source count");
            }
            for (int query = 0; query < 20; query++) {
                int length = query % (order + 2);
                long[] prefix = new long[length];
                for (int j = 0; j < length; j++) prefix[j] = TOKENS[(int) Long.remainderUnsigned(next(), TOKENS.length)];
                if (query % 4 == 0 && prefix.length > 0) prefix[0] = Long.MIN_VALUE;
                int[] kValues = {1, 2, 3, 5, 11, 4096};
                for (int k : kValues) {
                    var expected = expected(exact, totals, prefix, k);
                    var actual = snapshot.top(prefix, k);
                    check(actual.equals(expected), "top mismatch corpus=" + corpus + " query=" + query + " k=" + k);
                    check(actual.equals(snapshot.top(prefix, k)), "repeat mismatch");
                    for (var candidate : actual) {
                        absorb(candidate.nextId()); absorb(candidate.count());
                        absorb(Double.doubleToLongBits(candidate.conditionalProbability()));
                    }
                    checks++;
                }
            }
            check(snapshot.top(new long[]{Long.MAX_VALUE, 77}, 1).isEmpty(), "absent denominator");
            try { snapshot.top(null, 1); throw new AssertionError("missing NPE"); } catch (NullPointerException ok) { checks++; }
            for (int k : new int[]{0, -1, 4097, Integer.MAX_VALUE}) {
                try { snapshot.top(new long[0], k); throw new AssertionError("missing budget IAE"); }
                catch (IllegalArgumentException ok) { checks++; }
            }
            try { snapshot.top(new long[64], 1); throw new AssertionError("missing length IAE"); }
            catch (IllegalArgumentException ok) { checks++; }
            // A frozen snapshot remains stable after the producer is mutated.
            var before = snapshot.top(new long[0], 7);
            subject.add(scope, new long[]{TOKENS[corpus % TOKENS.length]}, 3);
            check(before.equals(snapshot.top(new long[0], 7)), "frozen snapshot mutated");
        }
        parallelReadOnlyProof();
        long bytes = allocationProbe();
        System.out.println("M3_TOPK_ORACLE_PASS corpora=384 checks=" + checks
                + " digest=" + HexFormat.of().formatHex(DIGEST.digest()));
        System.out.println("M3_TOPK_ALLOCATION_PROBE bytesPerCall=" + bytes + " corpusPairs=30000 k=10");
    }

    private static List<M3ScopedNgrams.Candidate> expected(Map<List<Long>, Long> counts,
            Map<List<Long>, Long> totals, long[] prefix, int k) {
        List<Long> context = Arrays.stream(prefix).boxed().toList();
        long denominator = totals.getOrDefault(context, 0L);
        if (denominator == 0L) return List.of();
        var result = new ArrayList<M3ScopedNgrams.Candidate>();
        for (var entry : counts.entrySet()) {
            var key = entry.getKey();
            if (key.size() != prefix.length + 1) continue;
            boolean match = true;
            for (int i = 0; i < prefix.length; i++) {
                if (key.get(i).longValue() != prefix[i]) { match = false; break; }
            }
            if (match) {
                long frequency = entry.getValue();
                result.add(new M3ScopedNgrams.Candidate(key.get(prefix.length), frequency,
                        (double) frequency / denominator));
            }
        }
        result.sort(RANK);
        return List.copyOf(result.subList(0, Math.min(k, result.size())));
    }

    private static void parallelReadOnlyProof() throws Exception {
        var scope = new M3ScopedNgrams.Scope("parallel", "v1", "en", 1, "id");
        var model = new M3ScopedNgrams(scope, 2, 20_000, 10);
        for (int id = 0; id < 2000; id++) model.add(scope, new long[]{42, id}, 1 + id % 7);
        var frozen = model.freeze();
        var expected = frozen.top(new long[]{42}, 10);
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 320; i++) calls.add(() -> expected.equals(frozen.top(new long[]{42}, 10)));
            for (Future<Boolean> result : pool.invokeAll(calls)) check(result.get(), "concurrent query divergence");
        }
    }

    private static long allocationProbe() {
        var scope = new M3ScopedNgrams.Scope("alloc", "v1", "en", 1, "id");
        var model = new M3ScopedNgrams(scope, 2, 30010, 10);
        for (int i = 0; i < 30000; i++) model.add(scope, new long[]{42, i}, 1 + i % 17);
        var frozen = model.freeze();
        long[] prefix = {42};
        for (int i = 0; i < 200; i++) consume(frozen.top(prefix, 10));
        var bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) return -1;
        if (!bean.isThreadAllocatedMemoryEnabled()) bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        long before = bean.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 200; i++) consume(frozen.top(prefix, 10));
        long after = bean.getThreadAllocatedBytes(thread);
        return (after - before) / 200;
    }

    private static volatile long consumed;
    private static void consume(List<M3ScopedNgrams.Candidate> list) {
        consumed += list.size();
    }
    private static long next() {
        state ^= state << 13; state ^= state >>> 7; state ^= state << 17; return state;
    }
    private static void absorb(long value) {
        DIGEST.update(Long.toString(value).getBytes(StandardCharsets.US_ASCII)); DIGEST.update((byte) 0);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
}
