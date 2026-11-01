/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK 21 from cpullm-java/facet-release/tests/FacetChecks.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.facets;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/** Independent set/byte/property oracles. Synthetic fixtures are never labelled Chrome/model evidence. */
public final class FacetChecks {
    private static int checks;
    private FacetChecks() { }
    private static void yes(boolean result, String why) { checks++; if (!result) throw new AssertionError(why); }
    @FunctionalInterface private interface Attempt { void run() throws Exception; }
    private static void no(Attempt run, String why) throws Exception {
        boolean failed = false;
        try { run.run(); } catch (IllegalArgumentException | IllegalStateException | java.io.IOException | IndexOutOfBoundsException e) { failed = true; }
        yes(failed, why);
    }
    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
    private static FacetCorpus corpus() {
        String[] text = {"The runtime reports success.", "The independent check reports failure.",
            "Keep this release obligation open.", "Chrome weights layout remains UNKNOWN.", "नमस्ते CPU 🧩."};
        var sources = new ArrayList<FacetCorpus.Source>(); var slices = new ArrayList<FacetCorpus.Slice>();
        for (int i = 0; i < text.length; i++) {
            byte[] data = bytes(text[i]); sources.add(new FacetCorpus.Source("s" + i, data));
            slices.add(new FacetCorpus.Slice("s" + i, 0, data.length, 1L << i, 100 - i,
                    i == 2, i < 2 ? "declared-conflict" : ""));
        }
        return new FacetCorpus(sources, List.of("pass", "fail", "obligation", "model", "unicode"), slices, 4096);
    }
    private static int[] oracleUnion(List<FacetNode> nodes) {
        var values = new TreeSet<Integer>(); for (FacetNode node : nodes) for (int row : node.evidenceRows()) values.add(row);
        return values.stream().mapToInt(Integer::intValue).toArray();
    }
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]); Files.createDirectory(out);
        FacetKernels java = FacetKernels.javaOnly();
        FacetKernels kernel = args.length == 3 ? FacetKernels.pinnedJni(Path.of(args[1]), args[2]) : java;
        FacetCorpus corpus = corpus(); FacetNode all = FacetNode.all(corpus);
        yes(corpus.size() == 5 && corpus.root().length() == 64, "source admission");
        var sources = new ArrayList<>(corpus.sources()); var slices = new ArrayList<>(corpus.slices());
        Collections.reverse(sources); Collections.reverse(slices);
        yes(corpus.root().equals(new FacetCorpus(sources, corpus.vocabulary(), slices, 4096).root()), "canonical input ordering");
        byte[] returned = corpus.expandSource("s0"); returned[0] ^= 1;
        yes(corpus.expand(0).startsWith("The"), "immutable source output");
        int[] posting = corpus.rowsForFacet("model"); posting[0] = 0;
        yes(corpus.rowsForFacet("model")[0] == 3, "immutable posting output");
        yes(corpus.mask("pass", "model") == 9, "facet dictionary lookup");
        no(() -> corpus.mask("absent"), "unknown named facet");
        no(() -> new FacetCorpus.Source("x", new byte[]{(byte) 0xc0, (byte) 0x80}), "invalid UTF8 source");
        no(() -> new FacetCorpus.Source("x\n", new byte[0]), "identifier framing");
        no(() -> new FacetCorpus.Source("\ud800", new byte[0]), "unpaired UTF16 name");
        no(() -> new FacetCorpus(List.of(new FacetCorpus.Source("x", bytes("é"))), List.of("a"),
                List.of(new FacetCorpus.Slice("x", 0, 1, 1, 0, false, "")), 4096), "multibyte range split");
        no(() -> new FacetCorpus(corpus.sources(), corpus.vocabulary(), corpus.slices(), 0), "budget checked");
        no(() -> new FacetCorpus(corpus.sources(), List.of("a", "a"), List.of(), 4096), "duplicate facet vocabulary");
        no(() -> FacetNode.leaf(corpus, 0, 0), "duplicate leaf IDs");
        no(() -> FacetNode.leaf(corpus, 5), "coordinate bounds");
        no(() -> FacetNode.combine(List.of()), "empty fan-in");
        var alternate = new FacetCorpus(corpus.sources(), corpus.vocabulary(), corpus.slices(), 8192);
        no(() -> FacetNode.combine(List.of(all, FacetNode.all(alternate))), "foreign policy/source corpus rejected");
        FacetNode first = FacetNode.leaf(corpus, 0, 1), second = FacetNode.leaf(corpus, 2, 3, 4);
        FacetNode grouped = FacetNode.combine(List.of(first, second, first));
        yes(all.semanticRoot().equals(grouped.semanticRoot()), "same evidence despite regrouping/shared child");
        yes(!all.lineageRoot().equals(grouped.lineageRoot()), "lineage distinct from semantic scope");
        yes(grouped.lineageRoot().equals(FacetNode.combine(List.of(second, first)).lineageRoot()), "permutation/dedup canonical lineage");
        var request = new FacetProjection.Request(31, 4096, 5);
        var result = FacetProjection.select(grouped, request, kernel);
        FacetProjection.verify(all, result);
        yes(result.status() == FacetProjection.Status.PASS && result.selectedAtoms() == 5, "root includes all facets");
        yes(result.utf8Bytes() == bytes(result.text()).length, "exact UTF8 budget not chars");
        yes(result.text().contains("नमस्ते") && result.text().contains("UNKNOWN"), "anchors/Unicode retained exactly");
        yes(result.equals(FacetProjection.select(all, request, java)), "binding independent result");
        var exact = FacetProjection.select(all, new FacetProjection.Request(31, result.utf8Bytes(), 5), kernel);
        yes(exact.status() == FacetProjection.Status.PASS && exact.utf8Bytes() == result.utf8Bytes(), "exact fit");
        var shortfall = FacetProjection.select(all, new FacetProjection.Request(31, result.utf8Bytes() - 1, 5), kernel);
        yes(shortfall.status() == FacetProjection.Status.GREEDY_COVERAGE_SHORTFALL, "one byte short explicit");
        yes(FacetProjection.select(all, new FacetProjection.Request(0, 0, 0), kernel).status()
                == FacetProjection.Status.MANDATORY_OVER_BUDGET, "never truncate mandatory evidence");
        yes(FacetProjection.select(FacetNode.leaf(corpus, 0, 2), request, kernel).status()
                == FacetProjection.Status.INCOMPLETE_CONFLICT_SCOPE, "do not claim one-sided conflict scope");
        yes(FacetProjection.select(first, new FacetProjection.Request(16, 4096, 5), kernel).status()
                == FacetProjection.Status.MISSING_FACETS, "missing facet explicit");
        no(() -> FacetProjection.select(all, new FacetProjection.Request(32, 4096, 5), kernel), "unknown mask bits");
        var fabricated = new FacetProjection.Result(result.status(), result.corpusRoot(), result.scopeRoot(), result.request(),
                result.evidenceIds(), "FABRICATED", result.coveredFacets(), result.eligibleAtoms(), result.selectedAtoms(), 10, result.receipt());
        no(() -> FacetProjection.verify(all, fabricated), "receipt alone is not verification");
        var limited = FacetProjection.select(all, new FacetProjection.Request(1, 4096, 3), kernel);
        yes(limited.status() == FacetProjection.Status.PASS && limited.selectedAtoms() == 3, "conflict pair plus obligation atomic");
        yes(limited.evidenceIds().contains(corpus.evidenceId(0)) && limited.evidenceIds().contains(corpus.evidenceId(1)), "both conflict members preserved");
        yes(corpus.expand(3).contains("UNKNOWN"), "unselected evidence remains expandable");
        Path snapshot = out.resolve("corpus.sff"); FacetSnapshot.write(snapshot, corpus, 4096);
        FacetCorpus loaded = FacetSnapshot.read(snapshot);
        yes(loaded.root().equals(corpus.root()), "source-complete snapshot round trip");
        yes(FacetProjection.select(FacetNode.all(loaded), request, kernel).equals(result), "snapshot replay");
        no(() -> FacetSnapshot.write(snapshot, corpus, 4096), "snapshot overwrite rejected");
        no(() -> FacetSnapshot.write(out.resolve("wrong.sff"), corpus, 5000), "wrong policy rejected");
        byte[] original = Files.readAllBytes(snapshot); byte[] changed = original.clone(); changed[20] ^= 1;
        Path damaged = out.resolve("tampered.sff"); Files.write(damaged, changed);
        no(() -> FacetSnapshot.read(damaged), "snapshot tamper rejected");
        Files.write(damaged, Arrays.copyOf(original, original.length - 1)); no(() -> FacetSnapshot.read(damaged), "truncation rejected");
        Files.write(damaged, Arrays.copyOf(original, original.length + 1)); no(() -> FacetSnapshot.read(damaged), "trailing byte rejected");
        var empty = new FacetCorpus(List.of(), List.of(), List.of(), 0);
        yes(FacetProjection.select(FacetNode.all(empty), new FacetProjection.Request(0, 0, 0), kernel).status()
                == FacetProjection.Status.PASS, "empty zero-budget corpus");
        Random random = new Random(973240L);
        for (int iteration = 0; iteration < 200; iteration++) {
            List<FacetNode> children = new ArrayList<>();
            for (int child = 0; child < 8; child++) {
                var rows = new TreeSet<Integer>(); for (int i = 0; i < 5; i++) if (random.nextBoolean()) rows.add(i);
                children.add(FacetNode.leaf(corpus, rows.stream().mapToInt(Integer::intValue).toArray()));
            }
            int[] reference = oracleUnion(children);
            yes(Arrays.equals(reference, FacetNode.combine(children).evidenceRows()), "independent set union oracle");
            Collections.shuffle(children, random);
            yes(Arrays.equals(reference, FacetNode.combine(children).evidenceRows()), "union permutation oracle");
            int[] batchSizes = {0, 1, 255, 256, 257, 1025};
            int n = batchSizes[iteration % batchSizes.length]; long[] masks = new long[n];
            for (int i = 0; i < n; i++) masks[i] = random.nextLong(); long wanted = random.nextLong();
            long[] prior = masks.clone(); int[] gains = kernel.gains(masks, wanted);
            yes(gains.length == n, "gain output length");
            for (int i = 0; i < n; i++) {
                int count = 0; for (int bit = 0; bit < 64; bit++) if (((masks[i] & wanted) & (1L << bit)) != 0) count++;
                yes(gains[i] == count, "independent 64-bit oracle");
            }
            yes(Arrays.equals(prior, masks), "native input preserved");
        }
        long[] full = new long[65_536]; Arrays.fill(full, -1L);
        yes(Arrays.stream(kernel.gains(full, -1)).allMatch(x -> x == 64), "maximum batch/all bits");
        no(() -> kernel.gains(new long[65_537], 1), "batch limit enforced");
        var names = new ArrayList<String>(); for (int i = 0; i < 64; i++) names.add("f" + i);
        var sign = new FacetCorpus(List.of(new FacetCorpus.Source("x", bytes("x"))), names,
                List.of(new FacetCorpus.Slice("x", 0, 1, Long.MIN_VALUE, 0, false, "")), 1024);
        yes(FacetProjection.select(FacetNode.all(sign), new FacetProjection.Request(Long.MIN_VALUE, 1024, 1), kernel).status()
                == FacetProjection.Status.PASS, "facet bit 63 works across JNI");
        legacy();
        Thread.currentThread().interrupt(); boolean interrupted = false;
        try { FacetProjection.select(all, request, kernel); } catch (java.util.concurrent.CancellationException e) { interrupted = true; }
        finally { Thread.interrupted(); }
        yes(interrupted, "cooperative cancellation");
        if (args.length == 3) no(() -> FacetKernels.pinnedJni(Path.of(args[1]), "0".repeat(64)), "native wrong hash rejected");
        Files.writeString(out.resolve("result.txt"), result.text());
        Files.writeString(out.resolve("receipt.txt"), result.receipt() + "\n");
        System.out.println("FACET_PASS checks=" + checks + " kernel=" + kernel.implementation() + " calls=" + kernel.calls()
                + " source_root=" + corpus.root() + " receipt=" + result.receipt());
    }
    private static void legacy() throws Exception {
        // Contract-shaped existing Summary output; not a claim that the whole legacy app was executed here.
        var records = List.<Map<String, Object>>of(Map.of("record_id", "r1", "title", "", "body", "First. Second. Third."));
        var sentences = List.of(Map.of("record_id", "r1", "evidence_id", "e1", "text", "First."),
                Map.of("record_id", "r1", "evidence_id", "e2", "text", "Second."), Map.of("record_id", "r1", "evidence_id", "e3", "text", "Third."));
        var legacy = Map.<String, Object>of("sentences", sentences, "summary_text", "First.", "evidence_ids", List.of("e1"));
        var corpus = LegacySummaryFacets.prepare(records, legacy, List.of("late"),
                Map.of("e3", new LegacySummaryFacets.Tags(1, 0, true, "")), 4096);
        yes(corpus.size() == 3, "import full evidence table, not selected summary");
        var result = FacetProjection.select(FacetNode.all(corpus), new FacetProjection.Request(1, 100, 1), FacetKernels.javaOnly());
        yes(result.status() == FacetProjection.Status.PASS && result.text().startsWith("Third."), "later evidence survives historical contraction");
        no(() -> LegacySummaryFacets.prepare(records, legacy, List.of(), Map.of("missing", new LegacySummaryFacets.Tags(0, 0, false, "")), 4096), "orphan annotation rejected");
        var bad = Map.<String, Object>of("sentences", List.of(Map.of("record_id", "r1", "evidence_id", "e1", "text", "Invented.")));
        no(() -> LegacySummaryFacets.prepare(records, bad, List.of(), Map.of(), 4096), "fabricated legacy sentence rejected");
        var duplicateScore = Map.<String, Object>of("level", 0, "method_scores",
                Map.of("ranking", List.of(Map.of("evidence_id", "e1"), Map.of("evidence_id", "e1"))));
        var duplicate = Map.<String, Object>of("sentences", sentences, "nodes", List.of(duplicateScore));
        no(() -> LegacySummaryFacets.prepare(records, duplicate, List.of(), Map.of(), 4096), "duplicate per-method scores rejected");
        var orphanScore = Map.<String, Object>of("level", 0, "method_scores", Map.of("ranking", List.of(Map.of("evidence_id", "absent"))));
        no(() -> LegacySummaryFacets.prepare(records, Map.of("sentences", sentences, "nodes", List.of(orphanScore)),
                List.of(), Map.of(), 4096), "orphan ranking evidence rejected");
        no(() -> LegacySummaryFacets.prepare(records, legacy, List.of(), Map.of(), -1), "legacy negative budget rejected");
        no(() -> LegacySummaryFacets.prepare(records, legacy, List.of(), Map.of(), 1), "legacy source budget before concatenation");
    }
}
