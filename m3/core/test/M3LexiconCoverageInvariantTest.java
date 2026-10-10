/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Mechanical coverage gate for the Synexia-to-M3JDK precompute manifest.
 *
 * <p>This test checks that every admitted source family has a manifest row and
 * that every typed precompute family has at least one mapped field. It does
 * not synthesize records, download datasets, or promote mapping-only
 * sidecars into image payload.</p>
 */
public final class M3LexiconCoverageInvariantTest {
    private static final Set<String> REQUIRED_SOURCE_IDS = Set.of(
            "dictlang.dictionary",
            "dictlang.frequency",
            "dictlang.thesaurus",
            "dictlang.antonyms",
            "dictlang.huggingface",
            "unicodex.langdex.lexemes",
            "translate.rows",
            "dictlang.si-units",
            "dictlang.acronyms",
            "dictlang.numbers.0-10000",
            "translate.index-phrases",
            "unicodex.instance-index");

    private static final Set<String> REQUIRED_FAMILY_SIDECARS = Set.of(
            "prefix-counts",
            "spell-index",
            "token-frequency",
            "token-hash-precompute",
            "translation-projection");

    private static final Set<String> REQUIRED_TYPED_FAMILIES = Set.of(
            "IndexWordFacts",
            "IndexWordSignal",
            "M3LangDexPrecompute.Entry",
            "M3LangDexPrecompute.WordProfile",
            "M3LangDexPrecompute.LexicalProfile",
            "M3LangDexPrecompute.Translation",
            "M3LangDexPrecompute.Identity",
            "M3LangDexPrecompute.TranslationIdentity",
            "SiUnit",
            "SiDimension",
            "M3LexiconPrecompute.SiUnitPrecompute",
            "M3NumberSpace",
            "M3LanguageGrammarSupport",
            "M3LexiconPrecompute.PrefixCounts",
            "M3LexiconPrecompute.SpellIndex",
            "M3LexiconPrecompute.TokenHashPrecompute",
            "M3LexiconPrecompute.TokenFrequency",
            "M3LexiconPrecompute.TranslationProjection",
            "M3InstanceIndexPrecompute.InstanceRecord",
            "M3InstanceIndexPrecompute",
            "M3PhrasePrecompute.Scope",
            "M3PhrasePrecompute.Phrase",
            "SharedLexiconPrecomputeCatalog.TranslationIdentity",
            "AcronymPrecompute");

    private static final Set<String> REQUIRED_LANGDEX_IDENTITY_FIELDS = Set.of(
            "langdex_source_id",
            "langdex_record_id",
            "langdex_glottocode",
            "langdex_surface",
            "langdex_source_revision",
            "langdex_domain",
            "langdex_canonical_schema",
            "langdex_canonical_bytes",
            "langdex_canonical_digest",
            "langdex_concept_id",
            "langdex_source_glottocode",
            "langdex_source_surface",
            "langdex_target_glottocode",
            "langdex_target_surface");

    public static void main(String[] args) throws Exception {
        Path sourceManifest = Path.of("lexicon", "synexia-source-manifest.tsv");
        Path fieldMap = Path.of("lexicon", "synexia-precompute-field-map.tsv");
        Path familyMap = Path.of("lexicon", "synexia-precompute-family-map.tsv");
        Path langDexIdentityMap = Path.of("lexicon", "synexia-langdex-identity-field-map.tsv");
        Path numberTargetMap = Path.of("lexicon", "synexia-number-target-map.tsv");
        Path acronymTargetMap = Path.of("lexicon", "synexia-acronym-target-map.tsv");
        Path instanceTargetMap = Path.of("lexicon", "synexia-instance-target-map.tsv");
        Set<String> sourceIds = firstColumn(sourceManifest);
        Set<String> mappedFamilies = mappedFamilies(fieldMap);
        Set<String> mappedPayloadFields = mappedPayloadFields(fieldMap);
        Set<String> requiredPayloadFields = precomputeFields(sourceManifest);
        Set<String> sidecarFamilies = secondColumn(familyMap);
        Set<String> mappedLangDexIdentityFields = langDexIdentityFields(langDexIdentityMap);
        Set<String> numberTargets = numberTargetRows(numberTargetMap);
        Set<String> acronymTargets = acronymTargetRows(acronymTargetMap);
        Set<String> instanceTargets = instanceTargetRows(instanceTargetMap);

        check(sourceIds.containsAll(REQUIRED_SOURCE_IDS),
                "source manifest is missing admitted families: "
                        + difference(REQUIRED_SOURCE_IDS, sourceIds));
        check(mappedFamilies.containsAll(REQUIRED_TYPED_FAMILIES),
                "precompute field map is missing typed families: "
                        + difference(REQUIRED_TYPED_FAMILIES, mappedFamilies));
        check(mappedPayloadFields.containsAll(requiredPayloadFields),
                "source manifest fields are missing M3JDK mappings: "
                        + difference(requiredPayloadFields, mappedPayloadFields));
        check(sidecarFamilies.containsAll(REQUIRED_FAMILY_SIDECARS),
                "precompute family map is missing sidecars: "
                        + difference(REQUIRED_FAMILY_SIDECARS, sidecarFamilies));
        check(mappedLangDexIdentityFields.containsAll(REQUIRED_LANGDEX_IDENTITY_FIELDS),
                "LangDex identity map is missing canonical fields: "
                        + difference(REQUIRED_LANGDEX_IDENTITY_FIELDS, mappedLangDexIdentityFields));
        check(numberTargets.contains("dictlang.numbers.0-10000\tcom.m3.text.M3NumberSpace\tADMITTED_TYPED_RECEIVER"),
                "number target map is not admitted to M3NumberSpace");
        check(acronymTargets.contains("dictlang.acronyms\tM3LexiconPrecompute.AcronymPrecompute\tSTAGED_PROVEN\t40"),
                "acronym target map is not source-pinned");
        check(instanceTargets.contains("proper-nouns\tcom.m3.text.M3InstanceIndex\tTARGET_CONTRACT_OPEN_NO_PAYLOAD\tREFERENCE_ONLY_PAYLOAD_NOT_ADMITTED")
                        && instanceTargets.contains("titles\tcom.m3.text.M3InstanceIndex\tTARGET_CONTRACT_OPEN_NO_PAYLOAD\tREFERENCE_ONLY_PAYLOAD_NOT_ADMITTED"),
                "proper-name/title target map does not preserve reference-only status");

        System.out.println("M3JDK_LEXICON_PRECOMPUTE_COVERAGE_PASS "
                + "source_families=" + REQUIRED_SOURCE_IDS.size()
                + " typed_families=" + REQUIRED_TYPED_FAMILIES.size()
                + " payload_fields=" + requiredPayloadFields.size()
                + " langdex_identity_fields=" + REQUIRED_LANGDEX_IDENTITY_FIELDS.size()
                + " target_maps=3");
    }

    private static Set<String> firstColumn(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("source_id\t")) {
                continue;
            }
            values.add(line.split("\\t", -1)[0]);
        }
        return values;
    }

    private static Set<String> secondColumn(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("schema_version\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length >= 2) {
                values.add(fields[1]);
            }
        }
        return values;
    }

    private static Set<String> precomputeFields(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("source_id\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 9) {
                throw new AssertionError("source manifest row has no precompute field column: " + line);
            }
            for (String field : fields[8].split(",", -1)) {
                if (!field.isBlank() && !field.equals("-")) {
                    values.add(field);
                }
            }
        }
        return values;
    }

    private static Set<String> mappedPayloadFields(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("donor_type\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 6) {
                throw new AssertionError("precompute field row is malformed: " + line);
            }
            if (fields[5].equals("MAPPED")) {
                values.add(fields[3]);
            }
        }
        return values;
    }

    private static Set<String> langDexIdentityFields(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("target_type\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 7
                    || !fields[4].equals("true")
                    || !fields[5].equals("m3langdex-family-v1:synexia.langdex.tsv")
                    || !(fields[0].equals("M3LangDexPrecompute.Identity")
                        || fields[0].equals("M3LangDexPrecompute.TranslationIdentity"))) {
                throw new AssertionError("invalid LangDex identity-map row: " + line);
            }
            values.add(fields[3]);
        }
        return values;
    }

    private static Set<String> numberTargetRows(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("schema\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 11) {
                throw new AssertionError("number target row is malformed: " + line);
            }
            values.add(fields[1] + "\t" + fields[6] + "\t" + fields[9]);
        }
        return values;
    }

    private static Set<String> acronymTargetRows(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("source_id\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 13) {
                throw new AssertionError("acronym target row is malformed: " + line);
            }
            values.add(fields[0] + "\t" + fields[5] + "\t" + fields[8] + "\t" + fields[12]);
        }
        return values;
    }

    private static Set<String> instanceTargetRows(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("synexia_capability\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length < 6) {
                throw new AssertionError("instance target row is malformed: " + line);
            }
            values.add(fields[0] + "\t" + fields[2] + "\t" + fields[4] + "\t" + fields[5]);
        }
        return values;
    }

    private static Set<String> mappedFamilies(Path path) throws Exception {
        Set<String> values = new HashSet<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("donor_type\t")) {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (fields.length >= 6 && fields[5].equals("MAPPED")) {
                values.add(fields[0]);
            }
        }
        return values;
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
