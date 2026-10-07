// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport of upstream JDK-8364182.
 *
 * <p>The upstream donor is openjdk/jdk commit
 * {@code f2f8828188f45d16344c82adfbf951f7409b8825}. The backport adds the
 * {@code VM.security_properties} diagnostic command while preserving the existing Java 21
 * {@code VM.system_properties} command and all Java source/class-file semantics.
 *
 * <p>Structured Java and HotSpot text/native sources are separate hash-pinned atoms so Java remains
 * an OpenRewrite Java LST and C++/header sources retain exact UTF-8 pre/post images.
 */
public final class M3Jdk8364182BackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "f2f8828188f45d16344c82adfbf951f7409b8825";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8364182 VM.security_properties";
    }

    @Override
    public String getDescription() {
        return "Replays the additive Java-21-compatible jcmd security-properties command from "
                + "exact JDK21 preimages while preserving VM.system_properties.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8364182",
                "jcmd",
                "serviceability",
                "security-properties",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-security-properties-8364182-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-security-properties-8364182-text"));
    }
}
