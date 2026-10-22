// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import org.eclipse.nebula.m3.rewrite.NebulaM3ConvergenceCatalog;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3AtomizePureIntReturnRecipe;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3DocumentPureIntAtomRecipe;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3InventoryPureIntAtomCandidates;
import org.eclipse.nebula.m3.rewrite.atom.NebulaM3PatternizePureIntAtomRecipe;
import org.eclipse.nebula.m3.rewrite.exact.NebulaM3SvgLoaderLengthConvergenceRecipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Recipe;

/** Canonical Nebula FILE-local convergence DAG. */
public final class NebulaM3FileConvergenceRecipeDag {
    public enum Phase {
        INVENTORY(false),
        ATOMIZATION(true),
        PATTERNIZATION(true),
        DOCUMENTATION(true),
        PROVEN_FILE_CONVERGENCE(true);

        private final boolean mutating;

        Phase(boolean mutating) {
            this.mutating = mutating;
        }

        public boolean mutating() {
            return mutating;
        }
    }

    public record Atom(String id, Phase phase, Recipe recipe) {
        public Atom {
            id = Objects.requireNonNull(id, "id").strip();
            phase = Objects.requireNonNull(phase, "phase");
            recipe = Objects.requireNonNull(recipe, "recipe");
            if (id.isEmpty()
                    || !recipe.getTags().contains("file-local")
                    || !recipe.getTags().contains("behavior-contract-preserving")) {
                throw new IllegalArgumentException("Nebula M3 atom is not FILE contract-preserving");
            }
        }
    }

    private static final List<Atom> ATOMS =
            validate(
                    List.of(
                            new Atom(
                                    "pure-int-inventory",
                                    Phase.INVENTORY,
                                    new NebulaM3InventoryPureIntAtomCandidates()),
                            new Atom(
                                    "pure-int-atomization",
                                    Phase.ATOMIZATION,
                                    new NebulaM3AtomizePureIntReturnRecipe()),
                            new Atom(
                                    "pure-int-patternization",
                                    Phase.PATTERNIZATION,
                                    new NebulaM3PatternizePureIntAtomRecipe()),
                            new Atom(
                                    "pure-int-documentation",
                                    Phase.DOCUMENTATION,
                                    new NebulaM3DocumentPureIntAtomRecipe()),
                            new Atom(
                                    "svgloader-length-convergence",
                                    Phase.PROVEN_FILE_CONVERGENCE,
                                    new NebulaM3SvgLoaderLengthConvergenceRecipe())));

    private NebulaM3FileConvergenceRecipeDag() {}

    public static List<Atom> atoms() {
        return ATOMS;
    }

    public static Recipe fixedPointRecipe() {
        return NebulaM3ConvergenceCatalog.activate();
    }

    private static List<Atom> validate(List<Atom> input) {
        List<Atom> atoms = List.copyOf(Objects.requireNonNull(input, "input"));
        HashSet<String> ids = new HashSet<>();
        int prior = -1;
        ArrayList<Atom> checked = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            if (!ids.add(atom.id())) {
                throw new IllegalArgumentException("duplicate convergence atom: " + atom.id());
            }
            if (atom.phase().ordinal() < prior) {
                throw new IllegalArgumentException("convergence phase order regression");
            }
            prior = atom.phase().ordinal();
            checked.add(atom);
        }
        return List.copyOf(checked);
    }
}
