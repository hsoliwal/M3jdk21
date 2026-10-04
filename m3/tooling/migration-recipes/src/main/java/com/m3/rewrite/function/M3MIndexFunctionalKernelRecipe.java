// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.function;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Installs the reviewed JDK-internal MIndex functional substrate as one MODULE join over
 * hash-pinned FILE atoms.
 *
 * <p>No public {@code java.util.function} or {@code java.util.stream} source is mutated.</p>
 */
public final class M3MIndexFunctionalKernelRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Install JDK-internal MIndex functional kernel";
    }

    @Override
    public String getDescription() {
        return "Adds reviewed primitive functional plans, predicates, fused pipelines and "
                + "range/iterate/scan/fold support without changing Java SE public API.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "jdk21", "java.base", "mindex", "functional",
                "recipe-first", "hash-pinned", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe("mindex-functional-kernel"));
    }
}
