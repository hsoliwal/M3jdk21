/* SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fastsearch/jdk/internal/m3/fastsearch/knowledge/index/HybridSearchKernelChecks.java (blob ab43c5d55178) by M3_SEARCH_RELOCATE v3.
package jdk.m3.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/** Dependency-free checks shared by the ordinary JUnit gate and JNI parity gate. */
public final class HybridSearchKernelChecks {
    private HybridSearchKernelChecks() {
    }

    public static void main(String[] args) throws Exception {
        verifySelector(M3TopKSelector.javaSelector());
        verifyOptions();
        verifyFusion();
        verifyCache();
        System.out.println("PASS: Java top-k 25000 randomized cases plus edge cases; "
                + "10000 legacy-budget comparisons; 3000 weighted-fusion oracles; "
                + "LRU, failure, close and concurrency checks");
    }

    public static void verifySelector(M3TopKSelector selector) throws Exception {
        checkSelection(selector, new int[0], new float[0], Integer.MAX_VALUE);
        checkSelection(selector, new int[] {4}, new float[] {-0.0f}, 1);
        checkSelection(selector, new int[] {9, 2, 2, -7, Integer.MIN_VALUE, Integer.MAX_VALUE},
                new float[] {0.0f, -0.0f, 0.0f, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MIN_VALUE}, 6);
        checkSelection(selector, new int[] {7, 7, 7, 1, 1, 9}, new float[] {2, 2, 2, 2, 2, 2}, 4);
        expect(NullPointerException.class, () -> selector.select(null, new float[0], 1));
        expect(NullPointerException.class, () -> selector.select(new int[0], null, 1));
        expect(IllegalArgumentException.class,
                () -> selector.select(new int[1], new float[0], 1));
        expect(IllegalArgumentException.class,
                () -> selector.select(new int[0], new float[0], 0));
        for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            expect(IllegalArgumentException.class,
                    () -> selector.select(new int[] {1, 2}, new float[] {1.0f, invalid}, 1));
        }
        Random random = new Random(0x5A17C0DEL);
        for (int trial = 0; trial < 25000; trial++) {
            int size = random.nextInt(384);
            int[] ids = new int[size];
            float[] scores = new float[size];
            for (int i = 0; i < size; i++) {
                ids[i] = trial % 3 == 0 ? random.nextInt(9) - 4 : random.nextInt();
                float arbitrary = Float.intBitsToFloat(random.nextInt());
                scores[i] = trial % 2 == 0 ? random.nextInt(7) - 3
                        : Float.isFinite(arbitrary) ? arbitrary : 0.0f;
            }
            int limit = trial % 11 == 0 ? Integer.MAX_VALUE : 1 + random.nextInt(size + 3);
            checkSelection(selector, ids, scores, limit);
        }
        int[] largeIds = IntStream.range(0, 100000).map(i -> 100000 - i).toArray();
        float[] largeScores = new float[largeIds.length];
        for (int i = 0; i < largeScores.length; i++) {
            largeScores[i] = i % 17;
        }
        checkSelection(selector, largeIds, largeScores, 32);
        try (var executor = Executors.newFixedThreadPool(6)) {
            var jobs = IntStream.range(0, 24).mapToObj(seed -> executor.submit(() -> {
                for (int limit = 1; limit < 32; limit++) {
                    checkSelection(selector, largeIds, largeScores, limit);
                }
                return seed;
            })).toList();
            for (var job : jobs) {
                job.get(30, TimeUnit.SECONDS);
            }
        }
    }

    public static void verifyOptions() throws Exception {
        M3HybridSearchOptions defaults = M3HybridSearchOptions.defaults(5);
        require(defaults.candidateCount(100) == 20, "default expansion");
        require(defaults.candidateCount(2) == 2, "corpus bound");
        require(defaults.candidateCount(0) == 0, "empty corpus");
        require(new M3HybridSearchOptions(5, 1, 1, 0).candidateCount(100) == 5, "unit expansion");
        require(new M3HybridSearchOptions(Integer.MAX_VALUE, Integer.MAX_VALUE, 1, 0)
                .candidateCount(Integer.MAX_VALUE) == Integer.MAX_VALUE, "wide multiplication");
        Random random = new Random(0xB0D6E7L);
        for (int i = 0; i < 10000; i++) {
            int limit = 1 + random.nextInt(Integer.MAX_VALUE);
            int documents = random.nextInt(Integer.MAX_VALUE);
            int legacy = Math.min(documents,
                    Math.max(limit, Math.min(Integer.MAX_VALUE / 4, limit * 4)));
            require(M3HybridSearchOptions.defaults(limit).candidateCount(documents) == legacy,
                    "legacy saturation contract");
        }
        expect(IllegalArgumentException.class, () -> M3HybridSearchOptions.defaults(0));
        expect(IllegalArgumentException.class, () -> new M3HybridSearchOptions(1, 0, 1, 1));
        expect(IllegalArgumentException.class, () -> new M3HybridSearchOptions(1, 1, 0, -0.0f));
        for (float invalid : new float[] {-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> new M3HybridSearchOptions(1, 1, invalid, 1));
            expect(IllegalArgumentException.class, () -> new M3HybridSearchOptions(1, 1, 1, invalid));
        }
        expect(IllegalArgumentException.class, () -> defaults.candidateCount(-1));
    }

    public static void verifyFusion() {
        Random random = new Random(0xF0510AL);
        for (int trial = 0; trial < 3000; trial++) {
            int[] lexical = random.ints(80, -120, 120).distinct().toArray();
            int[] vector = random.ints(90, -120, 120).distinct().toArray();
            float lexicalWeight = trial % 5 == 0 ? 0.0f : trial % 3 == 0 ? Float.MAX_VALUE : 1.0f;
            float vectorWeight = trial % 7 == 0 && lexicalWeight != 0.0f ? 0.0f : 0.375f;
            M3HybridSearchOptions options = new M3HybridSearchOptions(1 + trial % 200, 4, lexicalWeight, vectorWeight);
            var actual = M3ReciprocalRankFusion.rank(lexical, vector, options, M3TopKSelector.javaSelector());
            java.util.Map<Integer, Float> expected = new java.util.HashMap<>();
            int[][] lanes = {lexical, vector};
            float[] weights = {lexicalWeight, vectorWeight};
            for (int lane = 0; lane < lanes.length; lane++) {
                if (weights[lane] == 0.0f) {
                    continue;
                }
                for (int rank = 0; rank < lanes[lane].length; rank++) {
                    float contribution = weights[lane] * (1.0f / (60 + rank + 1.0f));
                    expected.merge(lanes[lane][rank], contribution, Float::sum);
                }
            }
            var ordered = expected.entrySet().stream()
                    .sorted(Comparator.<java.util.Map.Entry<Integer, Float>>comparingDouble(e -> e.getValue())
                            .reversed().thenComparingInt(e -> e.getKey()))
                    .limit(options.limit()).toList();
            require(actual.size() == ordered.size(), "fused result size");
            for (int i = 0; i < actual.size(); i++) {
                require(actual.get(i).documentId() == ordered.get(i).getKey(), "fused identity order");
                require(Float.floatToIntBits(actual.get(i).score())
                        == Float.floatToIntBits(ordered.get(i).getValue()), "fused score bits");
            }
        }
    }

    public static void verifyCache() throws Exception {
        AtomicInteger loads = new AtomicInteger();
        try (var cache = new M3PreparationCache<String>(2, 3)) {
            M3PreparationCache.Loader<String> loader = key -> key + loads.incrementAndGet();
            String a = cache.get("a", loader);
            cache.get("b", loader);
            require(cache.get("a", loader) == a, "hit identity");
            cache.get("c", loader);
            require(cache.get("a", loader) == a, "access-order eviction");
            cache.get("b", loader);
            require(cache.stats().equals(new M3QueryPreparationStats(2, 4, 2, 2)), "LRU stats");
            cache.get("oversized", loader);
            cache.get("oversized", loader);
            require(cache.stats().entries() == 2 && loads.get() == 6, "oversized bypass");
        }
        try (var disabled = new M3PreparationCache<String>(0, 10)) {
            disabled.get("a", key -> "first");
            require(disabled.get("a", key -> "second").equals("second"), "disabled cache");
            require(disabled.stats().entries() == 0, "zero retention");
        }
        try (var failures = new M3PreparationCache<String>(2, 5)) {
            expect(IOException.class, () -> failures.get("bad", key -> { throw new IOException("expected"); }));
            expect(NullPointerException.class, () -> failures.get("bad", key -> null));
            require(failures.stats().entries() == 0, "failed value not retained");
            require(failures.get("bad", key -> "recovered").equals("recovered"), "retry succeeds");
        }
        expect(IllegalArgumentException.class, () -> new M3PreparationCache<String>(-1, 2));
        expect(IllegalArgumentException.class, () -> new M3PreparationCache<String>(1, -2));
        verifyConcurrentPublication();
        verifyCloseDuringPreparation();
    }

    private static void verifyConcurrentPublication() throws Exception {
        try (var cache = new M3PreparationCache<Object>(2, 10);
                var executor = Executors.newFixedThreadPool(8)) {
            CountDownLatch started = new CountDownLatch(8);
            CountDownLatch release = new CountDownLatch(1);
            var jobs = IntStream.range(0, 8).mapToObj(i -> executor.submit(() -> cache.get("same", key -> {
                started.countDown();
                await(release);
                return new Object();
            }))).toList();
            try {
                require(started.await(10, TimeUnit.SECONDS), "compilation must not hold cache monitor");
            } finally {
                release.countDown();
            }
            Object retained = jobs.getFirst().get(10, TimeUnit.SECONDS);
            for (var job : jobs) {
                require(job.get(10, TimeUnit.SECONDS) == retained, "single published value");
            }
            require(cache.stats().entries() == 1, "one retained entry");
        }
    }

    private static void verifyCloseDuringPreparation() throws Exception {
        M3PreparationCache<Object> cache = new M3PreparationCache<>(1, 10);
        try (var executor = Executors.newSingleThreadExecutor()) {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            var job = executor.submit(() -> cache.get("late", key -> {
                started.countDown();
                await(release);
                return new Object();
            }));
            try {
                require(started.await(10, TimeUnit.SECONDS), "loader started");
                cache.close();
            } finally {
                release.countDown();
            }
            try {
                job.get(10, TimeUnit.SECONDS);
                throw new AssertionError("late publication accepted");
            } catch (ExecutionException expected) {
                require(expected.getCause() instanceof IllegalStateException, "closed cache fails explicitly");
            }
            require(cache.stats().entries() == 0, "no post-close retention");
            expect(IllegalStateException.class, () -> cache.get("x", key -> new Object()));
        } finally {
            cache.close();
        }
    }

    private static void checkSelection(M3TopKSelector selector, int[] ids, float[] scores, int limit) {
        int[] originalIds = ids.clone();
        float[] originalScores = scores.clone();
        int[] expected = IntStream.range(0, ids.length).boxed()
                .sorted(Comparator.<Integer>comparingDouble(i -> scores[i]).reversed()
                        .thenComparingInt(i -> ids[i]).thenComparingInt(i -> i))
                .limit(limit).mapToInt(Integer::intValue).toArray();
        int[] actual = selector.select(ids, scores, limit);
        require(Arrays.equals(expected, actual), "top-k differs from full-sort oracle");
        require(Arrays.equals(ids, originalIds), "document IDs mutated");
        require(Arrays.equals(scores, originalScores), "scores mutated");
    }

    private static void await(CountDownLatch latch) throws IOException {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IOException("test latch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException(exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends Throwable> type, CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (Throwable actual) {
            if (type.isInstance(actual)) {
                return;
            }
            throw new AssertionError("expected " + type.getName() + ", got " + actual, actual);
        }
        throw new AssertionError("expected " + type.getName());
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
