// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One file-local, exact-source candidate for the existing recipe-first M3 executor.
 *
 * <p>The postimage is checked when this recipe is constructed. A divergent source is held for
 * explicit rebase instead of receiving a guessed patch. A postimage is an unchanged fixed point.
 * This producer does not promote source: {@link M3RecipeFirstExecution} binds the source and
 * Maven recipe, {@link SealedRecipeEngine} executes the ordered behavior gates, and
 * {@link SealedHierarchyReview} verifies file/package/module/project fan-in.</p>
 *
 * <p>The Java declaration projection is deliberately conservative. It preserves the original
 * package, imports, types, fields and non-private method signatures in their original order;
 * additive declarations may be inserted. Passing it does not prove semantic equivalence, which
 * still requires the
 * independently supplied {@link SealedVerification.Oracle}.</p>
 */
public final class M3ExactSourceAtom implements SealedRecipe {
    private static final String CATEGORY = "M3_EXACT_SOURCE_ATOM";

    private final Identity identity;
    private final String path;
    private final String beforeSha256;
    private final String afterSha256;
    private final String postimage;

    /**
     * Binds the implementation closure and exact source transition to the registered recipe ID.
     * Both SHA-256 values refer to the complete UTF-8 file, including the final newline.
     */
    public M3ExactSourceAtom(String recipeId, String version, String implementationSha256,
            String sourcePath, String beforeSha256, String afterSha256, String postimage) {
        this.path = SealedSources.path(sourcePath);
        if (!path.endsWith(".java")) throw new IllegalArgumentException("SOURCE_ATOM_JAVA_PATH");
        this.beforeSha256 = SealHash.require(beforeSha256);
        this.afterSha256 = SealHash.require(afterSha256);
        this.postimage = Objects.requireNonNull(postimage, "postimage");
        if (!this.afterSha256.equals(SealHash.text(postimage))) {
            throw new IllegalArgumentException("SOURCE_ATOM_POSTIMAGE_DRIFT");
        }
        if (this.beforeSha256.equals(this.afterSha256)) {
            throw new IllegalArgumentException("SOURCE_ATOM_EMPTY_TRANSITION");
        }
        String closure = SealHash.frame(
                SealHash.require(implementationSha256), path, this.beforeSha256, this.afterSha256);
        identity = new Identity(recipeId, version, closure, CATEGORY);
    }

    @Override public Identity identity() { return identity; }
    public String sourcePath() { return path; }
    public String preimageSha256() { return beforeSha256; }
    public String postimageSha256() { return afterSha256; }

    @Override
    public Output propose(SealedSources sources, Set<String> writablePaths,
            SealedContract contract) {
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(contract, "contract");
        if (!Set.of(path).equals(Objects.requireNonNull(writablePaths, "writablePaths"))) {
            throw new IllegalArgumentException("SOURCE_ATOM_FILE_OWNERSHIP");
        }
        String current = sources.require(path);
        String digest = SealHash.text(current);
        if (afterSha256.equals(digest)) return Output.unchanged();
        if (!beforeSha256.equals(digest)) return Output.hold("SOURCE_ATOM_PREIMAGE_DRIFT");
        if (!preservesExistingDeclarations(current, postimage)) {
            return Output.hold("SOURCE_ATOM_DECLARATION_DRIFT");
        }
        return new Output(Map.of(path, postimage), "");
    }

    private static boolean preservesExistingDeclarations(String before, String after) {
        List<String> existing = JavaContractSurface.canonical(before).lines().toList();
        List<String> candidate = JavaContractSurface.canonical(after).lines().toList();
        if (existing.size() < 2 || candidate.size() < 2
                || !existing.get(0).equals(candidate.get(0))
                || !existing.get(1).equals(candidate.get(1))) return false;
        int offset = 2;
        for (int index = 2; index < existing.size(); index++) {
            while (offset < candidate.size() && !existing.get(index).equals(candidate.get(offset))) {
                offset++;
            }
            if (offset == candidate.size()) return false;
            offset++;
        }
        return true;
    }
}
