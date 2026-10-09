/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.m3.text.InternDomainV1;
import com.m3.text.M3LangDexPrecompute;
import com.m3.text.SharedLangDexPrecomputeCatalog;

/** Contract proof for the Synexia LangDex receiver and source mapping. */
public final class M3LangDexPrecomputeTest {
    private static int checks;

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("check " + checks);
    }

    private static String digest(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private static M3LangDexPrecompute.Identity identity(
            String sourceId, String recordId, String glottocode,
            String surface, String sourceRevision) {
        byte[] canonical = (sourceId + "\u0000" + recordId + "\u0000"
                + glottocode + "\u0000" + surface).getBytes(StandardCharsets.UTF_8);
        return new M3LangDexPrecompute.Identity(
                sourceId, recordId, glottocode, surface, sourceRevision,
                InternDomainV1.CONCEPT, "synexia-canonical-v2",
                canonical, digest(canonical));
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
            if (columns[0].startsWith("M3LangDexPrecompute.")) {
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
                .concat(" + M3LangDexPrecompute.LexicalProfile + M3LangDexPrecompute.TranslationIdentity"
                        + " + M3LangDexPrecompute.Translation")
                .equals(langdex[5]));
        check(langdex[4].equals(
                "glottocode,lemma,source,concept_id,canonical_schema,canonical_bytes,"
                        + "canonical_digest,domain"));
        check(langdex[8].equals(
                "langdex_canonical_bytes,langdex_canonical_digest,langdex_canonical_schema,"
                        + "langdex_concept_id,langdex_confidence_permille,langdex_domain,"
                        + "langdex_evidence_mask,langdex_feature_bits,langdex_flags,"
                        + "langdex_frequency,langdex_glottocode,langdex_lexical_class_mask,"
                        + "langdex_record_id,langdex_semantic_class_mask,"
                        + "langdex_source_glottocode,langdex_source_id,"
                        + "langdex_source_revision,langdex_source_surface,"
                        + "langdex_subject_id,langdex_surface,langdex_target_glottocode,"
                        + "langdex_target_lexeme_id,langdex_target_surface,lemma"));
        String[] huggingFace = rows.get("dictlang.huggingface");
        check(huggingFace != null
                && "M3StringFacts + M3LangDexPrecompute.Identity".equals(huggingFace[5])
                && ("language_tag,lexeme,source_revision,canonical_schema,canonical_bytes,"
                        + "canonical_digest,domain").equals(huggingFace[4])
                && "-".equals(huggingFace[8]));

        Map<String, Set<String>> fieldMap = parseFieldMap(Files.readString(
                Path.of("lexicon/synexia-precompute-field-map.tsv"),
                StandardCharsets.UTF_8));
        Map<String, Set<String>> identityFieldMap = parseFieldMap(Files.readString(
                Path.of("lexicon/synexia-langdex-identity-field-map.tsv"),
                StandardCharsets.UTF_8));
        check(fieldMap.get("M3LangDexPrecompute.Identity").equals(Set.of(
                "sourceId", "recordId", "glottocode", "surface", "sourceRevision",
                "domain", "canonicalSchema", "canonicalBytes", "canonicalDigest")));
        check(identityFieldMap.get("M3LangDexPrecompute.Identity").equals(Set.of(
                "sourceId", "recordId", "glottocode", "surface", "sourceRevision",
                "domain", "canonicalSchema", "canonicalBytes", "canonicalDigest")));
        check(identityFieldMap.get("M3LangDexPrecompute.TranslationIdentity").equals(Set.of(
                "sourceId", "recordId", "conceptId", "sourceGlottocode", "sourceSurface",
                "targetGlottocode", "targetSurface", "sourceRevision")));
        check(fieldMap.get("M3LangDexPrecompute.Entry").equals(
                Set.of("glottocode", "surface", "conceptId", "frequency", "flags", "lemma")));
        check(fieldMap.get("M3LangDexPrecompute.WordProfile").equals(Set.of(
                "lexicalClassMask", "semanticClassMask", "subjectId",
                "featureBits", "evidenceMask", "confidencePermille")));
        check(fieldMap.get("M3LangDexPrecompute.LexicalProfile").equals(Set.of(
                "lexicalClassMask", "featureBits", "evidenceMask", "confidencePermille")));
        check(fieldMap.get("M3LangDexPrecompute.Translation").equals(Set.of(
                "conceptId", "sourceGlottocode", "sourceSurface",
                "targetGlottocode", "targetSurface", "targetLexemeId")));

        M3LangDexPrecompute.Identity identity = identity("unicodex.langdex.lexemes", "lexeme-7", "ENG", "Color", "langdex-r1");
        M3LangDexPrecompute.Identity huggingFaceIdentity =
                identity("dictlang.huggingface", "dataset-7", "ENG", "Color", "hf-r1");
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
        check(identity.domain() == InternDomainV1.CONCEPT);
        check(identity.canonicalSchema().equals("synexia-canonical-v2"));
        check(identity.canonicalDigest().equals(digest(identity.canonicalBytes())));
        byte[] copied = identity.canonicalBytes();
        copied[0] ^= 1;
        check(identity.canonicalBytes()[0] != copied[0]);
        check(catalog.entryAt(identity).orElseThrow().glottocode().equals("eng"));
        check(catalog.wordProfileAt(identity).orElseThrow().confidencePermille() == 975);
        check(catalog.lexicalProfileAt(identity).orElseThrow().featureBits() == 8);
        check(SharedLangDexPrecomputeCatalog.builder()
                .entry(huggingFaceIdentity, entry).build().entryCount() == 1);

        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Identity(
                "unicodex.langdex.lexemes", "x", "eng", "x", "r",
                InternDomainV1.CONCEPT, "synexia-canonical-v2",
                "bad".getBytes(StandardCharsets.UTF_8), "0".repeat(64)));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 0L, 1L, 0, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 1L, -1L, 0, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.Entry(
                "eng", "x", 1L, 1L, 0x10000, "x"));
        expect(IllegalArgumentException.class, () -> new M3LangDexPrecompute.WordProfile(
                0L, 0L, 0, 0, 0, 1001));
        expect(IllegalArgumentException.class, () -> identity("unicodex.langdex.lexemes", "x", "en g", "x", "r"));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(identity, entry).entry(identity, entry));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(identity("other", "x", "eng", "Color", "r"), entry));
        expect(IllegalArgumentException.class, () -> SharedLangDexPrecomputeCatalog.builder()
                .entry(identity("unicodex.langdex.lexemes", "x", "eng", "Other", "r"), entry));
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