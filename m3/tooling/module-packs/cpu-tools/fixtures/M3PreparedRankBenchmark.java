// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.CpuTools;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jdk.m3.io.M3File;

/**
 * Honest repeat-query experiment: file admission is excluded from both timed
 * paths, but prepared-index construction is included in the cached total.
 * A single JVM run is an observation, not a stable performance guarantee.
 */
public final class M3PreparedRankBenchmark {
    private static volatile long observation;
    private M3PreparedRankBenchmark() { }

    public static void main(String[] arguments) throws Exception {
        int count = arguments.length > 0 ? Integer.parseInt(arguments[0]) : 96;
        int iterations = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 300;
        if (count < 1 || count > 256 || iterations < 1 || iterations > 20_000) {
            throw new IllegalArgumentException("benchmark geometry budget");
        }
        Path directory = Files.createTempDirectory("m3-prepared-rank-benchmark-");
        List<Path> created = new ArrayList<>();
        try {
            List<M3File> corpus = new ArrayList<>(count);
            for (int id = 0; id < count; id++) {
                Path file = directory.resolve("document-" + id + ".txt");
                StringBuilder content = new StringBuilder(1024);
                for (int term = 0; term < 32; term++) {
                    content.append("alpha beta gamma word").append(id % 17)
                            .append(" needle").append(term % 11)
                            .append(" domain").append((id + term) % 23).append(' ');
                }
                Files.writeString(file, content, StandardCharsets.UTF_8);
                created.add(file);
                corpus.add(M3File.load(file));
            }
            List<List<String>> queries = List.of(List.of("alpha"), List.of("beta", "gamma"),
                    List.of("needle3"), List.of("domain7"), List.of("missing"));
            var warmPlan = CpuTools.prepareRanking(corpus);
            for (int i = 0; i < 12; i++) {
                List<String> query = queries.get(i % queries.size());
                observation ^= digest(warmPlan.rank(query, 10));
                observation ^= digest(CpuTools.rank(corpus, query, 10));
            }

            long prepareStart = System.nanoTime();
            var prepared = CpuTools.prepareRanking(corpus);
            long prepareEnd = System.nanoTime();
            long cachedStart = System.nanoTime();
            long cachedDigest = 0;
            for (int i = 0; i < iterations; i++) {
                cachedDigest += digest(prepared.rank(queries.get(i % queries.size()), 10));
            }
            long cachedEnd = System.nanoTime();

            long directStart = System.nanoTime();
            long directDigest = 0;
            for (int i = 0; i < iterations; i++) {
                directDigest += digest(CpuTools.rank(corpus, queries.get(i % queries.size()), 10));
            }
            long directEnd = System.nanoTime();
            if (cachedDigest != directDigest) throw new AssertionError("prepared/one-shot score mismatch");
            for (List<String> query : queries) {
                var cached = prepared.rank(query, 10);
                var direct = CpuTools.rank(corpus, query, 10);
                if (cached.totalMatches() != direct.totalMatches()
                        || cached.complete() != direct.complete() || !cached.hits().equals(direct.hits())) {
                    throw new AssertionError("independent query parity mismatch");
                }
            }
            observation = cachedDigest;
            long prepNs = prepareEnd - prepareStart;
            long cachedNs = cachedEnd - cachedStart;
            long directNs = directEnd - directStart;
            System.out.printf(Locale.ROOT,
                    "M3_PREPARED_RANK_BENCH documents=%d queries=%d prepare_ns=%d "
                    + "prepared_query_ns=%d prepared_total_ns=%d one_shot_ns=%d "
                    + "one_shot_over_prepared_total=%.3f parity=PASS checksum=%d%n",
                    count, iterations, prepNs, cachedNs, prepNs + cachedNs, directNs,
                    directNs / (double) (prepNs + cachedNs), observation);
        } finally {
            for (Path file : created) Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }

    private static long digest(CpuTools.Ranking ranking) {
        long value = ranking.totalMatches() * 31L + ranking.size();
        if (ranking.size() != 0) {
            var first = ranking.hit(0);
            value = (value * 31L + first.documentId()) ^ Double.doubleToLongBits(first.score());
        }
        return value;
    }
}
