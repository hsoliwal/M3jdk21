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
        check(columns[2].equals("synexia-interned-language/src/main/java/com/synexia/language/TranslationTsvImporter.java"));
        check(columns[3].equals("translation_id"));
        check(columns[4].equals("source_language,target_language,source_token,target_token"));
        check(columns[5].equals(
                "M3StringFacts + M3LexiconPrecompute.TranslationProjection"
                        + " + SharedLexiconPrecomputeCatalog.TranslationIdentity"
                        + " + M3LanguageGrammarSupport"));
        Set<String> fields = Arrays.stream(columns[8].split(","))
                .collect(Collectors.toSet());
        check(fields.equals(Set.of("source_id", "record_id", "source_language",
                "target_language", "lexicon_fingerprint", "source_fingerprint",
                "translated_token_ids", "mapped_token_count",
                "translation_grammar_supported")));

        check(Class.forName("com.m3.text.M3LexiconPrecompute$TranslationProjection") != null);
        check(Class.forName(
                "com.m3.text.SharedLexiconPrecomputeCatalog$TranslationIdentity") != null);
        check(Class.forName("com.m3.text.M3LanguageGrammarSupport") != null);

        List<String> fieldMap = Files.readAllLines(
                Path.of("lexicon/synexia-precompute-field-map.tsv"),
                StandardCharsets.UTF_8);
        for (String field : List.of("sourceId", "recordId", "sourceLanguage",
                "targetLanguage", "lexiconFingerprint", "sourceFingerprint")) {
            check(fieldMap.stream().anyMatch(line -> line.startsWith(
                    "SharedLexiconPrecomputeCatalog.TranslationIdentity\t" + field + "\t")));
        }
        for (String row : List.of(
                "M3LexiconPrecompute.TranslationProjection\ttranslatedTokenIds\tint[]\ttranslated_token_ids\t",
                "M3LexiconPrecompute.TranslationProjection\tmappedTokenCount\tint\tmapped_token_count\t",
                "M3LexiconPrecompute.TranslationProjection\tsourceFingerprint\tString\tsource_fingerprint\t")) {
            check(fieldMap.stream().anyMatch(line -> line.startsWith(row)));
        }
        com.m3.text.SynexiaPrecomputePayload translationPayload =
                com.m3.text.SynexiaPrecomputePayload.parse(
                        "{\"lexicon_fingerprint\":\"fp-é\","
                                + "\"source_id\":\"translate.rows\","
                                + "\"source_revision\":\"rev-7\"}");
        check(translationPayload.requireString("lexicon_fingerprint").equals("fp-é"));
        check(translationPayload.requireString("source_id").equals("translate.rows"));
        check(translationPayload.canonicalJson().contains("\\u00e9"));
        check(com.m3.text.SynexiaPrecomputePayload.parse(translationPayload.canonicalJson())
                .requireString("lexicon_fingerprint").equals("fp-é"));
        check(com.m3.text.SynexiaPrecomputePayload.parse(
                "{\"lexicon_fingerprint\":7}").requireLong("lexicon_fingerprint") == 7L);

        System.out.println("M3JDK_TRANSLATION_RECEIVER_MAP_PASS checks=" + checks);
    }
}
