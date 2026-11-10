/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.SharedLexiconCatalog;
import jdk.m3.text.SharedLexiconCatalog.Coordinate;
import jdk.m3.text.SharedLexiconCatalog.SourceMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Black-box admission contract of {@link SharedLexiconCatalog}: every manifest, mapping,
 * precompute and profile refusal in eager and lazy mode, lazy first-touch verification and
 * repair, lookup routing at shard boundaries, identity lookup with collisions and singletons,
 * lifecycle fencing, and refusal to admit anything when no SHA-256 provider is installed.
 */
public final class SharedLexiconCatalogRefusalContractTest {
    private static final String SHARD_HEADER =
            "shard_id\tfile\tfirst_lexeme\tlast_lexeme\timage_records\tutf16_units\tsha256";
    private static final String RECORD_HEADER = "source_id\tsource_path\tsource_kind\tlanguage_tag\t"
            + "record_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\t"
            + "precompute_profile\tprecompute_payload";
    private static final String FACT_HEADER = "shard_id\timage_row\tutf16_units\tjava_hash\tcode_points\t"
            + "unpaired_surrogates\tnon_bmp_code_points\tascii\tlatin1\tcontains_whitespace\tprecompute_profile";
    private static final String PROFILE_HEADER = "precompute_profile\tsource_records\timage_records\tsha256";
    private static final String SHARDS = "m3.shards.tsv";
    private static final String RECORDS = "m3.records.tsv";
    private static final String FACTS = "m3.precompute.tsv";
    private static final String PROFILES = "m3.precompute-index.tsv";
    private static final List<List<String>> WORDS = List.of(
            List.of("abcd", "abcdefgh", "abce", "b"), List.of("dog", "dove", "zebra"));
    private static int checks;

    private interface Action { void run() throws Exception; }

    /** One exported Synexia mapping row; name and id are raw TSV cells. */
    private record Mapping(String sourceId, String recordId, int shard, int row, String mappingId,
                           String mappingName, String profile, String payload) { }

    /** A written export plus its pristine TSV rows, restorable after each mutation. */
    private record Export(Path root, List<String> shards, List<String> records,
                          List<String> facts, List<String> profiles) {
        void restore() throws IOException {
            write(root.resolve(SHARDS), SHARD_HEADER, shards);
            write(root.resolve(RECORDS), RECORD_HEADER, records);
            write(root.resolve(FACTS), FACT_HEADER, facts);
            write(root.resolve(PROFILES), PROFILE_HEADER, profiles);
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-catalog-refusal-");
        try {
            Export export = mainExport(Files.createDirectory(root.resolve("main")));
            admittedLookups(export);
            manifestRefusals(export);
            imageMetadataRefusals(export);
            recordRefusals(export);
            precomputeRefusals(export);
            profileRefusals(export);
            lazyFirstTouch(export);
            lazyRepair(Files.createDirectory(root.resolve("repair")));
            identityLookups(root);
            singletonLookups(root);
            sha256ProviderAbsent(export);
        } finally {
            delete(root);
        }
        System.out.println("M3_LEXICON_CATALOG_REFUSAL_PASS checks=" + checks);
    }

    private static Export mainExport(Path root) throws Exception {
        List<Mapping> mappings = new ArrayList<>();
        for (int shard = 0; shard < WORDS.size(); shard++) {
            for (int row = 0; row < WORDS.get(shard).size(); row++) {
                String id = shard + "-" + row;
                boolean first = shard == 0 && row == 0;
                mappings.add(new Mapping(shard == 0 ? "src-a" : "src-b", "r" + id, shard, row,
                        first ? "id\"q" : "map-" + id, first ? "\"MAP\"\"X\"" : "NAME-" + id,
                        shard == 0 ? "alpha" : "beta", "{}"));
            }
        }
        mappings.add(new Mapping("src-c", "extra", 0, 0, "map-extra", "\"tab\tname\"", "alpha",
                "\"{\"\"frequency_rank\"\":1}\""));
        return export(root, WORDS, mappings);
    }

    private static void admittedLookups(Export export) throws Exception {
        for (SharedLexiconCatalog catalog : List.of(SharedLexiconCatalog.open(export.root()),
                SharedLexiconCatalog.openLazy(export.root()))) {
            check(catalog.shardCount() == 2 && catalog.recordCount() == 7, "dimensions");
            SourceMapping first = catalog.findMapping("src-a", "r0-0").orElseThrow();
            check(first.mappingName().equals("MAP\"X") && first.mappingId().equals("id\"q"),
                    "doubled quote decodes inside a quoted cell; a mid-cell quote is literal");
            check(catalog.findMapping("src-c", "extra").orElseThrow().mappingName().equals("tab\tname"),
                    "quoted cell retains a tab");
            check(catalog.mappingsAt(new Coordinate(0, 0)).size() == 2, "two identities share one lexeme");
            check(catalog.findPrecomputePayload("src-c", "extra").orElseThrow().requireInt("frequency_rank") == 1,
                    "quoted payload cell keeps doubled JSON quotes");
            check(catalog.findMapping("src-a", "r1-0").isEmpty(), "identity is exact");
            check(catalog.find("b").equals(Optional.of(new Coordinate(0, 3))), "escaped last lexeme");
            check(catalog.find("a").isEmpty(), "before first shard");
            check(catalog.find("c").isEmpty(), "between shards");
            check(catalog.find("zzz").isEmpty(), "after last shard");
            check(catalog.find("abcz").isEmpty(), "absent inside a shard, first UTF-16 lane differs");
            check(catalog.find("abce").equals(Optional.of(new Coordinate(0, 2))), "lane-equal hit");
            check(catalog.find("zebra").equals(Optional.of(new Coordinate(1, 2))), "last record");
            check(catalog.prefix("abcd", 10).equals(List.of(new Coordinate(0, 0), new Coordinate(0, 1))),
                    "prefix over full lanes");
            check(catalog.prefix("abcc", 10).isEmpty(), "lower bound differs in its first lane");
            check(catalog.prefix("abcz", 10).isEmpty(), "lower bound record shorter than the prefix");
            check(catalog.prefix("do", 10).equals(List.of(new Coordinate(1, 0), new Coordinate(1, 1))),
                    "second shard prefix");
            check(catalog.textAt(new Coordinate(1, 1)).equals("dove"), "text at coordinate");
            check(catalog.precomputeAt(new Coordinate(0, 1)).utf16Units() == 8, "facts at coordinate");
            for (Coordinate bad : List.of(new Coordinate(-1, 0), new Coordinate(2, 0),
                    new Coordinate(0, -1), new Coordinate(0, 4))) {
                expect(IndexOutOfBoundsException.class, () -> catalog.textAt(bad), "text " + bad);
                expect(IndexOutOfBoundsException.class, () -> catalog.mappingsAt(bad), "mappings " + bad);
                expect(IndexOutOfBoundsException.class, () -> catalog.precomputeAt(bad), "facts " + bad);
            }
            catalog.close();
            catalog.close();
            expectMessage(IllegalStateException.class, () -> catalog.find("b"), "catalog closed");
            expectMessage(IllegalStateException.class, catalog::warm, "catalog closed");
        }
        expectMessage(IOException.class, () -> SharedLexiconCatalog.open(export.root().resolve(SHARDS)),
                "not a catalog directory");
        expectMessage(IOException.class, () -> SharedLexiconCatalog.openLazy(export.root().resolve(SHARDS)),
                "not a catalog directory");
    }

    private static void manifestRefusals(Export e) throws Exception {
        List<String> rows = e.shards();
        String row0 = rows.get(0);
        refuseRaw(e, SHARDS, "", "invalid shard manifest header");
        refuseRaw(e, SHARDS, "shard\n", "invalid shard manifest header");
        refuse(e, SHARDS, List.of(), "empty shard manifest");
        refuse(e, SHARDS, List.of("", rows.get(1)), "empty row");
        refuse(e, SHARDS, List.of("0\tpart-0.m3lex"), "wrong field count");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 0, "x")), "invalid shard_id");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 0, "-1")), "invalid shard_id");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 0, "1")), "non-contiguous shard_id");
        for (String unsafe : List.of("", ".", "..", "sub/part-0.m3lex", "sub\\part-0.m3lex")) {
            refuse(e, SHARDS, set(rows, 0, cell(row0, 1, unsafe)), "unsafe shard file");
        }
        driveRelativeFileNames(e);
        for (String escape : List.of("\\u006", "\\x0061", "\\u00g1", "a\\")) {
            refuse(e, SHARDS, set(rows, 0, cell(row0, 2, escape)), "invalid UTF-16 sidecar escape");
        }
        refuse(e, SHARDS, set(rows, 0, cell(row0, 4, "x")), "invalid image_records");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 5, "-5")), "invalid utf16_units");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 5, "y")), "invalid utf16_units");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 6, "ab")), "invalid sha256 length");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 6, "z".repeat(64))), "invalid sha256");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 4, "0")), "empty or inverted shard bounds");
        refuse(e, SHARDS, set(rows, 0, cell(row0, 3, "a")), "empty or inverted shard bounds");
        refuse(e, SHARDS, set(rows, 1, cell(rows.get(1), 2, "b")), "shards are not globally UTF-16 sorted");
    }

    /** Windows drive-relative names resolve outside the export; elsewhere they are plain names. */
    private static void driveRelativeFileNames(Export e) throws Exception {
        String row0 = e.shards().get(0);
        char drive = Character.toUpperCase(e.root().getRoot() == null ? 'C'
                : e.root().getRoot().toString().charAt(0));
        char other = drive == 'Q' ? 'R' : 'Q';
        for (String name : List.of(other + ":part-0.m3lex", drive + ":part-0.m3lex")) {
            if (java.io.File.separatorChar == '\\') {
                refuse(e, SHARDS, set(e.shards(), 0, cell(row0, 1, name)), "unsafe shard file");
            } else {
                e.restore();
                write(e.root().resolve(SHARDS), SHARD_HEADER, set(e.shards(), 0, cell(row0, 1, name)));
                SharedLexiconCatalog lazy = SharedLexiconCatalog.openLazy(e.root());
                check(lazy.shardFiles().get(0).equals(name), "colon is an ordinary POSIX file-name character");
                expect(UncheckedIOException.class, () -> lazy.find("abcd"), "absent image fails on first touch");
            }
        }
        e.restore();
    }

    private static void imageMetadataRefusals(Export e) throws Exception {
        Path v1 = e.root().resolve("v1.m3lex");
        jdk.m3.text.SharedLexiconImage.create(v1, WORDS.get(0));
        String row0 = e.shards().get(0);
        String message = "shard metadata does not match image";
        refuseEager(e, set(e.shards(), 0, cell(cell(row0, 1, "v1.m3lex"), 6, sha256(v1))), message);
        e.restore();
        write(e.root().resolve(SHARDS), SHARD_HEADER, set(e.shards(), 0, cell(row0, 4, "5")));
        expectMessage(IOException.class, () -> SharedLexiconCatalog.open(e.root()), message);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.openLazy(e.root()),
                "metadata coverage does not match image records");
        refuseEager(e, set(e.shards(), 0, cell(row0, 5, "18")), message);
        refuseEager(e, set(e.shards(), 0, cell(row0, 2, "abc")), message);
        refuseEager(e, set(e.shards(), 0, cell(row0, 3, "ba")), message);
        refuseEager(e, set(e.shards(), 0, cell(row0, 6, "0".repeat(64))), "shard SHA-256 mismatch");
    }

    private static void recordRefusals(Export e) throws Exception {
        List<String> rows = e.records();
        String row1 = rows.get(1);
        refuseRaw(e, RECORDS, "", "invalid sidecar header: m3.records.tsv");
        refuseRaw(e, RECORDS, "source_id\n", "invalid sidecar header: m3.records.tsv");
        refuse(e, RECORDS, List.of(), "empty m3.records.tsv");
        refuse(e, RECORDS, prepend(rows, ""), "empty sidecar row");
        refuse(e, RECORDS, set(rows, 1, row1.substring(0, row1.lastIndexOf('\t'))), "wrong sidecar field count");
        refuse(e, RECORDS, set(rows, 1, row1 + "\textra"), "wrong sidecar field count");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 9, "")), "empty sidecar field");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 9, "\"\"")), "empty sidecar field");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 9, "\"NAME\"x")), "characters after quoted sidecar field");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 9, "\"NAME\"\"")), "unterminated quoted sidecar field");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 4, "r0-0")), "duplicate source identity");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 6, "2")), "sidecar coordinate outside image");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 7, "4")), "sidecar coordinate outside image");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 6, "-1")), "invalid shard_id");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 7, "x")), "invalid image_row");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 5, "\\u00zz")), "invalid UTF-16 sidecar escape");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 12, "{bad")), "invalid precompute_payload");
        refuse(e, RECORDS, set(rows, 1, cell(row1, 12, "{ }")), "precompute_payload is not canonical JSON");
        refuseEager(e, RECORDS, set(rows, 1, cell(row1, 5, "abce")), "mapping lexeme does not match image coordinate");
        refuse(e, RECORDS, remove(rows, 6), "metadata coverage does not match image records");
    }

    private static void precomputeRefusals(Export e) throws Exception {
        List<String> rows = e.facts();
        String row1 = rows.get(1);
        refuseRaw(e, FACTS, "", "invalid sidecar header: m3.precompute.tsv");
        refuse(e, FACTS, List.of(), "empty m3.precompute.tsv");
        refuse(e, FACTS, set(rows, 2, row1), "duplicate precompute coordinate");
        refuse(e, FACTS, set(rows, 1, cell(row1, 2, "x")), "invalid utf16_units");
        for (String hash : List.of("-1", "4294967296", "x")) {
            refuse(e, FACTS, set(rows, 1, cell(row1, 3, hash)), "invalid java_hash");
        }
        refuse(e, FACTS, set(rows, 1, cell(row1, 4, "-3")), "invalid code_points");
        refuse(e, FACTS, set(rows, 1, cell(row1, 5, "u")), "invalid unpaired_surrogates");
        refuse(e, FACTS, set(rows, 1, cell(row1, 6, "n")), "invalid non_bmp_code_points");
        refuse(e, FACTS, set(rows, 1, cell(row1, 7, "true")), "invalid boolean precompute fact");
        refuse(e, FACTS, set(rows, 1, cell(row1, 9, "FALSE")), "invalid boolean precompute fact");
        refuseEager(e, FACTS, set(rows, 1, cell(row1, 2, "7")), "precompute text facts do not match image");
        refuseEager(e, FACTS, set(rows, 1, cell(row1, 3, "1")), "precompute text facts do not match image");
        refuse(e, FACTS, remove(rows, 6), "metadata coverage does not match image records");
        refuse(e, FACTS, set(rows, 1, cell(row1, 10, "beta")), "precompute owner dropped");
    }

    private static void profileRefusals(Export e) throws Exception {
        List<String> rows = e.profiles();
        String alpha = rows.get(0), beta = rows.get(1);
        String order = "precompute profiles are not strictly sorted or are duplicated";
        refuseRaw(e, PROFILES, "", "invalid sidecar header: m3.precompute-index.tsv");
        refuse(e, PROFILES, List.of(alpha, alpha), order);
        refuse(e, PROFILES, List.of(beta, alpha), order);
        refuse(e, PROFILES, List.of(alpha, beta, "gamma\t1\t1\t" + "0".repeat(64)), "unknown precompute profile");
        refuse(e, PROFILES, List.of(cell(alpha, 1, "4"), beta), "precompute profile cardinality mismatch");
        refuse(e, PROFILES, List.of(cell(alpha, 2, "5"), beta), "precompute profile cardinality mismatch");
        refuse(e, PROFILES, List.of(cell(alpha, 1, "x"), beta), "invalid source_records");
        refuse(e, PROFILES, List.of(cell(alpha, 3, "0".repeat(64)), beta), "precompute profile fingerprint mismatch");
        refuse(e, PROFILES, List.of(cell(alpha, 3, "0"), beta), "invalid sha256 length");
        refuse(e, PROFILES, List.of(), "precompute profile coverage does not match mappings");
        refuse(e, PROFILES, List.of(alpha), "precompute profile coverage does not match mappings");
        e.restore();
    }

    /** Lazy open defers image checks; each first touch verifies, and failures are not cached. */
    private static void lazyFirstTouch(Export e) throws Exception {
        firstTouch(e, RECORDS, set(e.records(), 1, cell(e.records().get(1), 5, "abce")),
                "mapping lexeme does not match image coordinate");
        firstTouch(e, FACTS, set(e.facts(), 1, cell(e.facts().get(1), 2, "7")),
                "precompute text facts do not match image");
        firstTouch(e, FACTS, set(e.facts(), 1, cell(e.facts().get(1), 3, "1")),
                "precompute text facts do not match image");
        firstTouch(e, SHARDS, set(e.shards(), 0, cell(e.shards().get(0), 5, "18")),
                "shard metadata does not match image");
        e.restore();
    }

    private static void firstTouch(Export e, String file, List<String> rows, String fragment) throws Exception {
        e.restore();
        write(e.root().resolve(file), header(file), rows);
        SharedLexiconCatalog lazy = SharedLexiconCatalog.openLazy(e.root());
        for (int attempt = 0; attempt < 2; attempt++) {
            checks++;
            try {
                lazy.find("abcd");
                throw new AssertionError("first touch admitted: " + fragment);
            } catch (UncheckedIOException failure) {
                check(failure.getMessage().contains("unable to open shard 0")
                        && failure.getCause().getMessage().contains(fragment), fragment);
            }
        }
        check(lazy.find("dog").isPresent(), "an unaffected shard still opens: " + fragment);
        lazy.close();
    }

    private static void lazyRepair(Path root) throws Exception {
        Export e = export(root, WORDS, List.of(
                new Mapping("s", "k0", 0, 0, "m", "n", "p", "{}"), new Mapping("s", "k1", 0, 1, "m", "n", "p", "{}"),
                new Mapping("s", "k2", 0, 2, "m", "n", "p", "{}"), new Mapping("s", "k3", 0, 3, "m", "n", "p", "{}"),
                new Mapping("s", "k4", 1, 0, "m", "n", "p", "{}"), new Mapping("s", "k5", 1, 1, "m", "n", "p", "{}"),
                new Mapping("s", "k6", 1, 2, "m", "n", "p", "{}")));
        Path shard = root.resolve("part-0.m3lex");
        byte[] good = Files.readAllBytes(shard);
        byte[] bad = good.clone();
        bad[bad.length - 1] ^= 1;
        Files.write(shard, bad);
        SharedLexiconCatalog lazy = SharedLexiconCatalog.openLazy(root);
        checks++;
        try {
            lazy.find("abcd");
            throw new AssertionError("corrupt shard admitted");
        } catch (UncheckedIOException failure) {
            check(failure.getCause().getMessage().contains("shard SHA-256 mismatch"), "first-touch digest");
        }
        Files.write(shard, good);
        check(lazy.find("abcd").equals(Optional.of(new Coordinate(0, 0))), "repaired shard admitted on retry");
        lazy.warm();
        lazy.close();
    }

    /**
     * Many identities share sources and record IDs, so open addressing must probe past
     * occupied slots and wrap; several table sizes vary where the clusters fall.
     */
    private static void identityLookups(Path parent) throws Exception {
        for (int size : new int[] {24, 40, 50}) {
            Path root = Files.createDirectory(parent.resolve("identity-" + size));
            List<String> words = new ArrayList<>();
            for (int i = 0; i < size; i++) words.add(String.format("w%02d", i));
            List<Mapping> mappings = new ArrayList<>();
            Set<List<String>> present = new HashSet<>();
            for (int row = 0; row < words.size(); row++) {
                for (String source : List.of("s0", "s1", "s2")) {
                    mappings.add(new Mapping(source, "k" + row, 0, row, "m", "n", "p", "{}"));
                    present.add(List.of(source, "k" + row));
                }
            }
            export(root, List.of(words), mappings);
            for (SharedLexiconCatalog catalog : List.of(SharedLexiconCatalog.open(root),
                    SharedLexiconCatalog.openLazy(root))) {
                int found = 0;
                for (String source : List.of("s0", "s1", "s2", "s3", "")) {
                    for (int row = -40; row < 160; row++) {
                        String record = "k" + row;
                        Optional<SourceMapping> mapping = catalog.findMapping(source, record);
                        boolean expected = present.contains(List.of(source, record));
                        check(mapping.isPresent() == expected, "identity oracle " + source + "/" + record);
                        if (expected) {
                            found++;
                            check(mapping.orElseThrow().coordinate().equals(new Coordinate(0, row)),
                                    "identity coordinate");
                        }
                    }
                }
                check(found == mappings.size(), "every admitted identity found");
                catalog.close();
            }
        }
    }

    /** A one-identity catalog compares its single slot directly, wherever that slot is. */
    private static void singletonLookups(Path parent) throws Exception {
        for (int variant = 0; variant < 8; variant++) {
            Path root = Files.createDirectory(parent.resolve("singleton-" + variant));
            String record = "solo-" + variant;
            export(root, List.of(List.of("solo")), List.of(new Mapping("solo-src", record, 0, 0, "m", "n", "p", "{}")));
            SharedLexiconCatalog catalog = SharedLexiconCatalog.openLazy(root);
            check(catalog.findMapping("solo-src", record).orElseThrow().lexeme().equals("solo"), "singleton hit");
            check(catalog.findMapping("solo-src", record + "x").isEmpty(), "singleton record mismatch");
            check(catalog.findMapping("other-src", record).isEmpty(), "singleton source mismatch");
            check(catalog.findMappings("solo").size() == 1 && catalog.findMappings("sol").isEmpty(), "singleton text");
            catalog.close();
        }
    }

    private static void sha256ProviderAbsent(Export e) throws Exception {
        e.restore();
        withoutSha256(() -> {
            expectAssertion(() -> SharedLexiconCatalog.open(e.root()), "eager shard digest");
            expectAssertion(() -> SharedLexiconCatalog.openLazy(e.root()), "lazy profile fingerprint");
        });
        check(SharedLexiconCatalog.openLazy(e.root()).recordCount() == 7, "admission after provider restore");
    }

    private static void withoutSha256(Action body) throws Exception {
        Provider[] installed = Security.getProviders();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < installed.length; i++) {
            if (installed[i].getService("MessageDigest", "SHA-256") != null) positions.add(i);
        }
        for (int position : positions) Security.removeProvider(installed[position].getName());
        try {
            expect(NoSuchAlgorithmException.class, () -> MessageDigest.getInstance("SHA-256"), "provider removed");
            body.run();
        } finally {
            for (int position : positions) Security.insertProviderAt(installed[position], position + 1);
        }
    }

    // ---------------------------------------------------------------- fixture writer

    private static Export export(Path root, List<List<String>> shards, List<Mapping> mappings) throws Exception {
        List<String> shardRows = new ArrayList<>();
        for (int shard = 0; shard < shards.size(); shard++) {
            List<String> words = shards.get(shard);
            Path image = root.resolve("part-" + shard + ".m3lex");
            Files.write(image, image(words), StandardOpenOption.CREATE_NEW);
            long units = words.stream().mapToLong(String::length).sum();
            shardRows.add(shard + "\tpart-" + shard + ".m3lex\t" + words.get(0) + "\t"
                    + escape(words.get(words.size() - 1)) + "\t" + words.size() + "\t" + units + "\t" + sha256(image));
        }
        List<String> recordRows = new ArrayList<>();
        Map<List<Integer>, Set<String>> owners = new TreeMap<>(Comparator.comparing(Object::toString));
        Map<String, Integer> sourceCounts = new TreeMap<>();
        Map<String, Set<List<Integer>>> imageCoordinates = new TreeMap<>();
        for (Mapping m : mappings) {
            String word = shards.get(m.shard()).get(m.row());
            String lexeme = word.equals("b") ? escape(word) : word;
            recordRows.add(String.join("\t", m.sourceId(), "path/" + m.sourceId(), "kind", "en", m.recordId(),
                    lexeme, Integer.toString(m.shard()), Integer.toString(m.row()), m.mappingId(),
                    m.mappingName(), "-", m.profile(), m.payload()));
            List<Integer> coordinate = List.of(m.shard(), m.row());
            owners.computeIfAbsent(coordinate, ignored -> new TreeSet<>()).add(m.profile());
            sourceCounts.merge(m.profile(), 1, Integer::sum);
            imageCoordinates.computeIfAbsent(m.profile(), ignored -> new HashSet<>()).add(coordinate);
        }
        List<String> factRows = new ArrayList<>();
        for (int shard = 0; shard < shards.size(); shard++) {
            for (int row = 0; row < shards.get(shard).size(); row++) {
                String word = shards.get(shard).get(row);
                Set<String> profile = owners.getOrDefault(List.of(shard, row), Set.of("p"));
                factRows.add(shard + "\t" + row + "\t" + word.length() + "\t"
                        + Integer.toUnsignedString(word.hashCode()) + "\t" + word.length()
                        + "\t0\t0\tTrue\tTrue\tFalse\t" + String.join(" + ", profile));
            }
        }
        List<String> profileRows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sourceCounts.entrySet()) {
            int images = imageCoordinates.get(entry.getKey()).size();
            profileRows.add(entry.getKey() + "\t" + entry.getValue() + "\t" + images + "\t"
                    + profileHash(entry.getKey(), entry.getValue(), images));
        }
        Export result = new Export(root, List.copyOf(shardRows), List.copyOf(recordRows),
                List.copyOf(factRows), List.copyOf(profileRows));
        result.restore();
        return result;
    }

    private static byte[] image(List<String> words) throws Exception {
        long units = 0;
        for (String word : words) units += word.length();
        int payload = 64 + 12 * words.size();
        ByteBuffer bytes = ByteBuffer.allocate(Math.toIntExact(payload + 2 * units)).order(ByteOrder.BIG_ENDIAN);
        bytes.putLong(0, 0x4d334c4558303031L).putInt(8, 2).putInt(12, words.size()).putLong(16, payload)
                .putLong(24, units);
        int offset = 0;
        for (int row = 0; row < words.size(); row++) {
            String word = words.get(row);
            int entry = 64 + 12 * row;
            bytes.putInt(entry, offset).putInt(entry + 4, word.length()).putInt(entry + 8, word.hashCode());
            for (int at = 0; at < word.length(); at++) {
                int destination = payload + 2 * (offset + at);
                bytes.put(destination, (byte) word.charAt(at)).put(destination + 1, (byte) (word.charAt(at) >>> 8));
            }
            offset += word.length();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(bytes.array(), 0, 32);
        digest.update(bytes.array(), 64, bytes.capacity() - 64);
        System.arraycopy(digest.digest(), 0, bytes.array(), 32, 32);
        return bytes.array();
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) out.append(String.format("\\u%04X", (int) value.charAt(i)));
        return out.toString();
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }

    private static String profileHash(String profile, long sourceRecords, long imageRecords) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                (profile + "\t" + sourceRecords + "\t" + imageRecords + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    private static String header(String file) {
        return switch (file) {
            case SHARDS -> SHARD_HEADER;
            case RECORDS -> RECORD_HEADER;
            case FACTS -> FACT_HEADER;
            default -> PROFILE_HEADER;
        };
    }

    private static void write(Path path, String header, List<String> rows) throws IOException {
        StringBuilder text = new StringBuilder(header).append('\n');
        for (String row : rows) text.append(row).append('\n');
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }

    private static String cell(String row, int index, String value) {
        String[] cells = row.split("\t", -1);
        cells[index] = value;
        return String.join("\t", cells);
    }

    private static List<String> set(List<String> rows, int index, String row) {
        List<String> copy = new ArrayList<>(rows);
        copy.set(index, row);
        return copy;
    }

    private static List<String> remove(List<String> rows, int index) {
        List<String> copy = new ArrayList<>(rows);
        copy.remove(index);
        return copy;
    }

    private static List<String> prepend(List<String> rows, String row) {
        List<String> copy = new ArrayList<>(rows);
        copy.add(0, row);
        return copy;
    }

    // ---------------------------------------------------------------- refusal helpers

    /** Both eager and lazy admission refuse the mutated sidecar before publishing a catalog. */
    private static void refuse(Export e, String file, List<String> rows, String fragment) throws Exception {
        e.restore();
        write(e.root().resolve(file), header(file), rows);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.open(e.root()), fragment);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.openLazy(e.root()), fragment);
    }

    private static void refuseRaw(Export e, String file, String text, String fragment) throws Exception {
        e.restore();
        Files.writeString(e.root().resolve(file), text, StandardCharsets.UTF_8);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.open(e.root()), fragment);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.openLazy(e.root()), fragment);
    }

    /** Only eager admission verifies image bytes; lazy admission defers the same check. */
    private static void refuseEager(Export e, String file, List<String> rows, String fragment) throws Exception {
        e.restore();
        write(e.root().resolve(file), header(file), rows);
        expectMessage(IOException.class, () -> SharedLexiconCatalog.open(e.root()), fragment);
        SharedLexiconCatalog.openLazy(e.root()).close();
        checks++;
    }

    private static void refuseEager(Export e, List<String> shardRows, String fragment) throws Exception {
        refuseEager(e, SHARDS, shardRows, fragment);
    }

    private static void delete(Path root) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                return;
            } catch (IOException failure) {
                if (attempt == 5) throw failure;
                System.gc();
            }
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Action action, String message) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError(message + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(message + ": no " + type.getSimpleName());
    }

    private static void expectMessage(Class<? extends Throwable> type, Action action, String fragment) {
        checks++;
        try {
            action.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown) && String.valueOf(thrown.getMessage()).contains(fragment)) return;
            throw new AssertionError(fragment + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(fragment + ": no " + type.getSimpleName());
    }

    private static void expectAssertion(Action action, String message) {
        checks++;
        try {
            action.run();
        } catch (AssertionError expected) {
            if (expected.getCause() instanceof NoSuchAlgorithmException) return;
            throw new AssertionError(message + ": wrong cause", expected);
        } catch (Throwable thrown) {
            throw new AssertionError(message + ": unexpected " + thrown, thrown);
        }
        throw new AssertionError(message + ": no AssertionError");
    }
}
