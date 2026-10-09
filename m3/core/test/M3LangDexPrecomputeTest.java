/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.m3.text.M3LangDexPrecompute;
import com.m3.text.SharedLangDexPrecomputeCatalog;

/** Contract proof for the Synexia LangDex receiver and source mapping. */
public final class M3LangDexPrecomputeTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static void expect(Class<? extends Throwable> type, Runnable body) {
        try {
            body.run();
            throw new AssertionError("missing " + type.getName());
        } catch (Throwable failure) {
            check(type.isInstance(failure));
        }
    }

    private static Map<String, String[]> parseManifest(String text) {
        String[] lines = text.split("\n", -1);
        check(lines.length > 1 && lines[lines.length - 1].isEmpty());
        Map<String, String[]> rows = new LinkedHashMap<>();
        for (int i = 1; i < lines.length - 1; i++) {
            String[] fields = lines[i].split("\t", -1);
            check(fields.length == 9);
            check(rows.putIfAbsent(fields[0], fields) == null);
        }
        return rows;
    }

    private static Map<String, Set<String>> parseFieldMap(String text) {
        String[] lines = text.split("\n", -1);
        Map<String, Set<String>> fields = new LinkedHashMap<>();
        for (int i = 1; i < lines.length - 1; i++) {
            String[] columns = lines[i].split("\t", -1);
            check(columns.length == 7);
            if (columns[0].startsWith("LangDex")) {
                check("MAPPED".equals(columns[5]));
                fields.computeIfAbsent(columns[0], ignored -> new java.util.LinkedHashSet<>())
                        .add(columns[1]);
                check("m3langdex-family-v1:synexia.langdex.tsv".equals(columns[4]));
            }
        }
        return fields;
    }

    public static void main(String[] args) throws Exception {
        String manifest = Files.readString(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8);
        Map<String, String[]> rows = parseManifest(manifest);
        String[] langdex = rows.get("unicodex.langdex.lexemes");
        check(langdex != null);
        check("M3LangDexPrecompute.Entry + M3LangDexPrecompute.WordProfile"
                .concat(" + M3LangDexPrecompute.LexicalProfile + M3LangDexPrecompute.Translation")
                .equals(langdex[5]));
        check(langdex[8].equals(
                "langdex_concept_id,langdex_confidence_permille,langdex_evidence_mask,"
                        + "langdex_feature_bits,langdex_flags,langdex_frequency,"
                        + "langdex_lexical_class_mask,langdex_semantic_class_mask,"
                        + "langdex_subject_id,langdex_target_lexeme_id"));
        String[] huggingFace = rows.get("dictlang.huggingface");
        check(huggingFace != null
                && "M3StringFacts + M3LangDexPrecompute.Identity".equals(huggingFace[5])
                && "-".equals(huggingFace[8]));

        Map<String, Set<String>> fieldMap = parseFieldMap(Files.readString(
                Path.of("lexicon/synexia-precompute-field-map.tsv"),
                StandardCharsets.UTF_8));
        check(fieldMap.get("LangDexProjection.Entry").equals(
                Set.of("conceptId", "frequency", "flags")));
        check(fieldMap.get("LangDexWordProfile").equals(Set.of(
                "lexicalClassMask", "semanticClassMask", "subjectId",
                "featureBits", "evidenceMask", "confidencePermille")));
        check(fieldMap.get("LangDexLexicalProfile").equals(Set.of(
                "lexicalClassMask", "featureBits", "evidenceMask", "confidencePermille")));
        check(fieldMap.get("LangDexTranslation").equals(Set.of("targetLexemeId")));

        M3LangDexPrecompute.Identity identity = new M3LangDexPrecompute.Identity(
                "unicodex.langdex.lexemes", "lexeme-7", "ENG", "Color", "langdex-r1");
        M3LangDexPrecompute.Identity huggingFaceIdentity =
                new M3LangDexPrecompute.Identity(
                        "dictlang.huggingface", "dataset-7", "ENG", "Color", "hf-r1");
        M3LangDexPrecompute.Entry entry = new M3LangDexPrecompute.Entry(
                "eng", "Color", 7L, 42L, 0x10, "colour");
        M3LangDexPrecompute.WordProfile profile =
                new M3LangDexPrecompute.WordProfile(3L, 5L, 2, 8, 16, 975);
        M3LangDexPrecompute.LexicalProfile lexical =
                new M3LangDexPrecompute.LexicalProfile(3L, 8, 16, 975);
        SharedLangDexPrecomputeCatalog catalog = SharedLangDexPrecomputeCatalog.builder()
                .entry(identity, entry)
                .wordProfile(identity, profile)
                .lexicalProfile(identity, lexical)
                .translation(
                    new M3LangDexPrecompute.TranslationIdentity(
                            "unicodex.langdex.lexemes", "translation-7", 7L,
                            "ENG", "Color", "FRA", "couleur", "langdex-r1"),
                    new M3LangDexPrecompute.Translation(
                            7L, "eng", "Color", "fra", "couleur", 19L))
                .build();
        check(catalog.entryCount() == 1
                && catalog.wordProfileCount() == 1
                && catalog.lexicalProfileCount() == 1
                && catalog.translationCount() == 1);
        check(catalog.entryAt(identity).orElseThrow().glottocode().equals("eng"));
        check(catalog.wordProfileAt(identity).orElseThrow().confidencePermille() == 975);
        check(catalog.lexicalProfileAt(identity).orElseThrow().featureBits() == 8);
        check(SharedLangDexPrecomputeCatalog.builder()
                .entry(huggingFaceIdentity, entry).build().entryCount() == 1);

        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 0L, 1L, 0, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 1L, -1L, 0, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 1L, 1L, 0x10000, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.WordProfile(
                0L, 0L, 0, 0, 0, 1001));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Identity(
                "unicodex.langdex.lexemes", "x", "en g", "x", "r"));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(identity, entry).entry(identity, entry));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(new M3LangDexPrecompute.Identity(
                        "other", "x", "eng", "Color", "r"), entry));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(new M3LangDexPrecompute.Identity(
                        "unicodex.langdex.lexemes", "x", "eng", "Other", "r"), entry));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.TranslationIdentity(
                "unicodex.langdex.lexemes", "translation-8", 0L,
                "eng", "Color", "fra", "couleur", "langdex-r1"));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .translation(new M3LangDexPrecompute.TranslationIdentity(
                        "unicodex.langdex.lexemes", "translation-8", 8L,
                        "eng", "Color", "fra", "couleur", "langdex-r1"),
                        new M3LangDexPrecompute.Translation(
                                7L, "eng", "Color", "fra", "couleur", 19L)));

        System.out.println("M3JDK_LANGDEX_CONTRACT_PASS checks=" + checks
                + " source=unicodex.langdex.lexemes");
    }
}
