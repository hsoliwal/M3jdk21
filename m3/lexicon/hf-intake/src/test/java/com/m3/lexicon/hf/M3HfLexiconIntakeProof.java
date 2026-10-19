// SPDX-License-Identifier: Apache-2.0
package com.m3.lexicon.hf;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/** Dependency-free differential/refusal proof; executable by javac and Maven. */
public final class M3HfLexiconIntakeProof {
    private static int checks;
    private static final String REVISION = "0123456789abcdef0123456789abcdef01234567";
    private M3HfLexiconIntakeProof() { }

    private static void require(boolean valid, String label) {
        checks++;
        if (!valid) throw new AssertionError(label);
    }

    private static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static M3HfLexiconIntakeRecipe.Approval approved(byte[] bytes,
            M3HfLexiconIntakeRecipe.Kind kind, long maxBytes, long maxRows) throws Exception {
        return new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset", REVISION,
                "language/rows.tsv", sha(bytes), "apache-2.0", "Operator-reviewed attribution",
                URI.create("https://example.org/rights/evidence"), "hi",
                kind, kind == M3HfLexiconIntakeRecipe.Kind.GRAMMAR_CONLLU ? 1 : 0,
                kind == M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV ? 1 : -1, false,
                maxBytes, maxRows, "APPROVED");
    }

    private static void fails(IoAction action, String reason) throws Exception {
        boolean refused = false;
        try { action.run(); }
        catch (IOException | IllegalArgumentException expected) { refused = true; }
        require(refused, reason);
    }

    @FunctionalInterface private interface IoAction { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path scratch = Files.createTempDirectory("m3-hf-intake-proof-");
        try {
            byte[] frequency = "अदरक\t34\nß\t7\nss\t5\nß\t8\n".getBytes(StandardCharsets.UTF_8);
            Path source = scratch.resolve("frequency.tsv");
            Path emitted = scratch.resolve("out.tsv");
            Path replay = scratch.resolve("replay.tsv");
            Files.write(source, frequency);
            var approval = approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 4);
            var receipt = M3HfLexiconIntakeRecipe.apply(approval, source, emitted);
            require(receipt.bytes() == frequency.length, "raw source bytes");
            require(receipt.sourceLines() == 4 && receipt.exportedRows() == 4, "per-row identities retain duplicates");
            List<String> rows = Files.readAllLines(emitted, StandardCharsets.UTF_8);
            require(rows.size() == 5 && rows.get(0).contains("precompute_payload"), "v2 header");
            require(rows.get(1).contains("अदरक") && rows.get(2).contains("ß"), "Unicode retained");
            require(rows.get(3).contains("\tss\t") && rows.get(4).contains("\tß\t"), "no false normalization or dedupe");
            require(rows.get(1).contains("\"frequency_count\":34"), "typed count precompute");
            require(rows.get(1).contains("\"source_revision\":\"" + REVISION + "\""), "pinned revision");
            require(rows.get(1).contains("\"license\":\"apache-2.0\""), "license provenance retained");
            M3HfLexiconIntakeRecipe.apply(approval, source, replay);
            require(Files.mismatch(emitted, replay) == -1, "recipe deterministic replay");
            byte[] preserved = Files.readAllBytes(emitted);
            fails(() -> M3HfLexiconIntakeRecipe.apply(approval, source, emitted), "no overwrite");
            require(java.util.Arrays.equals(preserved, Files.readAllBytes(emitted)), "output retained");
            var wrongHash = approved("something else".getBytes(StandardCharsets.UTF_8),
                    M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 4);
            Path mismatch = scratch.resolve("mismatch.tsv");
            fails(() -> M3HfLexiconIntakeRecipe.apply(wrongHash, source, mismatch), "sha mismatch refused");
            require(!Files.exists(mismatch), "no publication after hash failure");
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 3, 4),
                    source, scratch.resolve("too-large.tsv")), "byte budget");
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(frequency, M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 2048, 3),
                    source, scratch.resolve("too-many.tsv")), "row budget");
            Path invalid = scratch.resolve("malformed.txt");
            byte[] badUtf8 = {(byte) 0xc3, (byte) 0x28, (byte) '\n'};
            Files.write(invalid, badUtf8);
            fails(() -> M3HfLexiconIntakeRecipe.apply(
                    approved(badUtf8, M3HfLexiconIntakeRecipe.Kind.WORDS, 64, 3), invalid,
                    scratch.resolve("malformed.tsv")), "strict UTF8");
            byte[] grammar = ("# sent_id = 1\n1\tDogs\tdog\tNOUN\t_\tNumber=Plur\t0\troot\t_\t_\n"
                    + "2-3\tcan't\t_\t_\t_\t_\t_\t_\t_\t_\n"
                    + "2\tcan\tcan\tAUX\t_\t_\t1\taux\t_\t_\n").getBytes(StandardCharsets.UTF_8);
            Files.write(scratch.resolve("grammar.conllu"), grammar);
            M3HfLexiconIntakeRecipe.apply(approved(grammar,
                    M3HfLexiconIntakeRecipe.Kind.GRAMMAR_CONLLU, 8192, 4),
                    scratch.resolve("grammar.conllu"), scratch.resolve("grammar.tsv"));
            List<String> grammarOut = Files.readAllLines(scratch.resolve("grammar.tsv"));
            require(grammarOut.size() == 3, "CoNLL-U comments and ranges skipped");
            require(grammarOut.get(1).contains("\"lemma\":\"dog\"")
                    && grammarOut.get(1).contains("\"upos\":\"NOUN\""), "typed grammar facts");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "../unsafe",
                    sha(grammar), "apache-2.0", "attribution", URI.create("https://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "source path traversal rejected");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "safe.txt",
                    sha(grammar), "unknown", "attribution", URI.create("https://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "unknown license refused");
            fails(() -> new M3HfLexiconIntakeRecipe.Approval("owner/ok", REVISION, "safe.txt",
                    sha(grammar), "apache-2.0", "attribution", URI.create("http://example.org"),
                    "en", M3HfLexiconIntakeRecipe.Kind.WORDS, 0, -1, false, 8192, 4, "APPROVED"),
                    "non-HTTPS evidence refused");
            byte[] withHeader = "word\tfreq\nNamaste\t42\n".getBytes(StandardCharsets.UTF_8);
            Path headerSource = scratch.resolve("header.tsv");
            Files.write(headerSource, withHeader);
            var headerApproval = new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset",
                    REVISION, "language/rows.tsv", sha(withHeader), "apache-2.0",
                    "Operator-reviewed attribution", URI.create("https://example.org/rights/evidence"),
                    "hi", M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 0, 1, true,
                    1024, 2, "APPROVED");
            Path headerOutput = scratch.resolve("header-output.tsv");
            var headerReceipt = M3HfLexiconIntakeRecipe.apply(headerApproval,
                    headerSource, headerOutput);
            require(headerReceipt.exportedRows() == 1 && headerReceipt.sourceLines() == 2,
                    "source header counted but not projected");
            require(Files.readAllLines(headerOutput).get(1).contains("\"frequency_count\":42"),
                    "typed header source accepted");
            byte[] onlyHeader = "word\tfreq\n".getBytes(StandardCharsets.UTF_8);
            Path emptySource = scratch.resolve("only-header.tsv");
            Files.write(emptySource, onlyHeader);
            var emptyApproval = new M3HfLexiconIntakeRecipe.Approval("owner/reviewed-dataset",
                    REVISION, "language/rows.tsv", sha(onlyHeader), "apache-2.0",
                    "Operator-reviewed attribution", URI.create("https://example.org/rights/evidence"),
                    "hi", M3HfLexiconIntakeRecipe.Kind.FREQUENCY_TSV, 0, 1, true,
                    1024, 1, "APPROVED");
            fails(() -> M3HfLexiconIntakeRecipe.apply(emptyApproval, emptySource,
                    scratch.resolve("empty-out.tsv")), "no empty lexical image admitted");
            require(!Files.exists(scratch.resolve("empty-out.tsv")), "empty image not published");
            require(approval.pinnedUrl().toString().contains(REVISION), "immutable Hugging Face address");
            System.out.println("M3_HF_INTAKE_PROOF_PASS|checks=" + checks + "|types=3|jni_owner=reused");
        } finally {
            try (var files = Files.walk(scratch)) {
                for (var path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
