// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Build-time harvester: scans a jar set (and optionally {@code jrt:/}) for constant regex
 * operands, analyzes each distinct (pattern, flags) key against the stock JDK node tree and
 * emits the {@code M3CTRX01} image, a key table with measured per-key cost, a deterministic
 * input sample of harvested ldc strings, and a receipt. CPU only; one JVM.
 *
 * <pre>
 * java --add-opens java.base/java.util.regex=ALL-UNNAMED -cp ... \
 *   com.m3.compiletime.M3ConstantRegexHarvester --out DIR [--jdk] [--jars LIST] [--sample-mod 128]
 * </pre>
 */
public final class M3ConstantRegexHarvester {
    private static final int ROUNDS = 7;
    private static final int MAX_INPUT_UNITS = 256;

    private M3ConstantRegexHarvester() {}

    public static void main(String[] args) throws IOException {
        Path out = Path.of(M3CompileTimeStringInventory.arg(args, "--out", "target/constant-regex"));
        Files.createDirectories(out);
        String jars = M3CompileTimeStringInventory.arg(args, "--jars", null);
        boolean jdk = List.of(args).contains("--jdk");
        int sampleMod = Integer.parseInt(M3CompileTimeStringInventory.arg(args, "--sample-mod", "128"));
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        Map<String, Long> receipt = new TreeMap<>();
        M3InventoryState state = new M3InventoryState();
        M3CompileTimeStringInventory scanner = new M3CompileTimeStringInventory(state, null, true);
        long cpu0 = mx.getCurrentThreadCpuTime();
        long wall0 = System.nanoTime();
        List<String> jarList = jars == null ? List.of() : Files.readAllLines(Path.of(jars));
        if (jdk) {
            scanner.scanJdk();
        }
        scanner.scanJars(jarList);
        receipt.put("harvest_scan_thread_cpu_ns", mx.getCurrentThreadCpuTime() - cpu0);
        receipt.put("harvest_scan_wall_ns", System.nanoTime() - wall0);
        receipt.put("harvest_classes", state.totals.get(0).getOrDefault("all\tclasses", 0L)
                + state.totals.get(1).getOrDefault("all\tclasses", 0L));
        List<M3InventoryState.RegexKey> keys = new ArrayList<>(state.regex.values());
        long[][] costs = measure(keys, mx, receipt);
        String manifest = manifest(jdk, jarList);
        Files.writeString(out.resolve("INPUT_SET_MANIFEST.tsv"), manifest);
        byte[] image = M3ConstantRegexImageWriter.write(keys.stream()
                .map(k -> new M3ConstantRegexImageWriter.Item(k.pattern, k.flags, k.analysis)).toList(),
                M3ConstantRegexImageWriter.sha256Ints(manifest));
        Files.write(out.resolve("constant-regex.m3ctrx"), image);
        selfCheck(image, keys, receipt);
        receipt.put("image_bytes", (long) image.length);
        keyTable(out, keys, costs);
        long inputs = inputSample(out, state, sampleMod);
        receipt.put("input_sample_strings", inputs);
        receipt.put("input_sample_mod", (long) sampleMod);
        writeReceipt(out, receipt, keys, image, manifest);
        M3CompileTimeStringInventory.log("HARVEST DONE keys=" + keys.size() + " image=" + image.length);
    }

    /**
     * Per key: median wall ns (of {@value #ROUNDS} rounds) of stock compile and of compile+walk.
     * Thread CPU is read once around each whole pass, so the aggregate is exact up to the
     * platform CPU-clock granularity per pass.
     */
    private static long[][] measure(List<M3InventoryState.RegexKey> keys, ThreadMXBean mx,
            Map<String, Long> receipt) {
        long[][] compile = new long[keys.size()][ROUNDS];
        long[][] analyze = new long[keys.size()][ROUNDS];
        long cpuCompile = 0;
        long cpuAnalyze = 0;
        for (int r = 0; r < ROUNDS; r++) {
            long c0 = mx.getCurrentThreadCpuTime();
            for (int i = 0; i < keys.size(); i++) {
                long t0 = System.nanoTime();
                compileQuietly(keys.get(i).pattern, keys.get(i).flags);
                compile[i][r] = System.nanoTime() - t0;
            }
            long c1 = mx.getCurrentThreadCpuTime();
            for (int i = 0; i < keys.size(); i++) {
                long t0 = System.nanoTime();
                M3RegexRequirement.analyze(keys.get(i).pattern, keys.get(i).flags);
                analyze[i][r] = System.nanoTime() - t0;
            }
            cpuCompile += c1 - c0;
            cpuAnalyze += mx.getCurrentThreadCpuTime() - c1;
        }
        long[][] costs = new long[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            costs[i] = new long[] {median(compile[i]), median(analyze[i])};
        }
        long n = (long) keys.size() * ROUNDS;
        receipt.put("measure_rounds", (long) ROUNDS);
        receipt.put("measure_total_thread_cpu_ns_stock_compile", cpuCompile);
        receipt.put("measure_total_thread_cpu_ns_compile_plus_walk", cpuAnalyze);
        receipt.put("measure_mean_thread_cpu_ns_per_key_stock_compile", n == 0 ? 0 : cpuCompile / n);
        receipt.put("measure_mean_thread_cpu_ns_per_key_compile_plus_walk", n == 0 ? 0 : cpuAnalyze / n);
        return costs;
    }

    private static void compileQuietly(String pattern, int flags) {
        try {
            Pattern.compile(pattern, flags);
        } catch (IllegalArgumentException invalid) {
            // INVALID keys are measured for the cost of the stock failure itself.
        }
    }

    private static long median(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
    }

    private static String manifest(boolean jdk, List<String> jars) throws IOException {
        StringBuilder m = new StringBuilder("M3CTRX01_INPUT_SET\n");
        if (jdk) {
            m.append("jrt\t").append(System.getProperty("java.runtime.version")).append('\n');
        }
        List<String> sorted = jars.stream().map(String::trim).filter(s -> !s.isEmpty()).sorted().toList();
        for (String jar : sorted) {
            Path p = Path.of(jar);
            m.append("jar\t").append(sha256Hex(p)).append('\t').append(p.getFileName()).append('\n');
        }
        return m.toString();
    }

    static String sha256Hex(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
                sha.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    private static void selfCheck(byte[] image, List<M3InventoryState.RegexKey> keys, Map<String, Long> receipt)
            throws IOException {
        M3ConstantRegexImage read = M3ConstantRegexImage.verify(ByteBuffer.wrap(image));
        long ok = keys.stream().filter(k -> read.lookup(k.pattern, k.flags)
                .filter(e -> e.kind() == k.analysis.kind()
                        && e.atoms().length == k.analysis.requirement().atomCount())
                .isPresent()).count();
        if (ok != keys.size()) {
            throw new IllegalStateException("image self-check " + ok + "/" + keys.size());
        }
        receipt.put("image_self_check_keys", ok);
    }

    private static void keyTable(Path out, List<M3InventoryState.RegexKey> keys, long[][] costs) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("CONSTANT_REGEX_IMAGE_KEYS.tsv"))) {
            w.write("pattern\tflags\tkind\tmatcher_state_admitted\tatoms\tjdk21_sites\tvendor_sites"
                    + "\tstock_compile_median_ns\tcompile_plus_walk_median_ns\tapis\n");
            for (int i = 0; i < keys.size(); i++) {
                M3InventoryState.RegexKey k = keys.get(i);
                w.write(M3Tsv.escape(k.pattern) + '\t' + k.flags + '\t' + k.analysis.kind() + '\t'
                        + k.analysis.matcherStateAdmitted() + '\t' + k.analysis.requirement().atomCount()
                        + '\t' + k.sites[0] + '\t' + k.sites[1] + '\t' + costs[i][0] + '\t' + costs[i][1]
                        + '\t' + String.join(",", k.apis) + '\n');
            }
        }
    }

    private static long inputSample(Path out, M3InventoryState state, int mod) throws IOException {
        List<String> sample = state.ldc.keySet().stream()
                .filter(s -> s.length() <= MAX_INPUT_UNITS)
                .filter(s -> Math.floorMod(M3ContentKey.of(s, 0).h2(), mod) == 0)
                .sorted().toList();
        Files.write(out.resolve("INPUT_SAMPLE.tsv"), sample.stream().map(M3Tsv::escape).toList());
        return sample.size();
    }

    private static void writeReceipt(Path out, Map<String, Long> receipt, List<M3InventoryState.RegexKey> keys,
            byte[] image, String manifest) throws IOException {
        for (M3RegexRequirement.Kind kind : M3RegexRequirement.Kind.values()) {
            receipt.put("keys_" + kind, keys.stream().filter(k -> k.analysis.kind() == kind).count());
        }
        receipt.put("keys_total", (long) keys.size());
        receipt.put("keys_matcher_state_admitted", keys.stream().filter(k -> k.analysis.matcherStateAdmitted()).count());
        StringBuilder text = new StringBuilder("metric\tvalue\n");
        receipt.forEach((k, v) -> text.append(k).append('\t').append(v).append('\n'));
        text.append("image_sha256\t").append(HexFormat.of().formatHex(sha256(image))).append('\n');
        text.append("input_set_sha256\t").append(hexInts(M3ConstantRegexImageWriter.sha256Ints(manifest))).append('\n');
        text.append("producer_pattern_class_sha256\t").append(hexInts(M3ConstantRegexImage.patternClassSha256())).append('\n');
        text.append("producer_java_runtime_version\t").append(System.getProperty("java.runtime.version")).append('\n');
        Files.writeString(out.resolve("HARVEST_RECEIPT.tsv"), text);
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }

    static String hexInts(int[] ints) {
        StringBuilder s = new StringBuilder();
        Arrays.stream(ints).forEach(i -> s.append(String.format("%08x", i)));
        return s.toString();
    }
}
