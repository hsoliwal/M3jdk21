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

public final class M3SiUnitReceiverMapTest {
    private static int checks;

    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }

    public static void main(String[] args) throws Exception {
        String manifestRow = Files.readAllLines(
                Path.of("lexicon/synexia-source-manifest.tsv"), StandardCharsets.UTF_8)
                .stream()
                .filter(line -> line.startsWith("dictlang.si-units\t"))
                .findFirst()
                .orElseThrow();
        String[] columns = manifestRow.split("\\t", -1);
        check(columns.length >= 9);
        check(columns[5].equals(
                "M3StringFacts + M3LexiconPrecompute.SiUnitPrecompute"
                        + " + SharedLexiconPrecomputeCatalog.SiUnitIdentity"));
        Set<String> fields = Arrays.stream(columns[8].split(","))
                .collect(Collectors.toSet());
        check(fields.equals(Set.of(
                "si_decimal_exponent", "si_dimension_packed", "si_offset",
                "si_prefixable", "source_revision", "source_blob_sha")));

        check(Class.forName("com.m3.text.M3LexiconPrecompute$SiUnitPrecompute") != null);
        check(Class.forName(
                "com.m3.text.SharedLexiconPrecomputeCatalog$SiUnitIdentity") != null);

        List<String> fieldMap = Files.readAllLines(
                Path.of("lexicon/synexia-precompute-field-map.tsv"),
                StandardCharsets.UTF_8);
        for (String field : List.of(
                "decimalExponent", "dimensionPacked", "offset", "prefixable",
                "sourceRevision", "sourceBlobSha")) {
            check(fieldMap.stream().anyMatch(line -> line.startsWith(
                    "M3LexiconPrecompute.SiUnitPrecompute\t" + field + "\t")));
        }
        for (String field : List.of("sourceId", "recordId", "sourceRevision", "sourceBlobSha")) {
            check(fieldMap.stream().anyMatch(line -> line.startsWith(
                    "SharedLexiconPrecomputeCatalog.SiUnitIdentity\t" + field + "\t")));
        }
        System.out.println("M3JDK_SI_UNIT_RECEIVER_MAP_PASS checks=" + checks);
    }
}
