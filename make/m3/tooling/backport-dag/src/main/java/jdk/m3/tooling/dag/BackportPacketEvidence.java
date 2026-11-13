// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Complete semantic/proof evidence manifest for one immutable backport packet. */
public record BackportPacketEvidence(
        String packetId,
        List<RecipeAtomEvidence> atoms) {

    public BackportPacketEvidence {
        packetId = packetId(packetId);
        atoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
        if (atoms.isEmpty() || atoms.size() > 256) {
            throw new IllegalArgumentException("packet evidence atom budget");
        }
        LinkedHashMap<String, RecipeAtomEvidence> indexed = new LinkedHashMap<>();
        for (RecipeAtomEvidence atom : atoms) {
            RecipeAtomEvidence checked = Objects.requireNonNull(atom, "atom evidence");
            if (indexed.putIfAbsent(checked.atomId(), checked) != null) {
                throw new IllegalArgumentException("duplicate atom evidence: " + checked.atomId());
            }
        }
    }

    /**
     * Requires one and only one evidence row for every recipe atom and no rows for unknown atoms.
     */
    public void requireComplete(BackportPacket packet) {
        BackportPacket checked = Objects.requireNonNull(packet, "packet");
        if (!packetId.equals(checked.packetId())) {
            throw new IllegalArgumentException(
                    "packet/evidence id mismatch: " + checked.packetId() + " != " + packetId);
        }
        Map<String, RecipeAtomEvidence> indexed = byAtomId();
        for (RecipeAtom atom : checked.atoms()) {
            if (!indexed.containsKey(atom.id())) {
                throw new IllegalArgumentException("missing atom evidence: " + atom.id());
            }
        }
        for (RecipeAtomEvidence evidence : atoms) {
            checked.require(evidence.atomId());
        }
    }

    public RecipeAtomEvidence require(String atomId) {
        RecipeAtomEvidence evidence = byAtomId().get(Objects.requireNonNull(atomId, "atomId"));
        if (evidence == null) {
            throw new IllegalArgumentException("unknown atom evidence: " + atomId);
        }
        return evidence;
    }

    public Map<String, RecipeAtomEvidence> byAtomId() {
        LinkedHashMap<String, RecipeAtomEvidence> result = new LinkedHashMap<>();
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
