// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/** Deterministic whole-JDK source/test inventory for A3. */
public final class A3Inv {

    public enum Kind {
        JAVA,
        NATIVE,
        RESOURCE,
        OTHER
    }

    public record Row(
            String tree,
            String module,
            String area,
            String path,
            Kind kind,
            long bytes,
            String sha256) {

        public Row {
            tree = text(tree, "tree");
            module = text(module, "module");
            area = text(area, "area");
            path = text(path, "path");
            kind = Objects.requireNonNull(kind, "kind");
            if (bytes < 0) {
                throw new IllegalArgumentException("bytes");
            }
            if (!Objects.toString(sha256, "").matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("sha256");
            }
        }
    }

    private A3Inv() {
    }

    public static List<Row> scan(Path root) throws IOException {
        Path checkedRoot = A3Fs.root(root);
        ArrayList<Row> rows = new ArrayList<>();
        scanTree(checkedRoot, "src", rows);
        scanTree(checkedRoot, "test", rows);
        rows.sort(Comparator.comparing(Row::path));
        return List.copyOf(rows);
    }

    public static void write(Path root, Path out) throws IOException {
        List<Row> rows = scan(root);
        StringBuilder tsv =
                new StringBuilder(
                        "tree\tmodule\tarea\tpath\tkind\tbytes\tsha256\n");
        for (Row row : rows) {
            tsv.append(A3Fs.cell(row.tree()))
                    .append('\t')
                    .append(A3Fs.cell(row.module()))
                    .append('\t')
                    .append(A3Fs.cell(row.area()))
                    .append('\t')
                    .append(A3Fs.cell(row.path()))
                    .append('\t')
                    .append(row.kind())
                    .append('\t')
                    .append(row.bytes())
                    .append('\t')
                    .append(row.sha256())
                    .append('\n');
        }
        A3Fs.write(root, out, tsv.toString());
    }

    private static void scanTree(
            Path root,
            String tree,
            List<Row> rows) throws IOException {
        Path base = root.resolve(tree);
        if (!Files.isDirectory(base)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(base)) {
            for (Path path :
                    stream.filter(Files::isRegularFile)
                            .sorted()
                            .toList()) {
                byte[] bytes = Files.readAllBytes(path);
                Path rel = root.relativize(path);
                String module = segment(rel, 1, "_");
                String area = segment(rel, 2, "_");
                rows.add(
                        new Row(
                                tree,
                                module,
                                area,
                                rel.toString().replace('\\', '/'),
                                kind(rel.toString()),
                                bytes.length,
                                A3Fs.sha(bytes)));
            }
        }
    }

    private static String segment(
            Path path,
            int index,
            String fallback) {
        return path.getNameCount() > index
                ? path.getName(index).toString()
                : fallback;
    }

    static Kind kind(String path) {
        String name =
                Path.of(Objects.requireNonNull(path, "path"))
                        .getFileName()
                        .toString()
                        .toLowerCase(Locale.ROOT);
        if (name.endsWith(".java")) {
            return Kind.JAVA;
        }
        if (name.endsWith(".c")
                || name.endsWith(".cc")
                || name.endsWith(".cpp")
                || name.endsWith(".cxx")
                || name.endsWith(".h")
                || name.endsWith(".hh")
                || name.endsWith(".hpp")
                || name.endsWith(".s")
                || name.endsWith(".asm")) {
            return Kind.NATIVE;
        }
        if (name.endsWith(".properties")
                || name.endsWith(".xml")
                || name.endsWith(".json")
                || name.endsWith(".yaml")
                || name.endsWith(".yml")
                || name.endsWith(".txt")
                || name.endsWith(".md")
                || name.endsWith(".html")) {
            return Kind.RESOURCE;
        }
        return Kind.OTHER;
    }

    private static String text(String value, String field) {
        String checked = Objects.toString(value, "").strip();
        if (checked.isEmpty()
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0
                || checked.indexOf('\t') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }
}
