// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.jackson.tests;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3JsonLimits;
import jdk.m3.jackson.M3ObjectMapper;

/**
 * Opt-in 100 MB JSON acceptance workload; never run in the ordinary unit-test lane.
 *
 * <p>Reads an actual 100+ MB file, projects every Jackson token into the immutable
 * indexed AST, emits the entire AST without String materialization, then compares
 * the original and round-tripped Jackson token streams independently. This checks
 * duplicate field order, decoded Unicode and exact numeric lexemes.
 *
 * <p>Usage: java ... jdk.m3.jackson.tests.M3Json100MiBAcceptance INPUT.json NEW_OUTPUT.json
 * Memory and timing numbers are measurements, not fixed performance claims.
 */
public final class M3Json100MiBAcceptance {
    private static final long MINIMUM_BYTES = 100_000_000L;

    private M3Json100MiBAcceptance() { }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("expected input JSON path and NEW output JSON path");
        }
        Path input = Path.of(arguments[0]).toAbsolutePath().normalize();
        Path output = Path.of(arguments[1]).toAbsolutePath().normalize();
        if (!Files.isRegularFile(input)) throw new IOException("regular JSON input required");
        if (input.equals(output) || Files.exists(output)) {
            throw new IOException("output must be a different, previously absent file");
        }
        long inputBytes = Files.size(input);
        if (inputBytes < MINIMUM_BYTES) {
            throw new IllegalArgumentException("100 MB minimum: received " + inputBytes + " bytes");
        }

        M3ObjectMapper mapper = M3ObjectMapper.withLimits(M3JsonLimits.for100MiB());
        long gcBefore = gcCollections();
        long start = System.nanoTime();
        M3AST ast = mapper.readASTStreaming(input, StandardCharsets.UTF_8, inputBytes);
        long parsed = System.nanoTime();
        long gcParsed = gcCollections();
        if (ast.atomCount() < 1 || "FIELD".equals(ast.kindAt(ast.rootAtomIndex()))) {
            throw new AssertionError("invalid indexed root");
        }
        // Wrapper-free random access, including child coordinates shared by the DAG.
        int root = ast.rootAtomIndex();
        int children = ast.childCountAt(root);
        int stride = Math.max(1, children / 10_000);
        long sampled = 0;
        for (int index = 0; index < children; index += stride) {
            int child = ast.childAtomIndexAt(root, index);
            if (ast.kindAt(child) == null) throw new AssertionError("missing child kind");
            sampled++;
        }

        try (OutputStream sink = Files.newOutputStream(output,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            mapper.writeValue(sink, ast);
        }
        long written = System.nanoTime();
        long comparedTokens = compareTokens(input, output);
        long validated = System.nanoTime();
        long gcAfter = gcCollections();

        Runtime runtime = Runtime.getRuntime();
        long usedHeap = runtime.totalMemory() - runtime.freeMemory();
        long cumulativePoolPeakUpperBound = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == java.lang.management.MemoryType.HEAP
                    && pool.getPeakUsage() != null) {
                cumulativePoolPeakUpperBound += pool.getPeakUsage().getUsed();
            }
        }

        double parseSeconds = (parsed - start) / 1_000_000_000.0;
        double writeSeconds = (written - parsed) / 1_000_000_000.0;
        double verifySeconds = (validated - written) / 1_000_000_000.0;
        System.out.format(Locale.ROOT,
                "M3_JSON_100M_PASS inputBytes=%d outputBytes=%d atoms=%d childRefs=%d "
                + "rootChildren=%d indexedSamples=%d checkedTokens=%d "
                + "parseSeconds=%.3f parseMiBPerSecond=%.3f writeSeconds=%.3f "
                + "verifySeconds=%.3f heapUsedBytes=%d sumHeapPoolPeaksUpperBoundBytes=%d "
                + "gcDuringParse=%d gcTotal=%d%n",
                inputBytes, Files.size(output), ast.atomCount(), ast.childReferenceCount(),
                children, sampled, comparedTokens, parseSeconds,
                (inputBytes / 1_048_576.0) / parseSeconds,
                writeSeconds, verifySeconds, usedHeap, cumulativePoolPeakUpperBound,
                gcParsed - gcBefore, gcAfter - gcBefore);
    }

    private static long compareTokens(Path input, Path output) throws IOException {
        JsonFactory factory = new JsonFactory();
        try (JsonParser source = factory.createParser(input.toFile());
             JsonParser target = factory.createParser(output.toFile())) {
            long count = 0;
            while (true) {
                JsonToken left = source.nextToken();
                JsonToken right = target.nextToken();
                if (left != right) {
                    throw new AssertionError("JSON token mismatch at index " + count
                            + ": " + left + " versus " + right);
                }
                if (left == null) return count;
                if (left == JsonToken.FIELD_NAME || left == JsonToken.VALUE_STRING
                        || left.isNumeric()) {
                    String original = source.getText();
                    String rewritten = target.getText();
                    if (!original.equals(rewritten)) {
                        throw new AssertionError("JSON value/order mismatch at token " + count);
                    }
                }
                count++;
            }
        }
    }

    private static long gcCollections() {
        long total = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = bean.getCollectionCount();
            if (count >= 0) total += count;
        }
        return total;
    }
}
