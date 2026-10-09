/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class M3TranslationReceiverMapTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    public static void main(String[] args) throws Exception {
        String manifestRow = Files.readAllLines(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8)
                .stream()
                .filter(line -> line.startsWith("translate.rows\t"))
                .findFirst()
                .orElseThrow();
        String[] columns = manifestRow.split("\\t", -1);
        check(columns.length >= 9);
        check(columns[5].equals(
                "M3StringFacts + M3LexiconPrecompute.TranslationProjection"
                        + " + SharedLexiconPrecomputeCatalog.TranslationIdentity"
                        + " + M3LanguageGrammarSupport"));
        Set<String> fields = Arrays.stream(columns[8].split(","))
                .collect(Collectors.toSet());
        check(fields.equals(Set.of("source_id", "record_id", "source_language",
                "target_language", "lexicon_fingerprint", "source_fingerprint",
                "translated_token_ids", "mapped_token_count",
                "translation_grammar_supported", "source_revision", "source_blob_sha")));

        check(Class.forName("com.m3.text.M3LexiconPrecompute$TranslationProjection") != null);
        check(Class.forName(
                "com.m3.text.SharedLexiconPrecomputeCatalog$TranslationIdentity") != null);
        check(Class.forName("com.m3.text.M3LanguageGrammarSupport") != null);

        List<String> fieldMap = Files.readAllLines(
                Path.of("lexicon/synexia-precompute-field-map.tsv"),
                StandardCharsets.UTF_8);
        for (String field : List.of("sourceId", "recordId", "sourceLanguage",
                "targetLanguage", "lexiconFingerprint", "sourceFingerprint",
                "sourceRevision", "sourceBlobSha")) {
            check(fieldMap.stream().anyMatch(line -> line.startsWith(
                    "SharedLexiconPrecomputeCatalog.TranslationIdentity\t" + field + "\t")));
        }
        System.out.println("M3JDK_TRANSLATION_RECEIVER_MAP_PASS checks=" + checks);
    }
}
