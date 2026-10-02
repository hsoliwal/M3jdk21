// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;

/**
 * Executes one behavior-and-contract-preserving FILE recipe against exactly one source file until a
 * textual fixed point is reached.
 *
 * <p>The executor is the mechanical fence that makes repository-wide FILE work embarrassingly
 * parallel. A FILE worker cannot create, delete, rename, or mutate a second source because the
 * source set contains exactly one file and every returned change is checked against its original
 * path. Broader scopes use separate orchestrators.
 */
public final class M3FileRecipeExecutor {
    private M3FileRecipeExecutor() {}

    /** Apply a registered FILE recipe until fixed point. */
    public static Execution applyToFixedPoint(Recipe recipe, SourceFile source, int maxPasses) {
        Objects.requireNonNull(recipe, "recipe");
        return applyToFixedPoint(
                recipe,
                M3RecipeScopeRegistry.require(recipe.getClass()),
                source,
                maxPasses);
    }

    /**
     * Apply a recipe with an explicit policy. This overload exists for proof harnesses and future
     * registry loaders; production orchestration should normally use the registered overload.
     */
    public static Execution applyToFixedPoint(
            Recipe recipe,
            M3RecipeScopePolicy policy,
            SourceFile source,
            int maxPasses) {
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(source, "source");
        if (maxPasses < 1) {
            throw new IllegalArgumentException("maxPasses must be positive");
        }

        String originalPath = normalized(source.getSourcePath());
        if (!policy.fileLocalMechanical(List.of(originalPath))) {
            throw new IllegalArgumentException(
                    "recipe is not admitted for behavior-preserving FILE execution: "
                            + recipe.getClass().getName());
        }

        SourceFile current = source;
        int mutationPasses = 0;
        for (int pass = 0; pass < maxPasses; pass++) {
            var context = new InMemoryExecutionContext(error -> {
                throw new IllegalStateException("OpenRewrite FILE recipe failed", error);
            });
            var run = recipe.run(
                    new InMemoryLargeSourceSet(List.of(current)),
                    context,
                    1);
            List<Result> changes = run.getChangeset().getAllResults();
            if (changes.isEmpty()) {
                return new Execution(current, mutationPasses);
            }
            if (changes.size() != 1) {
                throw new IllegalStateException(
                        "FILE recipe produced " + changes.size() + " changed files");
            }

            Result change = changes.getFirst();
            SourceFile before = change.getBefore();
            SourceFile after = change.getAfter();
            if (before == null || after == null) {
                throw new IllegalStateException("FILE recipe may not create or delete source files");
            }

            String beforePath = normalized(before.getSourcePath());
            String afterPath = normalized(after.getSourcePath());
            if (!originalPath.equals(beforePath) || !originalPath.equals(afterPath)) {
                throw new IllegalStateException(
                        "FILE recipe may not rename or escape target: "
                                + beforePath + " -> " + afterPath);
            }

            if (current.printAll().equals(after.printAll())) {
                return new Execution(after, mutationPasses);
            }

            current = after;
            mutationPasses++;
        }

        throw new IllegalStateException(
                "FILE recipe did not reach fixed point within " + maxPasses + " passes");
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    /** Fixed-point result for one independently executable source file. */
    public record Execution(SourceFile source, int mutationPasses) {
        public Execution {
            Objects.requireNonNull(source, "source");
            if (mutationPasses < 0) {
                throw new IllegalArgumentException("mutationPasses must be non-negative");
            }
        }

        public boolean changed() {
            return mutationPasses > 0;
        }
    }
}
