/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
import com.m3.text.AcronymSidecarCatalog;
import com.m3.text.M3LexiconPrecompute;
import com.m3.text.SharedLexiconPrecomputeCatalog;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays the complete Synexia acronym snapshot through the Java receiver.
 *
 * <p>The sidecar parser test proves the sidecar format. This test proves that
 * the source export itself is not reduced to the single HTTP fixture used by
 * the unit-level contract test.</p>
 */
public final class M3AcronymSnapshotReplayTest {
    private static final String SOURCE_ID = "dictlang.acronyms";
    private static final String SNAPSHOT_SHA256 =
            "4dd2d943eff28044362f843d9e42988e036d5e649910471949ae90666b29e27a";
    private static final String TARGET_MAP_SHA256 = SNAPSHOT_SHA256;
    private static final int EXPECTED_ROWS = 40;
    private static final Map<String, Integer> EXPECTED_DOMAINS = Map.of(
            "computing", 12,
            "data", 6,
            "java", 7,
            "networking", 12,
            "standards", 3);

    public static void main(String[] args) throws Exception {
        Path snapshot = Path.of("lexicon/synexia-acronyms.tsv");
        byte[] snapshotBytes = Files.readAllBytes(snapshot);
        check(SNAPSHOT_SHA256.equals(sha256(snapshotBytes)), "snapshot sha256");

        List<String[]> rows = readSnapshot(snapshot);
        check(rows.size() == EXPECTED_ROWS, "snapshot row count");
        replayFileBackedReceiver(rows, snapshotBytes);
        Map<String, Integer> domains = new LinkedHashMap<>();
        SharedLexiconPrecomputeCatalog.Builder builder =
                SharedLexiconPrecomputeCatalog.builder();
        for (String[] row : rows) {
            check(SOURCE_ID.equals(row[0]), "source id");
            check(row[1].equals(row[4]), "record identity");
            M3LexiconPrecompute.AcronymPrecompute value =
                    new M3LexiconPrecompute.AcronymPrecompute(row[4], row[5], row[6]);
            SharedLexiconPrecomputeCatalog.AcronymIdentity identity =
                    new SharedLexiconPrecomputeCatalog.AcronymIdentity(SOURCE_ID, row[1]);
            builder.acronym(identity, value);
            domains.merge(value.domain(), 1, Integer::sum);
        }

        SharedLexiconPrecomputeCatalog catalog = builder.build();
        check(domains.equals(EXPECTED_DOMAINS), "domain counts");
        for (String[] row : rows) {
            SharedLexiconPrecomputeCatalog.AcronymIdentity identity =
                    new SharedLexiconPrecomputeCatalog.AcronymIdentity(SOURCE_ID, row[1]);
            M3LexiconPrecompute.AcronymPrecompute actual =
                    catalog.acronymAt(identity).orElseThrow();
            check(actual.acronym().equals(row[4]), "lookup acronym");
            check(actual.expansion().equals(row[5]), "lookup expansion");
            check(actual.domain().equals(row[6]), "lookup domain");
        }
        check(catalog.acronymAt(new SharedLexiconPrecomputeCatalog.AcronymIdentity(
                SOURCE_ID, "MISSING")).isEmpty(), "missing lookup");

        validateTargetMap(Path.of("lexicon/synexia-acronym-target-map.tsv"));
        validateProvenance(Path.of("lexicon/synexia-acronym-provenance.json"));
        validateManifest(Path.of("lexicon/synexia-source-manifest.tsv"));

        System.out.println("M3_ACRONYM_SNAPSHOT_REPLAY_PASS rows=" + rows.size()
                + " domains=" + domains);
    }

    private static List<String[]> readSnapshot(Path path) throws Exception {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        check(!text.contains("\r"), "snapshot must use LF");
        String[] lines = text.split("\\n", -1);
        check(lines.length == EXPECTED_ROWS + 2, "snapshot termination");
        check(lines[lines.length - 1].isEmpty(), "snapshot final LF");
        check(lines[0].equals(
                "source_id\trecord_id\tsource_manifest_revision\towner_fingerprint"
                        + "\tacronym\texpansion\tdomain"), "snapshot header");

        List<String[]> rows = new ArrayList<>();
        String previous = null;
        for (int i = 1; i < lines.length - 1; i++) {
            String[] row = lines[i].split("\\t", -1);
            check(row.length == 7, "snapshot row shape");
            check(previous == null || previous.compareTo(row[1]) < 0,
                    "snapshot sorted unique");
            previous = row[1];
            rows.add(row);
        }
        return rows;
    }

    private static void replayFileBackedReceiver(List<String[]> rows, byte[] snapshotBytes)
            throws Exception {
        Path directory = Files.createTempDirectory("m3-acronym-replay-");
        try {
            Files.write(directory.resolve(AcronymSidecarCatalog.DATA_FILE), snapshotBytes);
            String index = "schema_version\\tfile\\trows\\tsha256\\n"
                    + AcronymSidecarCatalog.SCHEMA_VERSION + "\\t"
                    + AcronymSidecarCatalog.DATA_FILE + "\\t" + EXPECTED_ROWS + "\\t"
                    + sha256(snapshotBytes) + "\\n";
            Files.writeString(directory.resolve(AcronymSidecarCatalog.INDEX_FILE), index,
                    StandardCharsets.UTF_8);

            AcronymSidecarCatalog sidecar = AcronymSidecarCatalog.open(directory);
            check(sidecar.size() == EXPECTED_ROWS, "file-backed receiver row count");
            check(sidecar.scope().sourceManifestRevision().equals(rows.get(0)[2]),
                    "file-backed receiver source revision");
            check(sidecar.scope().ownerFingerprint().equals(rows.get(0)[3]),
                    "file-backed receiver owner fingerprint");

            SharedLexiconPrecomputeCatalog.Builder builder =
                    SharedLexiconPrecomputeCatalog.builder();
            for (String[] row : rows) {
                AcronymSidecarCatalog.Entry entry = sidecar.find(row[1]).orElseThrow();
                check(entry.value().equals(new M3LexiconPrecompute.AcronymPrecompute(
                        row[4], row[5], row[6])), "file-backed receiver value");
                builder.acronym(
                        new SharedLexiconPrecomputeCatalog.AcronymIdentity(SOURCE_ID, row[1]),
                        entry.value());
            }
            SharedLexiconPrecomputeCatalog catalog = builder.build();
            for (String[] row : rows) {
                check(catalog.acronymAt(
                        new SharedLexiconPrecomputeCatalog.AcronymIdentity(SOURCE_ID, row[1]))
                        .orElseThrow().expansion().equals(row[5]),
                        "file-backed receiver catalog lookup");
            }
        } finally {
            Files.walk(directory)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (java.io.IOException failure) {
                            throw new RuntimeException(failure);
                        }
                    });
        }
    }

    private static void validateTargetMap(Path path) throws Exception {
        String[] lines = Files.readString(path, StandardCharsets.UTF_8)
                .split("\\n", -1);
        check(lines.length == 3 && lines[2].isEmpty(), "target map shape");
        String[] header = lines[0].split("\\t", -1);
        String[] row = lines[1].split("\\t", -1);
        check(header.length == 15 && row.length == header.length, "target map columns");
        check(row[0].equals(SOURCE_ID), "target map source");
        check(row[3].equals("acronym_id"), "target map record id");
        check(row[4].equals("expansion"), "target map mapping field");
        check(row[5].equals("M3LexiconPrecompute.AcronymPrecompute"),
                "target map owner");
        check(row[6].equals("m3lex-acronym-v1"), "target map protocol");
        check(row[7].equals("Apache-2.0"), "target map license");
        check(row[8].equals("STAGED_PROVEN"), "target map status");
        check(row[9].equals("m3/lexicon/synexia-acronyms.tsv"),
                "target map snapshot path");
        check(row[10].equals("m3/lexicon/synexia-acronym-provenance.json"),
                "target map provenance path");
        check(row[11].equals(TARGET_MAP_SHA256), "target map snapshot sha");
        check(row[12].equals(Integer.toString(EXPECTED_ROWS)), "target map count");
        check(row[13].equals(
                "computing=12;data=6;java=7;networking=12;standards=3"),
                "target map domains");
    }

    private static void validateProvenance(Path path) throws Exception {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        check(json.contains("\"source_repository\": \"https://github.com/hsoliwal/com.synexia\""),
                "provenance repository");
        check(json.contains("\"source_license\": \"Apache-2.0\""),
                "provenance license");
        check(json.contains("\"snapshot_sha256\": \"" + SNAPSHOT_SHA256 + "\""),
                "provenance snapshot sha");
        check(json.contains("\"record_count\": 40"), "provenance count");
        check(json.contains("\"computing\": 12")
                        && json.contains("\"data\": 6")
                        && json.contains("\"java\": 7")
                        && json.contains("\"networking\": 12")
                        && json.contains("\"standards\": 3"),
                "provenance domains");
        check(json.contains("\"status\": \"STAGED_PROVEN\""),
                "provenance status");
    }

    private static void validateManifest(Path path) throws Exception {
        String row = Arrays.stream(Files.readString(path, StandardCharsets.UTF_8)
                        .split("\\n", -1))
                .filter(line -> line.startsWith(SOURCE_ID + "\t"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("acronym manifest row missing"));
        String[] columns = row.split("\\t", -1);
        check(columns.length == 9, "acronym manifest columns");
        check(columns[3].equals("acronym_id"), "acronym manifest identity");
        check(columns[4].equals("expansion"), "acronym manifest mapping");
        check(columns[5].equals("M3StringFacts + AcronymPrecompute"),
                "acronym manifest owner");
        check(columns[6].equals("Apache-2.0"), "acronym manifest license");
        check(columns[8].equals("expansion"), "acronym manifest precompute");
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
