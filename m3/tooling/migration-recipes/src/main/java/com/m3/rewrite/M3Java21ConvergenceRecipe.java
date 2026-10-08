// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import com.synexia.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.synexia.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.synexia.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.synexia.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;
import org.openrewrite.java.RemoveUnusedImports;

/**
 * Top-level Java 21 FILE-local M3 convergence recipe.
 *
 * <p>This is the single trusted OpenRewrite entry point for mechanically proven Java 21
 * atomization/patternization/documentation passes. New semantic families are added here only after
 * their bounded JUnit proof is green. OpenRewrite cycles the ordered child recipes until source
 * reaches a fixed point.
 *
 * <p>The final import pass reuses the pinned OpenRewrite catalogue recipe. Its missing-type
 * guard remains upstream-owned: incomplete attribution must not become guessed import usage.
 * Import cleanup follows typed atom/pattern/documentation edits and does not replace them.
 */
public final class M3Java21ConvergenceRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "M3 Java 21 multi-pass FILE convergence";
    }

    @Override
    public String getDescription() {
        return "Inventories, atomizes, patternizes/IOP-types and documents admitted Java 21 "
                + "FILE-local leaves through one deterministic OpenRewrite DAG.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "java21",
                "multi-pass",
                "convergence",
                "inventory",
                "atomization",
                "patternization",
                "iop",
                "documentation",
                "file-local",
                "behavior-contract-preserving");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3InventoryPureIntAtomCandidates(),
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe(),
                new RemoveUnusedImports());
    }
}
