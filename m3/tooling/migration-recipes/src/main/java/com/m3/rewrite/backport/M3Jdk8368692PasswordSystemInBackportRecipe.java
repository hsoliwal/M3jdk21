// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible backport of OpenJDK JDK-8368692.
 *
 * <p>Upstream donor: {@code openjdk/jdk@9131c72d63cac7d2a0e845952cee0e3c7edbfc93}.
 * M3JDK21 preserves the JDK 21 Security Manager-era access path by resolving the new property
 * through {@code SecurityProperties.privilegedGetOverridable}. The default remains {@code true},
 * so existing JDK 21 behavior is unchanged unless the property is explicitly disabled.</p>
 */
public final class M3Jdk8368692PasswordSystemInBackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "9131c72d63cac7d2a0e845952cee0e3c7edbfc93";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8368692 password System.in policy";
    }

    @Override
    public String getDescription() {
        return "Replays the Java-21-compatible jdk.security.password.allowSystemIn policy from "
                + "exact JDK21 preimages while retaining privileged security-property access.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8368692",
                "security",
                "password",
                "system-in",
                "hash-pinned",
                "compatibility-split",
                "module-scope",
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
                        "jdk27-password-systemin-8368692-java"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-password-systemin-8368692-text"));
    }
}
