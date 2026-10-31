// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/*
 * Resource observation contract, written before implementation:
 * - A standalone main accepts one fresh output directory and creates resource-probe.json.
 * - Four batches of six deterministic finite JSON documents each contain 32 occurrences
 *   of the same subtree and 48 distinct text values. Each document passes through an
 *   M3File String snapshot, M3AST parsing, and Jackson JSON output with default limits.
 * - Only the latest AST is deliberately retained. Primitive counters and observations
 *   survive batches; source snapshots, output strings, and previous ASTs do not.
 * - Sample heap/non-heap use, current-thread CPU, process CPU, and collector counters
 *   before work, after each batch, after a GC request with one retained AST, and after
 *   releasing it and making another GC request. No sleep, forced completion, or retry
 *   loop is used. Weak-reference clearing is an observation, never a pass condition.
 * - GC requests need not complete collection. Samples include VM, mapper, source
 *   construction, JIT, and probe overhead; they do not measure an AST retained size,
 *   a leak rate, peak process memory, or comparative performance. External process/NMT
 *   receipts supply separate evidence. The fixed modest workload is not a benchmark.
 * - No verification command or execution accompanies authoring this source.
 */
package jdk.m3.jackson.tests;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jdk.m3.jackson.M3AST;
import jdk.m3.jackson.M3ASTAtom;
import jdk.m3.jackson.M3File;
import jdk.m3.jackson.M3ObjectMapper;

/** Bounded resource observations for a deterministic source/file/AST/output cohort. */
public final class M3JacksonResourceProbe {
    private static final int BATCHES = 4;
    private static final int DOCUMENTS_PER_BATCH = 6;
    private static final int REPEATED_OCCURRENCES = 32;
    private static final int UNIQUE_TEXT_VALUES = 48;
    private static final String REPEATED_SUBTREE =
            "{\"tag\":\"shared\",\"numbers\":[1,2,-0.0,3.25e+2],\"flags\":[null,true,false]}";

    private final M3ObjectMapper mapper = new M3ObjectMapper();
    private final Sampler sampler = new Sampler();
    private M3AST heldDocument;
    private int documents;
    private long inputTextUnits;
    private long outputTextUnits;
    private long uniqueAtoms;
    private long childReferences;
    private long distinctTextAtoms;
    private long repeatedChildOccurrences;
    private long checksum;

    private M3JacksonResourceProbe() { }

    /** Accept exactly one output directory; refuse replacement of an existing receipt. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: M3JacksonResourceProbe OUTPUT_DIRECTORY");
        }
        new M3JacksonResourceProbe().run(Path.of(args[0]));
    }

    private void run(Path outputDirectory) throws Exception {
        Path directory = outputDirectory.toAbsolutePath().normalize();
        Files.createDirectories(directory);
        Path receipt = directory.resolve("resource-probe.json");
        if (Files.exists(receipt)) {
            throw new IllegalArgumentException("resource receipt already exists: " + receipt);
        }

        List<Map<String, Object>> samples = new ArrayList<>();
        samples.add(sampler.sample("before_workload", false, 0, null));
        for (int batch = 0; batch < BATCHES; batch++) {
            for (int item = 0; item < DOCUMENTS_PER_BATCH; item++) {
                heldDocument = null;
                heldDocument = exercise(batch * DOCUMENTS_PER_BATCH + item);
            }
            samples.add(sampler.sample("after_batch_" + (batch + 1), false, 1, null));
        }

        Map<String, Object> heldShape = new LinkedHashMap<>();
        heldShape.put("uniqueAtoms", heldDocument.atomCount());
        heldShape.put("childReferences", heldDocument.childReferenceCount());
        heldShape.put("structuralHash64Unsigned", Long.toUnsignedString(heldDocument.structuralHash64()));
        WeakReference<M3AST> releasedDocument = new WeakReference<>(heldDocument);

        System.gc();
        samples.add(sampler.sample("after_gc_request_with_one_document", true, 1,
                releasedDocument.refersTo(null)));
        Reference.reachabilityFence(heldDocument);
        heldDocument = null;
        System.gc();
        samples.add(sampler.sample("after_gc_request_after_release", true, 0,
                releasedDocument.refersTo(null)));

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "m3-jackson-resource-probe-v1");
        report.put("purpose", "bounded observations; not a benchmark or retained-size measurement");
        report.put("environment", environment());
        report.put("cohort", cohort());
        report.put("totals", totals());
        report.put("lastDeliberatelyHeldDocument", heldShape);
        report.put("samples", samples);
        report.put("interpretation", List.of(
                "GC request samples are taken after System.gc returns; collection is not guaranteed.",
                "Weak-reference clearing observes only the last AST handle and is not a pass condition.",
                "Heap readings include VM, mapper, source construction, JIT, and probe scaffolding.",
                "CPU and collector counters are cumulative observations, not throughput comparisons.",
                "Null CPU readings and collector counters of -1 mean the metric is unavailable.",
                "Samples do not establish object retained size, leak rate, or peak process/native memory.",
                "External time/NMT receipts provide separate process/native observations.",
                "Report serialization and file output occur after the final resource sample."));
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        Files.writeString(receipt, json + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.println(json);
    }

    /** Locals other than the returned AST leave scope before the next observation. */
    private M3AST exercise(int document) throws Exception {
        String source = source(document);
        M3File file = M3File.of(URI.create("memory:/m3-jackson-resource/document-" + document + ".json"),
                source);
        M3AST ast = mapper.readAST(file);
        String output = mapper.writeValueAsString(ast);

        M3ASTAtom repeated = ast.root().child(1).child(0);
        M3ASTAtom unique = ast.root().child(2).child(0);
        if (repeated.childCount() != REPEATED_OCCURRENCES
                || unique.childCount() != UNIQUE_TEXT_VALUES) {
            throw new IllegalStateException("resource cohort shape was not preserved");
        }
        int sharedChild = repeated.childAtomIndex(0);
        for (int occurrence = 0; occurrence < REPEATED_OCCURRENCES; occurrence++) {
            if (repeated.childAtomIndex(occurrence) != sharedChild) {
                throw new IllegalStateException("resource cohort did not exercise subtree sharing");
            }
        }
        HashSet<Integer> distinct = new HashSet<>();
        for (int index = 0; index < UNIQUE_TEXT_VALUES; index++) {
            distinct.add(unique.childAtomIndex(index));
        }
        if (distinct.size() != UNIQUE_TEXT_VALUES) {
            throw new IllegalStateException("resource cohort did not preserve distinct text atoms");
        }

        documents++;
        inputTextUnits += source.length();
        outputTextUnits += output.length();
        uniqueAtoms += ast.atomCount();
        childReferences += ast.childReferenceCount();
        distinctTextAtoms += distinct.size();
        repeatedChildOccurrences += repeated.childCount();
        // Deliberately wrapping, noncryptographic observation sink, not a source identity.
        checksum = Long.rotateLeft(checksum, 7) ^ ast.structuralHash64()
                ^ output.hashCode() ^ file.sourceSha256().hashCode();
        return ast;
    }

    private static String source(int document) {
        StringBuilder source = new StringBuilder(8_192);
        source.append("{\"document\":").append(document).append(",\"repeated\":[");
        for (int occurrence = 0; occurrence < REPEATED_OCCURRENCES; occurrence++) {
            if (occurrence != 0) source.append(',');
            source.append(REPEATED_SUBTREE);
        }
        source.append("],\"unique\":[");
        for (int index = 0; index < UNIQUE_TEXT_VALUES; index++) {
            if (index != 0) source.append(',');
            source.append("\"document-").append(document).append("-entry-").append(index)
                    .append("-abcdefghijklmnopqrstuvwxyz0123456789-").append((char) 0x03bb)
                    .append("-line\\nend\"");
        }
        return source.append("]}").toString();
    }

    private Map<String, Object> environment() {
        Map<String, Object> environment = new LinkedHashMap<>();
        environment.put("javaRuntimeVersion", System.getProperty("java.runtime.version"));
        environment.put("javaVmName", System.getProperty("java.vm.name"));
        environment.put("javaVmVersion", System.getProperty("java.vm.version"));
        environment.put("osName", System.getProperty("os.name"));
        environment.put("osArch", System.getProperty("os.arch"));
        environment.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        environment.put("jacksonVersion", mapper.version().toString());
        environment.put("currentThreadCpuTimeSupported", sampler.threads.isCurrentThreadCpuTimeSupported());
        environment.put("threadCpuTimeEnabled", sampler.threads.isCurrentThreadCpuTimeSupported()
                && sampler.threads.isThreadCpuTimeEnabled());
        environment.put("processCpuSource", "ProcessHandle.Info.totalCpuDuration");
        return environment;
    }

    private static Map<String, Object> cohort() {
        Map<String, Object> cohort = new LinkedHashMap<>();
        cohort.put("algorithm", "finite-json-repeated-subtree-and-distinct-text-v1");
        cohort.put("batches", BATCHES);
        cohort.put("documentsPerBatch", DOCUMENTS_PER_BATCH);
        cohort.put("warmupDocuments", 0);
        cohort.put("repeatedSubtreeOccurrencesPerDocument", REPEATED_OCCURRENCES);
        cohort.put("distinctTextValuesPerDocument", UNIQUE_TEXT_VALUES);
        cohort.put("entryPoints", "M3File.of -> M3ObjectMapper.readAST -> writeValueAsString");
        cohort.put("limits", "M3JsonLimits.DEFAULT");
        cohort.put("deliberatelyHeldDocumentsBetweenBatches", 1);
        return cohort;
    }

    private Map<String, Object> totals() {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("documents", documents);
        totals.put("inputUtf16Units", inputTextUnits);
        totals.put("outputUtf16Units", outputTextUnits);
        totals.put("sumOfDocumentUniqueAtoms", uniqueAtoms);
        totals.put("sumOfDocumentChildReferences", childReferences);
        totals.put("distinctTextAtomsAcrossDocumentPools", distinctTextAtoms);
        totals.put("sharedSubtreeChildOccurrences", repeatedChildOccurrences);
        totals.put("observationChecksumUnsigned", Long.toUnsignedString(checksum));
        return totals;
    }

    private static final class Sampler {
        private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        private final List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        private final long startNanos = System.nanoTime();

        Map<String, Object> sample(String phase, boolean gcRequested,
                                   int deliberatelyHeldDocuments, Boolean lastDocumentCleared) {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("phase", phase);
            sample.put("elapsedWallNanos", System.nanoTime() - startNanos);
            sample.put("gcRequestedImmediatelyBeforeSample", gcRequested);
            sample.put("deliberatelyHeldDocuments", deliberatelyHeldDocuments);
            sample.put("lastDocumentWeakReferenceCleared", lastDocumentCleared);
            sample.put("heap", usage(memory.getHeapMemoryUsage()));
            sample.put("nonHeap", usage(memory.getNonHeapMemoryUsage()));
            boolean cpuAvailable = threads.isCurrentThreadCpuTimeSupported() && threads.isThreadCpuTimeEnabled();
            sample.put("currentThreadCpuTimeNanos", cpuAvailable ? available(threads.getCurrentThreadCpuTime()) : null);
            sample.put("currentThreadUserTimeNanos", cpuAvailable ? available(threads.getCurrentThreadUserTime()) : null);
            sample.put("processCpuTimeNanos", ProcessHandle.current().info().totalCpuDuration()
                    .map(duration -> duration.toNanos()).orElse(null));
            List<Map<String, Object>> collectorSamples = new ArrayList<>();
            for (GarbageCollectorMXBean collector : collectors) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("name", collector.getName());
                values.put("collectionCount", collector.getCollectionCount());
                values.put("collectionTimeMillis", collector.getCollectionTime());
                collectorSamples.add(values);
            }
            sample.put("garbageCollectors", collectorSamples);
            return sample;
        }

        private static Long available(long value) {
            return value < 0 ? null : value;
        }

        private static Map<String, Object> usage(MemoryUsage usage) {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("usedBytes", usage.getUsed());
            values.put("committedBytes", usage.getCommitted());
            values.put("maxBytes", usage.getMax());
            return values;
        }
    }
}
