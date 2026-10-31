// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Strict TSV codec for recipe-atom documentation/pattern/IOP/JUnit evidence. */
public final class M3BackportPacketEvidenceLoader {
    public static final String HEADER =
            "packet_id\tatom_id\tcontract_ref\tdocumentation_ref\tpattern\tiop_role"
                    + "\tjunit_proof_ref\tfixed_point_required";

    private M3BackportPacketEvidenceLoader() {}

    public static M3BackportPacketEvidence parse(String tsv) {
        List<String> lines =
                Objects.requireNonNull(tsv, "tsv").lines()
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .toList();
        if (lines.isEmpty() || !HEADER.equals(lines.getFirst())) {
            throw new IllegalArgumentException("unexpected packet evidence header");
        }
        if (lines.size() == 1) {
            throw new IllegalArgumentException("empty packet evidence");
        }

        String packetId = null;
        ArrayList<M3RecipeAtomEvidence> atoms = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 8) {
                throw new IllegalArgumentException("invalid packet evidence row: " + line);
            }
            if (packetId == null) {
                packetId = cells[0];
            } else if (!packetId.equals(cells[0])) {
                throw new IllegalArgumentException("mixed packet evidence ids");
            }
            atoms.add(
                    new M3RecipeAtomEvidence(
                            cells[1],
                            cells[2],
                            cells[3],
                            cells[4],
                            cells[5],
                            cells[6],
                            strictBoolean(cells[7])));
        }
        return new M3BackportPacketEvidence(packetId, atoms);
    }

    private static boolean strictBoolean(String value) {
        return switch (value) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("invalid boolean: " + value);
        };
    }
}
