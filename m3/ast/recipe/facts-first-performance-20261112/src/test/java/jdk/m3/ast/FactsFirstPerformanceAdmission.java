// SPDX-License-Identifier: Apache-2.0
package jdk.m3.ast;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Read-only, reproducible Java 21 timing profile of the exact product owner.
 * No performance ratio is treated as an assertion: measurements are machine-
 * and JIT-dependent. All results are checked against the public scan oracle.
 */
public final class FactsFirstPerformanceAdmission {
    private static final String EXPECTED_SOURCE_BLOB =
            "db14c75a439b3366517c2b344b41d8f4f7a390fc";
    private static final int QUERIES_PER_SAMPLE = 4096;
    private static final int SAMPLES = 5;
    private static volatile long blackhole;

    private FactsFirstPerformanceAdmission() { }

    private record Workload(String name, SyntaxIndex.Facts facts, String[] queries,
                            int factRows, int uniqueKeys) { }

    private static String[] propertyKeys(int count, boolean adversarial) {
        String[] keys = new String[count];
        for (int k = 0; k < count; k++) {
            if (!adversarial) {
                keys[k] = "key" + k;
            } else {
                // Every spelling has the same Java String hash, but distinct text.
                StringBuilder text = new StringBuilder(16);
                for (int bit = 0; bit < 8; bit++) {
                    text.append((k & (1 << bit)) == 0 ? "Aa" : "BB");
                }
                keys[k] = text.toString();
            }
        }
        if (adversarial) {
            for (int k = 1; k < count; k++) {
                if (keys[k].hashCode() != keys[0].hashCode()
                        || keys[k].equals(keys[0])) {
                    throw new AssertionError("incomplete collision corpus");
                }
            }
        }
        return keys;
    }

    private static Workload make(String name, int rows, int distinct, boolean adversarial) {
        SyntaxTree.Builder b = SyntaxTree.builder("m3-facts-perf", new byte[] {'x'});
        b.open("file", "", 0, SyntaxTree.NAMED);
        b.close(1);
        SyntaxTree tree = b.build();
        String[] keys = propertyKeys(distinct, adversarial);
        SyntaxIndex.FactBuilder writer = SyntaxIndex.facts(tree);
        for (int i = 0; i < rows; i++) {
            writer.add(0, "owner", keys[i % distinct], "value" + i);
        }
        SyntaxIndex.Facts facts = writer.freeze();
        facts.requireSource(tree);
        if (facts.size() != rows || facts.count(0) != rows) {
            throw new AssertionError("frozen sidecar size changed");
        }
        String[] queries = new String[QUERIES_PER_SAMPLE];
        for (int i = 0; i < queries.length; i++) {
            queries[i] = i % 8 == 0 ? "owner:not-present-" + i
                    : "owner:" + keys[(i * 37) % distinct];
        }
        return new Workload(name, facts, queries, rows, distinct);
    }

    /** The original public, source-order observable scan, not a hash shortcut. */
    private static String scan(SyntaxIndex.Facts facts, String key) {
        for (int ordinal = 0, count = facts.count(0); ordinal < count; ordinal++) {
            if (facts.key(0, ordinal).equals(key)) {
                return facts.value(0, ordinal);
            }
        }
        return null;
    }

    private static void proveParity(Workload work) {
        for (String query : work.queries) {
            String reference = scan(work.facts, query);
            String observed = work.facts.first(0, query);
            if (!Objects.equals(reference, observed)) {
                throw new AssertionError("first-value parity: workload=" + work.name
                        + " query=" + query + " expected=" + reference + " got=" + observed);
            }
        }
        for (int i = 0; i < work.uniqueKeys; i++) {
            String key = work.facts.key(0, i);
            if (!work.facts.value(0, i).equals(work.facts.first(0, key))) {
                throw new AssertionError("earliest duplicate changed: " + key);
            }
        }
    }

    private static long time(Workload work, boolean indexed, int iterations) {
        long checksum = 0;
        int length = work.queries.length;
        long started = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String key = work.queries[i % length];
            String value = indexed ? work.facts.first(0, key) : scan(work.facts, key);
            checksum = checksum * 31 + (value == null ? 0 : value.hashCode());
        }
        long elapsed = System.nanoTime() - started;
        blackhole ^= checksum;
        return elapsed;
    }

    private static long median(long[] values) {
        Arrays.sort(values);
        return values[values.length / 2];
    }

    private static void measure(Workload work) {
        proveParity(work); // also warms the optional index when admitted
        time(work, false, 4096);
        time(work, true, 4096);
        long[] scanSamples = new long[SAMPLES], candidateSamples = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            if ((i & 1) == 0) {
                scanSamples[i] = time(work, false, QUERIES_PER_SAMPLE);
                candidateSamples[i] = time(work, true, QUERIES_PER_SAMPLE);
            } else {
                candidateSamples[i] = time(work, true, QUERIES_PER_SAMPLE);
                scanSamples[i] = time(work, false, QUERIES_PER_SAMPLE);
            }
        }
        double scanNs = median(scanSamples) / (double) QUERIES_PER_SAMPLE;
        double candidateNs = median(candidateSamples) / (double) QUERIES_PER_SAMPLE;
        System.out.printf(Locale.ROOT, "%s\t%d\t%d\t%d\t%.1f\t%.1f\t%.2f%n",
                work.name, work.factRows, work.uniqueKeys, QUERIES_PER_SAMPLE,
                scanNs, candidateNs, scanNs / Math.max(1.0, candidateNs));
    }

    private static void requireExactSource(Path file) throws Exception {
        if (!Files.isRegularFile(file) || Files.isSymbolicLink(file)) {
            throw new IllegalArgumentException("missing exact SyntaxIndex source");
        }
        byte[] bytes = Files.readAllBytes(file);
        MessageDigest git = MessageDigest.getInstance("SHA-1");
        git.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.US_ASCII));
        String observed = HexFormat.of().formatHex(git.digest(bytes));
        if (!EXPECTED_SOURCE_BLOB.equals(observed)) {
            throw new IllegalArgumentException("source drift: " + observed);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: FactsFirstPerformanceAdmission <SyntaxIndex.java>");
        }
        requireExactSource(Path.of(args[0]));
        System.out.println("M3_FACTS_FIRST_PROFILE sourceGitBlob=" + EXPECTED_SOURCE_BLOB
                + " samples=" + SAMPLES + " java=" + Runtime.version());
        System.out.println("workload\tfacts\tkeys\tqueries\tscan_ns_per_query\tfirst_ns_per_query\tratio");
        measure(make("small-cold", 128, 8, false));
        measure(make("normal-repeated", 4096, 64, false));
        measure(make("high-cardinality", 4096, 1024, false));
        measure(make("equal-hash-adversarial", 4096, 256, true));
        if (blackhole == Long.MIN_VALUE) System.out.print("");
        System.out.println("M3_FACTS_FIRST_PROFILE_COMPLETE correctness=PASS performance=MEASURE_ONLY");
    }
}
