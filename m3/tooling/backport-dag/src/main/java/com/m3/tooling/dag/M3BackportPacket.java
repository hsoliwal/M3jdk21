// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import com.m3.rewrite.scope.M3EditScope;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable recipe-atom packet composed into the canonical backport lifecycle DAG. */
public record M3BackportPacket(String packetId, List<M3RecipeAtom> atoms) {
    public M3BackportPacket {
        packetId = packetId(packetId);
        atoms = List.copyOf(Objects.requireNonNull(atoms, "atoms"));
        if (atoms.isEmpty() || atoms.size() > 256) {
            throw new IllegalArgumentException("packet atom budget");
        }

        Map<String, M3RecipeAtom> indexed = new LinkedHashMap<>();
        for (M3RecipeAtom atom : atoms) {
            M3RecipeAtom checked = Objects.requireNonNull(atom, "atom");
            if (indexed.putIfAbsent(checked.id(), checked) != null) {
                throw new IllegalArgumentException("duplicate packet atom: " + checked.id());
            }
        }
        for (M3RecipeAtom atom : atoms) {
            for (String dependency : atom.dependsOn()) {
                if (!indexed.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            "missing packet atom dependency " + dependency + " for " + atom.id());
                }
            }
        }
    }

    public M3EditScope maximumScope() {
        M3EditScope scope = M3EditScope.FILE;
        for (M3RecipeAtom atom : atoms) {
            scope = scope.promote(atom.scope());
        }
        return scope;
    }

    /** Atoms with no packet-local dependents; these rejoin at canonical recipe-junit. */
    public List<M3RecipeAtom> terminalAtoms() {
        Set<String> dependencies = new HashSet<>();
        atoms.forEach(atom -> dependencies.addAll(atom.dependsOn()));
        return atoms.stream().filter(atom -> !dependencies.contains(atom.id())).toList();
    }

    public M3RecipeAtom require(String atomId) {
        String checked = Objects.requireNonNull(atomId, "atomId");
        return atoms.stream()
                .filter(atom -> atom.id().equals(checked))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown packet atom: " + checked));
    }

    private static String packetId(String value) {
        String checked = Objects.requireNonNull(value, "packetId").strip();
        if (!checked.matches("[a-z0-9][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid packet id: " + checked);
        }
        return checked;
    }
}
