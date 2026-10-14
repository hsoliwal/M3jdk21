// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import com.m3.precompute.M3Progress;
import com.m3.precompute.M3RegexOracleCorpus;
import com.m3.precompute.M3RegexOracleHarness;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

/**
 * Differential proof and measurement of {@link M3ConstantRegexGate} against stock
 * {@code java.util.regex}: (1) every harvested key x every sampled input for {@code find},
 * {@code matches}, {@code replaceAll} and {@code split}; (2) the frozen 10,000 x 64
 * {@link M3RegexOracleHarness} corpus through an image built from its expressions. Any
 * mismatch fails the run. Runtime cost is measured as whole-pass thread CPU, wall time and
 * thread-allocated bytes, stock versus gated, over the hit keys.
 *
 * <pre>
 * java -cp ... com.m3.compiletime.M3ConstantRegexDifferential --harvest DIR [--rounds 5]
 * </pre>
 */
public final class M3ConstantRegexDifferential {
    private static final String REPLACEMENT = "\u0001";
    private static final com.sun.management.ThreadMXBean MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    /** Repetitions of the whole key x input sweep inside one measured pass. */
    private static int reps = 8;

    private M3ConstantRegexDifferential() {}

    /** One measured pass. */
    record Pass(long cpuNs, long wallNs, long allocatedBytes, long calls, long trueResults) {}

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(M3CompileTimeStringInventory.arg(args, "--harvest", "target/constant-regex"));
        int rounds = Integer.parseInt(M3CompileTimeStringInventory.arg(args, "--rounds", "5"));
        reps = Integer.parseInt(M3CompileTimeStringInventory.arg(args, "--reps", "8"));
        Map<String, String> receipt = new TreeMap<>();
        M3ConstantRegexImage image = M3ConstantRegexImage.open(dir.resolve("constant-regex.m3ctrx"));
        List<String> inputs = Files.readAllLines(dir.resolve("INPUT_SAMPLE.tsv")).stream().map(M3Tsv::unescape).toList();
        List<M3ConstantRegexGate> gates = gates(dir.resolve("CONSTANT_REGEX_IMAGE_KEYS.tsv"), image);
        receipt.put("image_entries", Integer.toString(image.entryCount()));
        receipt.put("inputs", Integer.toString(inputs.size()));
        receipt.put("gates_valid_keys", Integer.toString(gates.size()));
        receipt.put("gates_hit", Long.toString(gates.stream().filter(M3ConstantRegexGate::hit).count()));
        long mismatches = differential(gates, inputs, receipt);
        List<M3ConstantRegexGate> hit = gates.stream().filter(M3ConstantRegexGate::hit).toList();
        bench("find_hit_keys", hit, inputs, rounds, receipt,
                (g, s) -> g.pattern().matcher(s).find(), M3ConstantRegexGate::find);
        bench("matches_hit_keys", hit, inputs, rounds, receipt,
                (g, s) -> g.pattern().matcher(s).matches(), M3ConstantRegexGate::matches);
        bench("replace_all_hit_keys", hit, inputs, rounds, receipt,
                (g, s) -> g.pattern().matcher(s).replaceAll(REPLACEMENT) != s,
                (g, s) -> g.replaceAll(s, REPLACEMENT) != s);
        bench("find_all_keys", gates, inputs, rounds, receipt,
                (g, s) -> g.pattern().matcher(s).find(), M3ConstantRegexGate::find);
        mismatches += oracle(receipt);
        receipt.put("total_mismatches", Long.toString(mismatches));
        StringBuilder text = new StringBuilder("metric\tvalue\n");
        receipt.forEach((k, v) -> text.append(k).append('\t').append(v).append('\n'));
        Files.writeString(dir.resolve("DIFFERENTIAL_RECEIPT.tsv"), text);
        M3CompileTimeStringInventory.log(text.toString());
        if (mismatches != 0) {
            throw new IllegalStateException("gate differs from stock java.util.regex: " + mismatches);
        }
    }

    private static List<M3ConstantRegexGate> gates(Path keys, M3ConstantRegexImage image) throws IOException {
        List<M3ConstantRegexGate> out = new ArrayList<>();
        for (String line : Files.readAllLines(keys).subList(1, Files.readAllLines(keys).size())) {
            String[] cells = line.split("\t", -1);
            if (cells[2].equals("INVALID")) {
                continue;
            }
            out.add(M3ConstantRegexGate.compile(M3Tsv.unescape(cells[0]), Integer.parseInt(cells[1]), image));
        }
        return out;
    }

    private static long differential(List<M3ConstantRegexGate> gates, List<String> inputs, Map<String, String> receipt) {
        long[] count = new long[7];
        for (M3ConstantRegexGate g : gates) {
            Pattern p = g.pattern();
            for (String s : inputs) {
                count[0]++;
                count[1] += p.matcher(s).find() != g.find(s) ? 1 : 0;
                count[2] += p.matcher(s).matches() != g.matches(s) ? 1 : 0;
                count[3] += !p.matcher(s).replaceAll(REPLACEMENT).equals(g.replaceAll(s, REPLACEMENT)) ? 1 : 0;
                count[4] += !Arrays.equals(p.split(s), g.split(s)) ? 1 : 0;
                if (g.hit() && !g.holds(s)) {
                    count[5]++;
                }
                if (g.hit()) {
                    count[6]++;
                }
            }
        }
        receipt.put("diff_pairs", Long.toString(count[0]));
        receipt.put("diff_mismatch_find", Long.toString(count[1]));
        receipt.put("diff_mismatch_matches", Long.toString(count[2]));
        receipt.put("diff_mismatch_replace_all", Long.toString(count[3]));
        receipt.put("diff_mismatch_split", Long.toString(count[4]));
        receipt.put("diff_hit_pairs", Long.toString(count[6]));
        receipt.put("diff_pruned_pairs", Long.toString(count[5]));
        return count[1] + count[2] + count[3] + count[4];
    }

    private static void bench(String name, List<M3ConstantRegexGate> gates, List<String> inputs, int rounds,
            Map<String, String> receipt, BiPredicate<M3ConstantRegexGate, String> stock,
            BiPredicate<M3ConstantRegexGate, String> gated) {
        for (int w = 0; w < 2; w++) {
            pass(gates, inputs, stock);
            pass(gates, inputs, gated);
        }
        Pass[] a = new Pass[rounds];
        Pass[] b = new Pass[rounds];
        for (int r = 0; r < rounds; r++) {
            a[r] = pass(gates, inputs, stock);
            b[r] = pass(gates, inputs, gated);
        }
        if (a[0].trueResults() != b[0].trueResults()) {
            throw new IllegalStateException(name + " result count differs");
        }
        put(receipt, name, "stock", a);
        put(receipt, name, "gated", b);
        receipt.put(name + "_calls_per_pass", Long.toString(a[0].calls()));
        receipt.put(name + "_reps_per_pass", Integer.toString(reps));
    }

    private static void put(Map<String, String> receipt, String name, String side, Pass[] passes) {
        receipt.put(name + "_" + side + "_cpu_ns_median", Long.toString(median(passes, Pass::cpuNs)));
        receipt.put(name + "_" + side + "_wall_ns_median", Long.toString(median(passes, Pass::wallNs)));
        receipt.put(name + "_" + side + "_alloc_bytes_median", Long.toString(median(passes, Pass::allocatedBytes)));
        receipt.put(name + "_" + side + "_true_results", Long.toString(passes[0].trueResults()));
    }

    private static long median(Pass[] passes, java.util.function.ToLongFunction<Pass> f) {
        long[] v = Arrays.stream(passes).mapToLong(f).sorted().toArray();
        return v[v.length / 2];
    }

    private static Pass pass(List<M3ConstantRegexGate> gates, List<String> inputs,
            BiPredicate<M3ConstantRegexGate, String> op) {
        long calls = 0;
        long hits = 0;
        long alloc0 = MX.getCurrentThreadAllocatedBytes();
        long cpu0 = MX.getCurrentThreadCpuTime();
        long wall0 = System.nanoTime();
        for (int rep = 0; rep < reps; rep++) {
            for (M3ConstantRegexGate g : gates) {
                for (String s : inputs) {
                    calls++;
                    hits += op.test(g, s) ? 1 : 0;
                }
            }
        }
        long wall = System.nanoTime() - wall0;
        long cpu = MX.getCurrentThreadCpuTime() - cpu0;
        return new Pass(cpu, wall, MX.getCurrentThreadAllocatedBytes() - alloc0, calls, hits);
    }

    /** Oracle corpus: image built from the frozen expressions, checked pair by pair. */
    private static long oracle(Map<String, String> receipt) throws IOException {
        M3RegexOracleHarness harness = M3RegexOracleHarness.compile(M3Progress.none());
        List<String> expressions = M3RegexOracleCorpus.expressions();
        List<M3ConstantRegexImageWriter.Item> items = new ArrayList<>();
        Map<String, Boolean> seen = new HashMap<>();
        for (int r = 0; r < expressions.size(); r++) {
            if (harness.regexValid(r) && seen.put(expressions.get(r), Boolean.TRUE) == null) {
                items.add(new M3ConstantRegexImageWriter.Item(expressions.get(r), 0,
                        M3RegexRequirement.analyze(expressions.get(r), 0)));
            }
        }
        M3ConstantRegexImage image = M3ConstantRegexImage.verify(ByteBuffer.wrap(
                M3ConstantRegexImageWriter.write(items, M3ConstantRegexImageWriter.sha256Ints(harness.oracleRoot()))));
        Map<String, M3ConstantRegexGate> gates = new HashMap<>();
        items.forEach(i -> gates.put(i.pattern(), M3ConstantRegexGate.compile(i.pattern(), 0, image)));
        M3RegexOracleHarness.Report report = harness.check("m3-constant-regex-image",
                (regex, input) -> {
                    M3ConstantRegexGate g = gates.get(regex);
                    return g == null || !g.hit() || g.holds(input)
                            ? M3RegexOracleHarness.Verdict.MAY_MATCH : M3RegexOracleHarness.Verdict.CANNOT_MATCH;
                }, 16);
        long mismatches = 0;
        for (int r = 0; r < expressions.size(); r++) {
            M3ConstantRegexGate g = gates.get(expressions.get(r));
            if (g == null || !harness.regexValid(r)) {
                continue;
            }
            for (int s = 0; s < harness.stringCount(); s++) {
                mismatches += g.find(harness.input(s)) != harness.find(r, s) ? 1 : 0;
                mismatches += g.matches(harness.input(s)) != harness.matches(r, s) ? 1 : 0;
            }
        }
        receipt.put("oracle_root", harness.oracleRoot());
        receipt.put("oracle_pairs", Long.toString(report.pairs()));
        receipt.put("oracle_find_true", Long.toString(report.findTrue()));
        receipt.put("oracle_pruned", Long.toString(report.pruned()));
        receipt.put("oracle_unsound", Long.toString(report.unsound()));
        receipt.put("oracle_report_root", report.root());
        receipt.put("oracle_distinct_valid_keys", Integer.toString(items.size()));
        receipt.put("oracle_keys_hit", Long.toString(gates.values().stream().filter(M3ConstantRegexGate::hit).count()));
        receipt.put("oracle_find_matches_mismatches", Long.toString(mismatches));
        return mismatches + report.unsound();
    }
}
