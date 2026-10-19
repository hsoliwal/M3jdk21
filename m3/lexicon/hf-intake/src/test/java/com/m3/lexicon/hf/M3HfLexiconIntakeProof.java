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

    private static List<String> resourceLines(String resource) throws IOException {
        try (var input = M3HfLexiconIntakeProof.class.getResourceAsStream(resource)) {
            require(input != null, "catalogue resource exists: " + resource);
            if (input == null) return List.of();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
        }
    }

    private static String[] splitTsv(String line) {
        return line.split(String.valueOf((char) 9), -1);
    }

    private static int verifyLanguageResourceDiscovery() throws IOException {
        List<String> lines = resourceLines(
                "/com/m3/lexicon/hf/language-resource-discovery.tsv");
        require(lines.size() == 64, "63 language-resource candidates plus header");
        List<String> expectedHeader = List.of("source_id", "resource", "category",
                "data_shape_and_identity", "rights_and_attribution", "m3_candidate_use",
                "admission_blockers", "state", "evidence");
        require(List.of(splitTsv(lines.getFirst())).equals(expectedHeader),
                "language-resource catalogue schema");
        java.util.Set<String> sourceIds = new java.util.HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = splitTsv(lines.get(i));
            require(fields.length == 9, "language-resource row has nine fields: " + i);
            if (fields.length != 9) continue;
            require(!fields[0].isBlank() && sourceIds.add(fields[0]),
                    "unique nonempty language-resource source ID: " + i);
            require(!fields[1].isBlank() && !fields[2].isBlank()
                    && !fields[3].isBlank() && !fields[4].isBlank()
                    && !fields[5].isBlank() && !fields[6].isBlank(),
                    "catalogue row has data shape, rights, fit and blockers: " + i);
            require("REFERENCE_ONLY".equals(fields[7]),
                    "discovery never implies data admission: " + fields[0]);
            require(java.util.Arrays.stream(fields[8].split(";", -1))
                    .allMatch(uri -> uri.startsWith("https://")),
                    "catalogue evidence links are HTTPS: " + fields[0]);
        }
        require(sourceIds.containsAll(List.of("wikidata-lexemes", "universal-dependencies",
                "glottolog-5-3", "dgt-tm", "geonames", "bipm-si-brochure-2026",
                "hf-langdex", "hf-langdex-dictionaries", "kaggle-langdex-notebook",
                "clarin-vlo-api-4-15", "olac-archive-network-2026",
                "elra-language-resource-catalogue", "concepticon-3-4", "norare-1-0",
                "asjp-database", "clics-4", "wold", "asl-lex-2", "mudidi-hf-2026",
                "openslr-beep", "dobes-archive", "elar-archive", "elarspeech-preprint-2026", "ai4bharat-indic-frequency-lists",
                "morphynet-multilingual-morphology",
                "cmudict-us-english-pronunciations", "hunspell-language-dictionaries",
                "global-wordnet-discovery-index", "babelnet-restricted-lexical-graph",
                "hf-paranames-1-0", "giellalt-language-repositories",
                "gf-resource-grammar-library", "sigmorphon-2022-segmentation")),
                "lexicon, grammar, translation, concept, norms, archive, sign and HF leads retained");
        String paranames = lines.stream().skip(1)
                .filter(line -> "hf-paranames-1-0".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(paranames.contains("MIT") && paranames.contains("CC BY 4.0")
                && paranames.contains("Conflicting license evidence"),
                "ParaNames license discrepancy stays visible and non-admitted");
        String giella = lines.stream().skip(1)
                .filter(line -> "giellalt-language-repositories".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(giella.contains("per repository") && giella.contains("GPL-3.0"),
                "GiellaLT language-package license scope remains explicit");
        String sigmorphon = lines.stream().skip(1)
                .filter(line -> "sigmorphon-2022-segmentation".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(sigmorphon.contains("CC BY-SA 3.0") && sigmorphon.contains("Wiktionary"),
                "SIGMORPHON task attribution and share-alike remain explicit");
        String panlex = lines.stream().skip(1).filter(line -> "panlex".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(panlex.contains("CC BY-NC-SA 4.0") && panlex.contains("CC0")
                && panlex.contains("clarification"),
                "conflicting PanLex license statements remain unresolved and conservative");
        String indicFrequency = lines.stream().skip(1)
                .filter(line -> "ai4bharat-indic-frequency-lists".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(indicFrequency.contains("CC BY-NC 4.0")
                && indicFrequency.contains("CC BY-NC-SA 4.0")
                && indicFrequency.contains("while"),
                "Indic frequency license mismatch remains visible and non-admitted");
        String morphynet = lines.stream().skip(1)
                .filter(line -> "morphynet-multilingual-morphology".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(morphynet.contains("CC BY-SA 3.0") && morphynet.contains("Wiktionary"),
                "MorphyNet attribution and share-alike terms remain attached");
        String hunspell = lines.stream().skip(1)
                .filter(line -> "hunspell-language-dictionaries".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(hunspell.contains("per-dictionary licenses"),
                "Hunspell engine rights are not mistaken for dictionary payload rights");
        String babelnet = lines.stream().skip(1)
                .filter(line -> "babelnet-restricted-lexical-graph".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(babelnet.contains("non-commercial") && babelnet.contains("research-institution"),
                "BabelNet restrictions remain explicit");
        String langdex = lines.stream().skip(1)
                .filter(line -> "hf-langdex".equals(splitTsv(line)[0]))
                .findFirst().orElse("");
        require(langdex.contains("preserve donor IDs and reject ambiguous rows")
                && langdex.contains("occurrence-coordinate"),
                "LangDex keeps native IDs and rejects ambiguous row identity");
        return lines.size() - 1;
    }

    private static int verifyLangDexTableAudit() throws IOException {
        List<String> lines = resourceLines(
                "/com/m3/lexicon/hf/langdex-table-admission.tsv");
        require(lines.size() == 12, "11 audited LangDex tables plus header");
        List<String> expectedHeader = List.of("dataset", "revision", "table",
                "reported_rows", "native_id_candidate", "identity_evidence",
                "null_duplicate_audit", "upstream_license", "combined_release_license",
                "admission_state", "blockers", "evidence");
        require(List.of(splitTsv(lines.getFirst())).equals(expectedHeader),
                "LangDex audit table schema");
        java.util.Set<String> tables = new java.util.HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] fields = splitTsv(lines.get(i));
            require(fields.length == 12, "LangDex audit row has twelve fields: " + i);
            if (fields.length != 12) continue;
            require("Asakiri/langdex".equals(fields[0])
                    && "e4e1d8aacd13eff1f8ba7f3df89bc872ba68f349".equals(fields[1]),
                    "LangDex source revision pinned: " + i);
            require(tables.add(fields[2]), "unique LangDex table name: " + fields[2]);
            require(Long.parseLong(fields[3]) > 0, "positive reported LangDex table count");
            require("NOT_AUDITED".equals(fields[6])
                    && "REFERENCE_ONLY".equals(fields[9]),
                    "unproven identity constraints cannot be admitted: " + fields[2]);
            require(fields[8].contains("CC-BY-SA-3.0")
                    && fields[11].startsWith("https://"),
                    "composite attribution and evidence retained: " + fields[2]);
            require(!fields[4].toLowerCase(java.util.Locale.ROOT).contains("occurrence"),
                    "do not substitute occurrence coordinates for donor IDs: " + fields[2]);
            if ("etymology".equals(fields[2])) {
                require(fields[10].contains("never synthesize occurrence coordinates"),
                        "ambiguous LangDex edges are rejected, not renumbered");
            }
            if ("cognate_sets".equals(fields[2])) {
                require(fields[5].contains("reversible")
                        && fields[10].contains("do not strip prefixes or invent row IDs"),
                        "native cognate ID must round-trip exactly");
            }
            if ("lexeme_concept".equals(fields[2])) {
                require(fields[10].contains("reject ambiguous duplicate pairs"),
                        "ambiguous duplicate donor mappings are rejected");
            }
        }
        return tables.size();
    }

    public static void main(String[] args) throws Exception {
        int discoveredSources = verifyLanguageResourceDiscovery();
        int auditedTables = verifyLangDexTableAudit();
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
            System.out.println("M3_HF_INTAKE_PROOF_PASS|checks=" + checks + "|types=3|language_sources="
                    + discoveredSources + "|langdex_tables=" + auditedTables + "|jni_owner=reused");
        } finally {
            try (var files = Files.walk(scratch)) {
                for (var path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }
}
