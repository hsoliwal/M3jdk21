// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.convergence;

import com.m3.rewrite.M3Java21ConvergenceRecipe;
import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import com.m3.rewrite.scope.M3ContractMode;
import com.m3.rewrite.scope.M3EditScope;
import com.m3.rewrite.scope.M3RecipeScopeRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Recipe;

/**
 * Canonical registry/DAG of independently proven FILE convergence recipe atoms.
 *
 * <p>The executor depends on this registry rather than hard-coding transformation classes. A new
 * atomization, patternization/IOP or documentation family joins whole-JDK convergence only by adding
 * one registry entry after its own JUnit proof and FILE/contract admission exist.
 */
public final class M3FileConvergenceRecipeDag {
    public enum Phase {
        INVENTORY(false),
        ATOMIZATION(true),
        PATTERNIZATION(true),
        DOCUMENTATION(true);

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
            id = token(id, "id");
            phase = Objects.requireNonNull(phase, "phase");
            recipe = Objects.requireNonNull(recipe, "recipe");
            var policy = M3RecipeScopeRegistry.require(recipe.getClass());
            if (policy.minimumScope() != M3EditScope.FILE
                    || policy.contractMode()
                            != M3ContractMode.BEHAVIOR_AND_CONTRACT_PRESERVING) {
                throw new IllegalArgumentException(
                        "convergence atom is not FILE contract-preserving: " + recipe.getName());
            }
        }

        private static String token(String value, String field) {
            String checked = Objects.requireNonNull(value, field).strip();
            if (checked.isEmpty() || checked.indexOf('\0') >= 0) {
                throw new IllegalArgumentException(field);
            }
            return checked;
        }
    }

    private static final List<Atom> ATOMS =
            validate(
                    List.of(
                            new Atom(
                                    "pure-int-inventory",
                                    Phase.INVENTORY,
                                    new M3InventoryPureIntAtomCandidates()),
                            new Atom(
                                    "pure-int-atomization",
                                    Phase.ATOMIZATION,
                                    new M3AtomizePureIntReturnRecipe()),
                            new Atom(
                                    "pure-int-patternization",
                                    Phase.PATTERNIZATION,
                                    new M3PatternizePureIntAtomRecipe()),
                            new Atom(
                                    "pure-int-documentation",
                                    Phase.DOCUMENTATION,
                                    new M3DocumentPureIntAtomRecipe())));

    private M3FileConvergenceRecipeDag() {}

    public static List<Atom> atoms() {
        return ATOMS;
    }

    public static List<Atom> phase(Phase phase) {
        Phase checked = Objects.requireNonNull(phase, "phase");
        return ATOMS.stream().filter(atom -> atom.phase() == checked).toList();
    }

    public static Recipe fixedPointRecipe() {
        return new M3Java21ConvergenceRecipe();
    }

    private static List<Atom> validate(List<Atom> input) {
        List<Atom> atoms = List.copyOf(Objects.requireNonNull(input, "input"));
        if (atoms.isEmpty()) throw new IllegalArgumentException("empty convergence DAG");

        HashSet<String> ids = new HashSet<>();
        int prior = -1;
        ArrayList<Atom> checked = new ArrayList<>(atoms.size());
        for (Atom atom : atoms) {
            Atom value = Objects.requireNonNull(atom, "atom");
            if (!ids.add(value.id())) {
                throw new IllegalArgumentException("duplicate convergence atom: " + value.id());
            }
            if (value.phase().ordinal() < prior) {
                throw new IllegalArgumentException("convergence phase order regression");
            }
            prior = value.phase().ordinal();
            checked.add(value);
        }
        return List.copyOf(checked);
    }
}
