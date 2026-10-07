// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Preserves every current JEP/JBS/capability/algorithm decision while assigning an A3 lane. */
public final class A3Plan {

    public enum Kind {
        JEP,
        JBS,
        CAP,
        ALG
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

    public static List<Row> load(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        ArrayList<Row> rows = new ArrayList<>();
        loadJep(
                checkedRoot.resolve("m3/backports/JEP_CATALOGUE.tsv"),
                rows);
        loadJbs(
                checkedRoot.resolve("m3/backports/UPSTREAM_CHANGE_SEEDS.tsv"),
                rows);
        loadCap(
                checkedRoot.resolve(
                        "m3/backports/COMMUNITY_CAPABILITY_CANDIDATES.tsv"),
                rows);
        loadAlg(checkedRoot, rows);
        rows.sort(
                Comparator.comparing((Row row) -> row.kind().ordinal())
                        .thenComparing(Row::release)
                        .thenComparing(Row::id));
        return List.copyOf(rows);
    }

    public static void write(Path root, Path out) throws IOException {
        StringBuilder tsv =
                new StringBuilder(
                        "kind\trelease\tid\ttitle\tdomain\tdisposition\tlane\tupstream\treason\n");
        for (Row row : load(root)) {
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
        for (String[] fields : data(path, 7)) {
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
        for (String[] fields : data(path, 7)) {
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

    private static void loadCap(Path path, List<Row> rows) throws IOException {
        for (String[] fields : data(path, 10)) {
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

    private static void loadAlg(Path root, List<Row> rows) throws IOException {
        for (A3Alg.Row algorithm : A3Alg.load(root)) {
            rows.add(
                    new Row(
                            Kind.ALG,
                            "",
                            algorithm.atomId(),
                            algorithm.technique(),
                            algorithm.category(),
                            algorithm.disposition(),
                            lane(algorithm.disposition()),
                            algorithm.githubRef(),
                            algorithm.nextProof()));
        }
    }

    private static List<String[]> data(Path path, int minimumColumns)
            throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("missing A3 catalogue: " + path);
        }
        List<String> lines =
                Files.readAllLines(path, StandardCharsets.UTF_8);
        ArrayList<String[]> rows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split("\t", -1);
            if (fields.length < minimumColumns) {
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
        if (value.equals("candidate-high-risk")) {
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
