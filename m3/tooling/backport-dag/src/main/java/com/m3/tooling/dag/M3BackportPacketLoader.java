// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Strict TSV codec for one M3 backport packet and its small recipe atoms. */
public final class M3BackportPacketLoader {
    public static final String HEADER =
            "packet_id\tatom_id\tscope\tscope_promotion_approved\twork_ref\tdepends_on";

    private M3BackportPacketLoader() {}

    public static M3BackportPacket parse(String tsv) {
        List<String> lines =
                Objects.requireNonNull(tsv, "tsv").lines()
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .toList();
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected packet header");
        }
        if (lines.size() == 1) {
            throw new IllegalArgumentException("empty packet");
        }

        String packetId = null;
        ArrayList<M3RecipeAtom> atoms = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 6) {
                throw new IllegalArgumentException("invalid packet row: " + line);
            }
            if (packetId == null) {
                packetId = cells[0];
            } else if (!packetId.equals(cells[0])) {
                throw new IllegalArgumentException("mixed packet ids");
            }
            List<String> dependencies =
                    cells[5].isBlank()
                            ? List.of()
                            : Arrays.stream(cells[5].split(","))
                                    .map(String::strip)
                                    .filter(value -> !value.isEmpty())
                                    .toList();
            atoms.add(
                    new M3RecipeAtom(
                            cells[1],
                            M3EditScope.valueOf(cells[2]),
                            strictBoolean(cells[3]),
                            cells[4],
                            dependencies));
        }
        return new M3BackportPacket(packetId, atoms);
    }

    private static boolean strictBoolean(String value) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("invalid boolean: " + value);
        };
    }
}
