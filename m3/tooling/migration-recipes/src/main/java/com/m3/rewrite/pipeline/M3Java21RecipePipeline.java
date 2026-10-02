// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pipeline;

import com.m3.rewrite.atom.M3AtomizePureIntReturnRecipe;
import com.m3.rewrite.atom.M3DocumentPureIntAtomRecipe;
import com.m3.rewrite.atom.M3InventoryPureIntAtomCandidates;
import com.m3.rewrite.atom.M3PatternizePureIntAtomRecipe;
import java.util.List;
import org.openrewrite.Recipe;

/**
 * Canonical registry and constructor for the trusted Java 21 FILE-local OpenRewrite DAG.
 *
 * <p>Only recipes with bounded JUnit proof and an explicit M3 scope/contract policy may be added
 * here. The registry is deliberately boring: deterministic order, no discovery by classpath scan,
 * no reflection-based mutation and no implicit scope promotion.
 */
public final class M3Java21RecipePipeline {
    private static final List<M3RecipeRegistration> REGISTRATIONS = List.of(
            new M3RecipeRegistration(
                    "m3.java21.inventory.pure-int",
                    M3RecipeStage.INVENTORY,
                    0,
                    M3InventoryPureIntAtomCandidates.class),
            new M3RecipeRegistration(
                    "m3.java21.atomize.pure-int",
                    M3RecipeStage.ATOMIZATION,
                    10,
                    M3AtomizePureIntReturnRecipe.class),
            new M3RecipeRegistration(
                    "m3.java21.patternize-iop.pure-int",
                    M3RecipeStage.PATTERNIZATION_IOP,
                    20,
                    M3PatternizePureIntAtomRecipe.class),
            new M3RecipeRegistration(
                    "m3.java21.document.pure-int",
                    M3RecipeStage.DOCUMENTATION,
                    30,
                    M3DocumentPureIntAtomRecipe.class));

    private M3Java21RecipePipeline() {}

    public static List<M3RecipeRegistration> registrations() {
        return REGISTRATIONS;
    }

    public static List<Recipe> instantiate() {
        return List.of(
                new M3InventoryPureIntAtomCandidates(),
                new M3AtomizePureIntReturnRecipe(),
                new M3PatternizePureIntAtomRecipe(),
                new M3DocumentPureIntAtomRecipe());
    }
}
