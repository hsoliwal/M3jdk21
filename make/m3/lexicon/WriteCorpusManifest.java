/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.m3.text.M3CorpusSourceReceipt;
import jdk.m3.text.M3LanguageCorpusManifest;
import jdk.m3.text.M3LanguageCorpusManifest.Artifact;
import jdk.m3.text.M3LanguageCorpusManifest.Plane;

/**
 * Writes the M3LanguageCorpusManifest of the registry lexicons: per lexicon version one
 * DICTIONARY artifact (the catalog export, pinned by registry export_sha256) and one WORD_FACTS
 * artifact (the WORD TAB COUNT input, pinned by counts_sha256), both bound to the export's
 * SHA-256, the first 64 bits of it as coordinate fingerprint, and generation = lexicon_num.
 * Source receipts come from lexicon-receipts.tsv. Reads the file back and requires both planes
 * for every lexicon.
 *
 * <p>usage: java --add-exports java.base/jdk.m3.text=ALL-UNNAMED WriteCorpusManifest.java
 * REGISTRY INPUTS RECEIPTS OUTPUT_DIR</p>
 */
public final class WriteCorpusManifest {
    private static final String NORMALIZATION = "none";

    private WriteCorpusManifest() { }

    public static void main(String[] args) throws Exception {
        List<String[]> registry = rows(Path.of(args[0]));
        Map<String, String[]> inputs = byKey(rows(Path.of(args[1])));
        Map<String, String[]> receipts = byKey(rows(Path.of(args[2])));
        List<Artifact> artifacts = new ArrayList<>();
        for (String[] row : registry) {
            String[] input = inputs.get(row[0]);
            if (row[7].equals("-") || input == null || input[4].equals("-"))
                throw new IllegalStateException("unpinned lexicon " + row[0]);
            artifacts.add(artifact(Plane.DICTIONARY, row, row[7], receipts.get(input[7])));
            artifacts.add(artifact(Plane.WORD_FACTS, row, input[4], receipts.get(input[8])));
        }
        Path written = M3LanguageCorpusManifest.of(artifacts).writeNew(Path.of(args[3]));
        M3LanguageCorpusManifest back = M3LanguageCorpusManifest.read(written);
        for (String[] row : registry) {
            List<Artifact> bound = back.requireBoundArtifactsForPlanes(row[4],
                    Long.parseLong(row[0]), row[7], fingerprint(row[7]), tokenizer(row),
                    NORMALIZATION, EnumSet.of(Plane.DICTIONARY, Plane.WORD_FACTS));
            if (bound.size() != 2) throw new IllegalStateException("planes of " + row[1]);
        }
        System.out.println("CORPUS_MANIFEST_PASS artifacts=" + back.artifacts().size()
                + " lexicons=" + registry.size() + " root=" + back.root() + " file="
                + written.getFileName() + " bytes=" + Files.size(written));
    }

    private static Artifact artifact(Plane plane, String[] row, String output, String[] source) {
        if (source == null) throw new IllegalStateException("missing receipt for " + row[1]);
        M3CorpusSourceReceipt receipt = new M3CorpusSourceReceipt(source[1], URI.create(source[2]),
                source[3], source[4], source[5]);
        return new Artifact(plane, row[4], row[1], tokenKind(row[2]), tokenizer(row),
                NORMALIZATION, Long.parseLong(row[0]), fingerprint(row[7]), row[7], output,
                receipt, source[6], URI.create(source[7]), 0, null);
    }

    private static String tokenKind(String sourceId) {
        if (sourceId.startsWith("grammar.")) return "token";
        if (sourceId.startsWith("hf.tok.")) return "subword";
        if (sourceId.startsWith("cldr.exemplar.")) return "exemplar";
        if (sourceId.startsWith("cldr.names.")) return "name";
        return "word";
    }

    /** The measurement's word split for the Wikipedia counts; the other lists are entries. */
    private static String tokenizer(String[] row) {
        return row[2].startsWith("hf.wiki.") ? "unicode-letter-runs+xlm-roberta-cjk@e73636d4"
                : "whole-entry";
    }

    private static long fingerprint(String sha256) {
        return Long.parseUnsignedLong(sha256.substring(0, 16), 16);
    }

    private static List<String[]> rows(Path path) throws Exception {
        List<String[]> rows = new ArrayList<>();
        String[] lines = Files.readString(path, StandardCharsets.UTF_8).split("\n");
        for (int at = 1; at < lines.length; at++) rows.add(lines[at].split("\t", -1));
        return rows;
    }

    private static Map<String, String[]> byKey(List<String[]> rows) {
        Map<String, String[]> result = new HashMap<>();
        for (String[] row : rows) result.put(row[0], row);
        return result;
    }
}
