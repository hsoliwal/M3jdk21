/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Proves the five typed family owners are declared in the target manifest. */
public final class SharedLexiconFamilyContractTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(
                Path.of("lexicon/synexia-precompute-field-map.tsv"), StandardCharsets.UTF_8);
        check(!lines.isEmpty(), "field map is non-empty");
        check(lines.get(0).equals(
                "donor_type\tdonor_field\tdonor_java_type\tcanonical_payload_field"
                        + "\tm3jdk_storage\tstatus\tpreservation_rule"),
                "field map header");
        Set<String> expected = Set.of(
                "M3LexiconPrecompute.PrefixCounts|prefixCounts|long[]|prefix_counts|m3lex-family-v2:synexia.prefix-counts.tsv",
                "M3LexiconPrecompute.SpellIndex|deleteToTokenIds|Map<String,int[]>|deleteToTokenIds|m3lex-family-v2:synexia.spell.tsv",
                "M3LexiconPrecompute.SpellIndex|frequencies|Map<Integer,Long>|frequencies|m3lex-family-v2:synexia.spell.tsv",
                "M3LexiconPrecompute.SpellIndex|sourceFingerprint|String|sourceFingerprint|m3lex-family-v2:synexia.spell.tsv",
                "M3LexiconPrecompute.TokenHashPrecompute|tokenSha256|byte[][]|tokenSha256|m3lex-family-v2:synexia.token-hash.tsv",
                "M3LexiconPrecompute.TokenHashPrecompute|rangeFingerprint|RangeFingerprint|rangeFingerprint|m3lex-family-v2:synexia.token-hash.tsv",
                "M3LexiconPrecompute.TokenHashPrecompute|rangeSha256|byte[]|rangeSha256|m3lex-family-v2:synexia.token-hash.tsv",
                "M3LexiconPrecompute.TokenFrequency|frequencies|Map<Integer,Integer>|frequencies|m3lex-family-v2:synexia.token-frequency.tsv",
                "M3LexiconPrecompute.TranslationProjection|translatedTokenIds|int[]|translatedTokenIds|m3lex-family-v2:synexia.translation.tsv",
                "M3LexiconPrecompute.TranslationProjection|mappedTokenCount|int|mappedTokenCount|m3lex-family-v2:synexia.translation.tsv",
                "M3LexiconPrecompute.TranslationProjection|sourceFingerprint|String|sourceFingerprint|m3lex-family-v2:synexia.translation.tsv");
        Set<String> found = new HashSet<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\\t", -1);
            check(fields.length == 7, "field-map row width");
            if (fields[4].startsWith("m3lex-family-v2:")) {
                check(fields[5].equals("MAPPED"), "family field is admitted");
                found.add(String.join("|", fields[0], fields[1], fields[2], fields[3], fields[4]));
            }
        }
        check(found.equals(expected), "all typed family fields are declared exactly once");
        System.out.println("M3JDK_FAMILY_CONTRACT_PASS checks=" + checks
                + " families=5 fields=" + found.size());
    }

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
