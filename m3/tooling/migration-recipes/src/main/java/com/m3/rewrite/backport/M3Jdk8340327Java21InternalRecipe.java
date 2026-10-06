// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.util.List;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible internal adaptation of JDK-8340327.
 *
 * <p>The upstream named-key/KEM/signature framework is retained, but the post-21 public
 * {@code java.security.AsymmetricKey} dependency is not imported. Product source remains
 * materialization-gated by the hash-pinned crate.</p>
 */
public final class M3Jdk8340327Java21InternalRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Adapt JDK-8340327 named-key framework to Java 21";
    }

    @Override
    public String getDescription() {
        return "Replays the pinned JDK-8340327 internal security framework with Java-21-compatible "
                + "named-key parameter access and no post-21 public AsymmetricKey API.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new M3HashPinnedJavaSnapshotRecipe("jdk-8340327-java21-internal"));
    }
}
