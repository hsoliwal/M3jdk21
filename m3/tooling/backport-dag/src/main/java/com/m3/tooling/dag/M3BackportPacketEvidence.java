// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete semantic/proof evidence manifest for one immutable backport packet. */
public record M3BackportPacketEvidence(
        String packetId,
        List<M3RecipeAtomEvidence> atoms) {

    public M3BackportPacketEvidence {
        packetId = packetId(packetId);
        atoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
        if (atoms.isEmpty() || atoms.size() > 256) {
            throw new IllegalArgumentException("packet evidence atom budget");
        }
        LinkedHashMap<String, M3RecipeAtomEvidence> indexed = new LinkedHashMap<>();
        for (M3RecipeAtomEvidence atom : atoms) {
            M3RecipeAtomEvidence checked = Objects.requireNonNull(atom, "atom evidence");
            if (indexed.putIfAbsent(checked.atomId(), checked) != null) {
                throw new IllegalArgumentException("duplicate atom evidence: " + checked.atomId());
            }
        }
    }

    /**
     * Requires one and only one evidence row for every recipe atom and no rows for unknown atoms.
     */
    public void requireComplete(M3BackportPacket packet) {
        M3BackportPacket checked = Objects.requireNonNull(packet, "packet");
        if (!packetId.equals(checked.packetId())) {
            throw new IllegalArgumentException(
                    "packet/evidence id mismatch: " + checked.packetId() + " != " + packetId);
        }
        Map<String, M3RecipeAtomEvidence> indexed = byAtomId();
        for (M3RecipeAtom atom : checked.atoms()) {
            if (!indexed.containsKey(atom.id())) {
                throw new IllegalArgumentException("missing atom evidence: " + atom.id());
            }
        }
        for (M3RecipeAtomEvidence evidence : atoms) {
            checked.require(evidence.atomId());
        }
    }

    public M3RecipeAtomEvidence require(String atomId) {
        M3RecipeAtomEvidence evidence = byAtomId().get(Objects.requireNonNull(atomId, "atomId"));
        if (evidence == null) {
            throw new IllegalArgumentException("unknown atom evidence: " + atomId);
        }
        return evidence;
    }

    public Map<String, M3RecipeAtomEvidence> byAtomId() {
        LinkedHashMap<String, M3RecipeAtomEvidence> result = new LinkedHashMap<>();
        atoms.forEach(atom -> result.put(atom.atomId(), atom));
        return Map.copyOf(result);
    }

    private static String packetId(String value) {
        String checked = Objects.requireNonNull(value, "packetId").strip();
        if (!checked.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid packet id: " + checked);
        }
        return checked;
    }
}
