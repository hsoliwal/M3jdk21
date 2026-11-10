/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
import jdk.m3.text.SharedLexiconCatalog.Coordinate;
import jdk.m3.text.SharedRelatedLexemeCatalog;
import jdk.m3.text.SharedRelatedLexemeCatalog.Relation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Black-box contract of the directed related-lexeme sidecar: quoted TSV and escape decoding,
 * identity and source-range lookups, lifecycle fencing, and every refusal of the mapping,
 * admitted-source and relation files.
 */
public final class SharedRelatedLexemeCatalogContractTest {
    private static final String RECORDS = "m3.records.tsv";
    private static final String SOURCES = "m3.related-sources.tsv";
    private static final String RELATED = SharedRelatedLexemeCatalog.FILE;
    private static final String RECORD_HEADER = "source_id\tsource_path\tsource_kind\tlanguage_tag\t"
            + "record_id\tlexeme\tshard_id\timage_row\tmapping_id\tmapping_name\ttranslation_profile\t"
            + "precompute_profile\tprecompute_payload";
    private static final String RELATED_HEADER =
            "source_id\trecord_id\tlexeme\trelated_lexeme\tshard_id\timage_row";
    private static final List<String> RECORD_ROWS = List.of(
            mapping("src-a", "r1", "alpha", 0, 0),
            mapping("src-a", "r2", "beta", 0, 1),
            mapping("\"src-b\"", "r1", "gamma", 0, 2),
            mapping("src-b", "r2", "back\\\\slash", 0, 3),
            mapping("src-c", "r1", "\\u0064elta", 1, 0));
    private static final List<String> SOURCE_ROWS = List.of("src-a", "src-b");
    private static final List<String> RELATED_ROWS = List.of(
            "src-a\tr1\talpha\tomega\t0\t0",
            "src-a\tr1\talpha\tzeta\t0\t0",
            "src-a\tr2\tbeta\tth\"eta\t0\t1",
            "src-b\tr1\tgamma\t\"x\"\"y\"\t0\t2",
            "src-b\tr2\tback\\\\slash\t\\u0041\t0\t\"3\"");
    private static int checks;

    private interface Action { void run() throws Exception; }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("m3lex-related-contract-");
        try {
            relationRecord();
            admitted(root);
            optionalPresence(root);
            mappingRefusals(root);
            sourceRefusals(root);
            relationRefusals(root);
        } finally {
            delete(root);
        }
        System.out.println("M3_RELATED_LEXEME_CONTRACT_PASS checks=" + checks);
    }

    private static void relationRecord() {
        Coordinate at = new Coordinate(0, 0);
        check(new Relation("s", "r", "l", "x", at).identity().recordId().equals("r"), "identity view");
        expectMessage(IllegalArgumentException.class, () -> new Relation("", "r", "l", "x", at), "empty relation");
        expectMessage(IllegalArgumentException.class, () -> new Relation("s", "", "l", "x", at), "empty relation");
        expectMessage(IllegalArgumentException.class, () -> new Relation("s", "r", "", "x", at), "empty relation");
        expectMessage(IllegalArgumentException.class, () -> new Relation("s", "r", "l", "", at), "empty relation");
        expect(NullPointerException.class, () -> new Relation("s", "r", "l", "x", null), "coordinate required");
        expect(NullPointerException.class, () -> new Relation(null, "r", "l", "x", at), "source required");
    }

    private static void admitted(Path root) throws Exception {
        write(root, RECORDS, RECORD_HEADER, RECORD_ROWS);
        write(root, SOURCES, "source_id", SOURCE_ROWS);
        write(root, RELATED, RELATED_HEADER, RELATED_ROWS);
        SharedRelatedLexemeCatalog catalog = SharedRelatedLexemeCatalog.openOptional(root).orElseThrow();
        check(catalog.relations().size() == 5, "all relations admitted in order");
        check(catalog.find("src-a", "r1").isEmpty(), "two targets are ambiguous for find");
        check(catalog.findAll("src-a", "r1").stream().map(Relation::relatedLexeme).toList()
                .equals(List.of("omega", "zeta")), "findAll keeps both directed targets");
        check(catalog.find("src-a", "r2").orElseThrow().relatedLexeme().equals("th\"eta"), "mid-cell quote is literal");
        check(catalog.find("src-b", "r1").orElseThrow().relatedLexeme().equals("x\"y"), "doubled quote decodes");
        Relation escaped = catalog.find("src-b", "r2").orElseThrow();
        check(escaped.lexeme().equals("back\\slash") && escaped.relatedLexeme().equals("A")
                && escaped.coordinate().equals(new Coordinate(0, 3)), "escapes and quoted final cell");
        check(catalog.find("src-c", "r1").isEmpty() && catalog.findAll("src-z", "r1").isEmpty(), "unrelated identity");
        check(catalog.findBySource("src-a").size() == 3, "source range");
        check(catalog.findBySource("src-b").size() == 2, "last source range");
        for (String absent : List.of("", "src-0", "src-ab", "src-z")) {
            check(catalog.findBySource(absent).isEmpty(), "empty source range " + absent);
        }
        expect(NullPointerException.class, () -> catalog.findBySource(null), "source required");
        expect(UnsupportedOperationException.class, () -> catalog.relations().clear(), "immutable relations");
        catalog.close();
        for (Action closed : List.<Action>of(catalog::relations, () -> catalog.find("src-a", "r2"),
                () -> catalog.findAll("src-a", "r2"), () -> catalog.findBySource("src-a"))) {
            expectMessage(IllegalStateException.class, closed, "related catalog closed");
        }
    }

    private static void optionalPresence(Path root) throws Exception {
        Path empty = Files.createDirectory(root.resolve("legacy"));
        write(empty, RECORDS, RECORD_HEADER, RECORD_ROWS);
        check(SharedRelatedLexemeCatalog.openOptional(empty).isEmpty(), "legacy export has no related sidecar");
        write(empty, SOURCES, "source_id", SOURCE_ROWS);
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.openOptional(empty),
                "related sidecar missing");
        Files.delete(empty.resolve(SOURCES));
        write(empty, RELATED, RELATED_HEADER, RELATED_ROWS);
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.openOptional(empty),
                "related source sidecar missing");
        Files.delete(empty.resolve(RECORDS));
        write(empty, SOURCES, "source_id", SOURCE_ROWS);
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.open(empty), "mapping sidecar missing");
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.open(empty.resolve(SOURCES)),
                "not a catalog directory");
    }

    private static void mappingRefusals(Path root) throws Exception {
        String row = RECORD_ROWS.get(1);
        refuseRaw(root, RECORDS, "", "invalid mapping sidecar header");
        refuseRaw(root, RECORDS, "source_id\n", "invalid mapping sidecar header");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, row + "\textra"), "mapping field count");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 0, "")), "empty mapping identity");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 4, "")), "empty mapping identity");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 4, "r1")), "duplicate mapping identity");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 6, "-1")), "invalid shard_id");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 7, "row")), "invalid image_row");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 5, "\"be\"ta")), "invalid quoted TSV field");
        refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 5, "\"beta")), "unterminated quoted TSV field");
        for (String escape : List.of("b\\", "b\\x0041", "b\\u004", "b\\uZZZZ")) {
            refuse(root, RECORDS, set(RECORD_ROWS, 1, cell(row, 5, escape)), "invalid UTF-16 escape");
        }
    }

    private static void sourceRefusals(Path root) throws Exception {
        String header = "invalid related source sidecar header";
        refuseRaw(root, SOURCES, "", header);
        refuseRaw(root, SOURCES, "source\nsrc-a\n", header);
        refuse(root, SOURCES, List.of(), "empty related source sidecar");
        refuse(root, SOURCES, List.of("src-a\tsrc-b"), "invalid related source identity");
        refuse(root, SOURCES, List.of("src-a", ""), "invalid related source identity");
        refuse(root, SOURCES, List.of("src-b", "src-a"), "related sources are not sorted or are duplicated");
        refuse(root, SOURCES, List.of("src-a", "src-a"), "related sources are not sorted or are duplicated");
    }

    private static void relationRefusals(Path root) throws Exception {
        List<String> rows = RELATED_ROWS;
        String first = rows.get(0);
        refuseRaw(root, RELATED, "", "invalid related sidecar header");
        refuseRaw(root, RELATED, "source_id\trecord_id\n", "invalid related sidecar header");
        refuse(root, RELATED, set(rows, 0, first + "\t0"), "wrong field count");
        refuse(root, RELATED, set(rows, 0, cell(first, 0, "src-c")), "relation source family is not admitted");
        String order = "relations are not sorted or are duplicated";
        refuse(root, RELATED, List.of(rows.get(1), rows.get(0)), order);
        refuse(root, RELATED, List.of(rows.get(0), rows.get(0)), order);
        refuse(root, RELATED, List.of(rows.get(2), rows.get(0)), order);
        refuse(root, RELATED, List.of(rows.get(3), rows.get(0)), order);
        refuse(root, RELATED, List.of(first, cell(rows.get(1), 2, "alph")), order);
        refuse(root, RELATED, set(rows, 0, cell(first, 1, "r7")), "relation identity is not mapped");
        refuse(root, RELATED, List.of(first, cell(rows.get(1), 2, "alphb")), "relation mapping mismatch");
        refuse(root, RELATED, set(rows, 0, cell(first, 4, "1")), "relation mapping mismatch");
        refuse(root, RELATED, set(rows, 0, cell(first, 5, "1")), "relation mapping mismatch");
        refuse(root, RELATED, set(rows, 0, cell(first, 4, "-1")), "invalid shard_id");
        refuse(root, RELATED, set(rows, 0, cell(first, 5, "x")), "invalid image_row");
        refuse(root, RELATED, set(rows, 0, cell(first, 3, "\\u12")), "invalid UTF-16 escape");
        write(root, RELATED, RELATED_HEADER, RELATED_ROWS);
    }

    private static String mapping(String source, String record, String lexeme, int shard, int row) {
        return String.join("\t", source, "path", "kind", "en", record, lexeme, Integer.toString(shard),
                Integer.toString(row), "map", "name", "-", "profile", "{}");
    }

    /** Writes the mutated file over a pristine fixture and expects the exact refusal. */
    private static void refuse(Path root, String file, List<String> rows, String fragment) throws Exception {
        pristine(root);
        write(root, file, header(file), rows);
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.open(root), fragment);
    }

    private static void refuseRaw(Path root, String file, String text, String fragment) throws Exception {
        pristine(root);
        Files.writeString(root.resolve(file), text, StandardCharsets.UTF_8);
        expectMessage(IOException.class, () -> SharedRelatedLexemeCatalog.open(root), fragment);
    }

    private static void pristine(Path root) throws IOException {
        write(root, RECORDS, RECORD_HEADER, RECORD_ROWS);
        write(root, SOURCES, "source_id", SOURCE_ROWS);
        write(root, RELATED, RELATED_HEADER, RELATED_ROWS);
    }

    private static String header(String file) {
        return switch (file) {
            case RECORDS -> RECORD_HEADER;
            case SOURCES -> "source_id";
            default -> RELATED_HEADER;
        };
    }

    private static void write(Path root, String file, String header, List<String> rows) throws IOException {
        StringBuilder text = new StringBuilder(header).append('\n');
        for (String row : rows) text.append(row).append('\n');
        Files.writeString(root.resolve(file), text, StandardCharsets.UTF_8);
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

    private static void delete(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
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
}
