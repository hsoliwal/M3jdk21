// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Deterministic TSV loader for M3 backport recipe DAGs. */
public final class M3BackportDagLoader {
    private static final String ROOT = "/com/m3/tooling/dag/";

    private M3BackportDagLoader() {}

    public static M3RecipeDag load(String resource) {
        String text = resource(resource);
        List<String> lines = text.lines()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .toList();
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("empty DAG resource: " + resource);
        }
        String expectedHeader =
                "id\tkind\tscope\tmutating\tserial_promotion\tscope_promotion_approved\twork_ref\tdepends_on";
        if (!expectedHeader.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected DAG header");
        }

        ArrayList<M3DagNode> nodes = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\\t", -1);
            if (cells.length != 8) {
                throw new IllegalArgumentException("invalid DAG row: " + line);
            }
            List<String> dependencies = cells[7].isBlank()
                    ? List.of()
                    : Arrays.stream(cells[7].split(","))
                            .map(String::strip)
                            .filter(value -> !value.isEmpty())
                            .toList();
            nodes.add(new M3DagNode(
                    cells[0],
                    M3DagKind.valueOf(cells[1]),
                    M3EditScope.valueOf(cells[2]),
                    Boolean.parseBoolean(cells[3]),
                    Boolean.parseBoolean(cells[4]),
                    Boolean.parseBoolean(cells[5]),
                    cells[6],
                    dependencies));
        }
        return new M3RecipeDag(nodes);
    }

    private static String resource(String name) {
        String checked = Objects.requireNonNull(name, "name").strip();
        if (checked.isEmpty() || checked.contains("..") || checked.indexOf('/') >= 0) {
            throw new IllegalArgumentException("invalid DAG resource: " + name);
        }
        try (InputStream input = M3BackportDagLoader.class.getResourceAsStream(ROOT + checked)) {
            if (input == null) {
                throw new IllegalArgumentException("missing DAG resource: " + checked);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read DAG resource", failure);
        }
    }
}
