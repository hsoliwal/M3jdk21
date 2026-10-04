// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Preserves JEP/JBS/upstream/capability/challenge evidence while assigning one A3 absorption lane.
 */
public final class A3Plan {

    private static final List<String> JEP_HEADER =
            List.of(
                    "release",
                    "jep",
                    "title",
                    "domain",
                    "disposition",
                    "reason",
                    "superseded_by");
    private static final List<String> JBS_HEADER =
            List.of(
                    "release",
                    "jbs",
                    "title",
                    "component",
                    "disposition",
                    "reason",
                    "upstream_commit");
    private static final List<String> CAP_HEADER =
            List.of(
                    "capability_id",
                    "plane",
                    "candidate",
                    "evidence_type",
                    "evidence_ref",
                    "upstream_join_key",
                    "packaging_candidate",
                    "status",
                    "selected_for_distribution",
                    "next_proof");
    private static final List<String> UPSTREAM_HEADER =
            List.of(
                    "release",
                    "base_ref",
                    "head_ref",
                    "commit",
                    "jbs_ids",
                    "subject",
                    "domain",
                    "javac_touch",
                    "grammar_touch",
                    "hotspot_compiler_touch",
                    "compatibility_signal",
                    "disposition",
                    "paths");
    private static final List<String> ALGO_HEADER =
            List.of(
                    "category_id",
                    "family",
                    "problem_shape",
                    "reference_platforms",
                    "review_order",
                    "jdk_owner_hint",
                    "github_donor",
                    "license",
                    "source_copy_authority",
                    "next_proof");
    private static final String CHALLENGE_REVIEW_ORDER =
            "LEETCODE>HACKERRANK>GEEKSFORGEEKS";

    public enum Kind {
        JEP,
        JBS,
        UPSTREAM,
        CAP,
        ALGO
    }

    public enum Lane {
        ADMIT,
        ADAPT,
        DIRECT,
        SYSTEM,
        HOLD,
        LINEAGE,
        BLOCK,
        REVIEW
    }

    public record Row(
            Kind kind,
            String release,
            String id,
            String title,
            String domain,
            String disposition,
            Lane lane,
            String upstream,
            String reason) {

        public Row {
            kind = Objects.requireNonNull(kind, "kind");
            release = clean(release);
            id = text(id, "id");
            title = text(title, "title");
            domain = text(domain, "domain");
            disposition = text(disposition, "disposition");
            lane = Objects.requireNonNull(lane, "lane");
            upstream = clean(upstream);
            reason = clean(reason);
        }
    }

    private A3Plan() {
    }

    /** Backward-compatible catalogue plan without the generated complete upstream denominator. */
    public static List<Row> load(Path root) throws IOException {
        return load(root, null);
    }

    /**
     * Loads the canonical plan and optionally joins every row from inventory.py's generated
     * released-change denominator.
     */
    public static List<Row> load(Path root, Path upstreamInventory)
            throws IOException {
        Path checkedRoot = A3Fs.root(root);
        ArrayList<Row> rows = new ArrayList<>();
        loadJep(
                checkedRoot.resolve("m3/backports/JEP_CATALOGUE.tsv"),
                rows);
        loadJbs(
                checkedRoot.resolve("m3/backports/UPSTREAM_CHANGE_SEEDS.tsv"),
                rows);
        if (upstreamInventory != null) {
            loadUpstream(
                    A3Fs.source(checkedRoot, upstreamInventory),
                    rows);
        }
        loadCap(
                checkedRoot.resolve(
                        "m3/backports/COMMUNITY_CAPABILITY_CANDIDATES.tsv"),
                rows);
        loadAlgo(
                checkedRoot.resolve(
                        "m3/backports/CHALLENGE_SEARCH_TAXONOMY.tsv"),
                rows);
        rows.sort(
                Comparator.comparing((Row row) -> row.kind().ordinal())
                        .thenComparing(Row::release)
                        .thenComparing(Row::id));
        return List.copyOf(rows);
    }

    public static void write(Path root, Path out) throws IOException {
        write(root, null, out);
    }

    public static void write(
            Path root,
            Path upstreamInventory,
            Path out)
            throws IOException {
        StringBuilder tsv =
                new StringBuilder(
                        "kind\trelease\tid\ttitle\tdomain\tdisposition\tlane\tupstream\treason\n");
        for (Row row : load(root, upstreamInventory)) {
            tsv.append(row.kind())
                    .append('\t')
                    .append(A3Fs.cell(row.release()))
                    .append('\t')
                    .append(A3Fs.cell(row.id()))
                    .append('\t')
                    .append(A3Fs.cell(row.title()))
                    .append('\t')
                    .append(A3Fs.cell(row.domain()))
                    .append('\t')
                    .append(A3Fs.cell(row.disposition()))
                    .append('\t')
                    .append(row.lane())
                    .append('\t')
                    .append(A3Fs.cell(row.upstream()))
                    .append('\t')
                    .append(A3Fs.cell(row.reason()))
                    .append('\n');
        }
        A3Fs.write(root, out, tsv.toString());
    }

    private static void loadJep(Path path, List<Row> rows) throws IOException {
        for (String[] fields : data(path, JEP_HEADER)) {
            rows.add(
                    new Row(
                            Kind.JEP,
                            fields[0],
                            "JEP-" + fields[1],
                            fields[2],
                            fields[3],
                            fields[4],
                            lane(fields[4]),
                            fields[6],
                            fields[5]));
        }
    }

    private static void loadJbs(Path path, List<Row> rows) throws IOException {
        for (String[] fields : data(path, JBS_HEADER)) {
            rows.add(
                    new Row(
                            Kind.JBS,
                            fields[0],
                            fields[1],
                            fields[2],
                            fields[3],
                            fields[4],
                            lane(fields[4]),
                            fields[6],
                            fields[5]));
        }
    }

    private static void loadUpstream(Path path, List<Row> rows)
            throws IOException {
        for (String[] fields : data(path, UPSTREAM_HEADER)) {
            rows.add(
                    new Row(
                            Kind.UPSTREAM,
                            fields[0],
                            fields[3],
                            fields[5],
                            fields[6],
                            fields[11],
                            lane(fields[11]),
                            fields[3],
                            upstreamDetail(fields)));
        }
    }

    private static void loadCap(Path path, List<Row> rows) throws IOException {
        for (String[] fields : data(path, CAP_HEADER)) {
            rows.add(
                    new Row(
                            Kind.CAP,
                            "",
                            fields[0],
                            fields[2],
                            fields[1],
                            fields[7],
                            lane(fields[7]),
                            fields[5],
                            fields[9]));
        }
    }

    private static void loadAlgo(Path path, List<Row> rows)
            throws IOException {
        for (String[] fields : data(path, ALGO_HEADER)) {
            if (!CHALLENGE_REVIEW_ORDER.equals(fields[4])) {
                throw new IOException(
                        "challenge review order drift for " + fields[0]);
            }
            if (!"false".equals(fields[8])) {
                throw new IOException(
                        "challenge source-copy authority must remain false for "
                                + fields[0]);
            }
            rows.add(
                    new Row(
                            Kind.ALGO,
                            "",
                            fields[0],
                            fields[2],
                            fields[1],
                            "reference-only",
                            Lane.REVIEW,
                            fields[6],
                            "platforms="
                                    + fields[3]
                                    + ";order="
                                    + fields[4]
                                    + ";owner="
                                    + fields[5]
                                    + ";license="
                                    + fields[7]
                                    + ";next="
                                    + fields[9]));
        }
    }

    private static String upstreamDetail(String[] fields) {
        return "jbs="
                + fields[4]
                + ";base="
                + fields[1]
                + ";head="
                + fields[2]
                + ";javac="
                + fields[7]
                + ";grammar="
                + fields[8]
                + ";hotspotCompiler="
                + fields[9]
                + ";compatibilitySignal="
                + fields[10]
                + ";paths="
                + fields[12];
    }

    private static List<String[]> data(
            Path path,
            List<String> expectedHeader)
            throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("missing A3 catalogue: " + path);
        }
        List<String> lines =
                Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            throw new IOException("empty A3 catalogue: " + path);
        }
        List<String> actualHeader =
                Arrays.asList(lines.getFirst().split("\t", -1));
        if (!actualHeader.equals(expectedHeader)) {
            throw new IOException(
                    "A3 catalogue header drift in "
                            + path
                            + ": "
                            + actualHeader);
        }

        ArrayList<String[]> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length != expectedHeader.size()) {
                throw new IOException(
                        "malformed A3 catalogue row "
                                + (index + 1)
                                + " in "
                                + path);
            }
            rows.add(fields);
        }
        return rows;
    }

    private static Lane lane(String disposition) {
        String value =
                Objects.toString(disposition, "")
                        .strip()
                        .toLowerCase(Locale.ROOT);
        if (value.equals("admitted")) {
            return Lane.ADMIT;
        }
        if (value.equals("candidate-adapted")) {
            return Lane.ADAPT;
        }
        if (value.equals("candidate")) {
            return Lane.DIRECT;
        }
        if (value.equals("candidate-high-risk")
                || value.startsWith("review-hotspot")) {
            return Lane.SYSTEM;
        }
        if (value.startsWith("hold-")) {
            return Lane.HOLD;
        }
        if (value.startsWith("superseded")) {
            return Lane.LINEAGE;
        }
        if (value.startsWith("reject-")) {
            return Lane.BLOCK;
        }
        return Lane.REVIEW;
    }

    private static String text(String value, String field) {
        String checked = clean(value);
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String clean(String value) {
        return Objects.toString(value, "")
                .strip()
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }
}
