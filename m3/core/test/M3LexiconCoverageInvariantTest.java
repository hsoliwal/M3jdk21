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

    public static void main(String[] args) throws Exception {
        Path sourceManifest = Path.of("lexicon", "synexia-source-manifest.tsv");
        Path fieldMap = Path.of("lexicon", "synexia-precompute-field-map.tsv");
        Path familyMap = Path.of("lexicon", "synexia-precompute-family-map.tsv");
        Set<String> sourceIds = firstColumn(sourceManifest);
        Set<String> mappedFamilies = mappedFamilies(fieldMap);
        Set<String> sidecarFamilies = secondColumn(familyMap);

        check(sourceIds.containsAll(REQUIRED_SOURCE_IDS),
                "source manifest is missing admitted families: "
                        + difference(REQUIRED_SOURCE_IDS, sourceIds));
        check(mappedFamilies.containsAll(REQUIRED_TYPED_FAMILIES),
                "precompute field map is missing typed families: "
                        + difference(REQUIRED_TYPED_FAMILIES, mappedFamilies));
        check(sidecarFamilies.containsAll(REQUIRED_FAMILY_SIDECARS),
                "precompute family map is missing sidecars: "
                        + difference(REQUIRED_FAMILY_SIDECARS, sidecarFamilies));

        System.out.println("M3JDK_LEXICON_PRECOMPUTE_COVERAGE_PASS "
                + "source_families=" + REQUIRED_SOURCE_IDS.size()
                + " typed_families=" + REQUIRED_TYPED_FAMILIES.size());
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
