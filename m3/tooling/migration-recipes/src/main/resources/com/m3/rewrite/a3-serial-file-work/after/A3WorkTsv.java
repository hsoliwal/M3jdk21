// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict TSV/index boundary for {@link A3Work}. */
final class A3WorkTsv {

    private static final String INVENTORY_HEADER =
            "tree\tmodule\tarea\tpath\tkind\tbytes\tsha256";
    private static final String QUEUE_HEADER =
            "order\trelease\tcommit\tjbs_ids\tsubject\tdomain\tinventory_disposition\t"
                    + "risk\tscope_floor\tproof_lane\trecipe_strategy\tpriority\t"
                    + "compatibility_state\tnext_action\tpaths";

    record Feature(
            int order,
            int release,
            String commit,
            String jbsIds,
            String subject,
            String domain,
            String risk,
            String scope,
            String proofLane,
            String recipeStrategy,
            int priority,
            String compatibilityState,
            String nextAction,
            List<String> paths) {}

    private A3WorkTsv() {}

    static Map<String, A3Inv.Row> inventory(Path file) throws IOException {
        List<String> lines = read(file, INVENTORY_HEADER, "A3 inventory");
        Map<String, A3Inv.Row> rows = new HashMap<>();
        for (int index = 1; index < lines.size(); index++) {
            if (!lines.get(index).isBlank()) {
                A3Inv.Row row = inventoryRow(file, index, lines.get(index));
                if (rows.putIfAbsent(row.path(), row) != null) {
                    throw new IOException("duplicate A3 inventory path: " + row.path());
                }
            }
        }
        return Map.copyOf(rows);
    }

    static List<Feature> queue(Path file) throws IOException {
        List<String> lines = read(file, QUEUE_HEADER, "compatibility queue");
        ArrayList<Feature> rows = new ArrayList<>();
        int previous = -1;
        for (int index = 1; index < lines.size(); index++) {
            if (!lines.get(index).isBlank()) {
                Feature row = feature(file, index, lines.get(index));
                if (row.order() <= previous) {
                    throw new IOException(
                            "compatibility queue order is not strictly increasing");
                }
                previous = row.order();
                rows.add(row);
            }
        }
        return List.copyOf(rows);
    }

    static String render(List<A3Work.Row> rows) {
        StringBuilder out =
                new StringBuilder(
                        "feature_order\tfile_order\trelease\tcommit\tjbs_ids\tsubject\t"
                                + "domain\trisk\tjoin_scope\tproof_lane\trecipe_strategy\t"
                                + "priority\tcompatibility_state\tnext_action\tpath\t"
                                + "target_state\tkind\ttarget_sha256\tprepare_lane\tatom_scope\n");
        for (A3Work.Row row : rows) {
            append(out, row);
        }
        return out.toString();
    }

    private static A3Inv.Row inventoryRow(Path file, int index, String line)
            throws IOException {
        String[] cells = cells(file, index, line, 7);
        return new A3Inv.Row(
                cells[0],
                cells[1],
                cells[2],
                A3WorkValues.sourcePath(cells[3]),
                A3Inv.Kind.valueOf(cells[4]),
                A3WorkValues.parseLong(cells[5], "bytes"),
                A3WorkValues.sha256(cells[6]));
    }

    private static Feature feature(Path file, int index, String line)
            throws IOException {
        String[] cells = cells(file, index, line, 15);
        return new Feature(
                A3WorkValues.parseInt(cells[0], "order"),
                A3WorkValues.parseInt(cells[1], "release"),
                A3WorkValues.gitSha(cells[2]),
                A3WorkValues.clean(cells[3]),
                A3WorkValues.text(cells[4], "subject"),
                A3WorkValues.text(cells[5], "domain"),
                A3WorkValues.text(cells[7], "risk"),
                A3WorkValues.scope(cells[8]),
                A3WorkValues.text(cells[9], "proofLane"),
                A3WorkValues.text(cells[10], "recipeStrategy"),
                A3WorkValues.parseInt(cells[11], "priority"),
                A3WorkValues.text(cells[12], "compatibilityState"),
                A3WorkValues.text(cells[13], "nextAction"),
                paths(cells[14]));
    }

    private static String[] cells(
            Path file,
            int index,
            String line,
            int expected) throws IOException {
        String[] cells = line.split("\t", -1);
        if (cells.length != expected) {
            throw new IOException(
                    "malformed TSV row " + (index + 1) + " in " + file);
        }
        return cells;
    }

    private static List<String> paths(String cell) {
        String checked = A3WorkValues.clean(cell);
        if (checked.isEmpty()) {
            return List.of();
        }
        Set<String> unique = new HashSet<>();
        ArrayList<String> values = new ArrayList<>();
        for (String raw : checked.split(",")) {
            String path = A3WorkValues.sourcePath(raw.strip());
            if (!unique.add(path)) {
                throw new IllegalArgumentException(
                        "duplicate compatibility path: " + path);
            }
            values.add(path);
        }
        values.sort(String::compareTo);
        return List.copyOf(values);
    }

    private static List<String> read(Path file, String header, String label)
            throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("missing " + label + ": " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !header.equals(lines.getFirst())) {
            throw new IOException("invalid " + label + " header: " + file);
        }
        return lines;
    }

    private static void append(StringBuilder out, A3Work.Row row) {
        out.append(row.featureOrder())
                .append('\t')
                .append(row.fileOrder())
                .append('\t')
                .append(row.release())
                .append('\t')
                .append(row.commit())
                .append('\t')
                .append(A3Fs.cell(row.jbsIds()))
                .append('\t')
                .append(A3Fs.cell(row.subject()))
                .append('\t')
                .append(A3Fs.cell(row.domain()))
                .append('\t')
                .append(row.risk())
                .append('\t')
                .append(row.joinScope())
                .append('\t')
                .append(row.proofLane())
                .append('\t')
                .append(row.recipeStrategy())
                .append('\t')
                .append(row.priority())
                .append('\t')
                .append(row.compatibilityState())
                .append('\t')
                .append(A3Fs.cell(row.nextAction()))
                .append('\t')
                .append(A3Fs.cell(row.path()))
                .append('\t')
                .append(row.targetState())
                .append('\t')
                .append(row.kind())
                .append('\t')
                .append(row.targetSha256())
                .append('\t')
                .append(row.prepareLane())
                .append('\t')
                .append(row.atomScope())
                .append('\n');
    }

}
