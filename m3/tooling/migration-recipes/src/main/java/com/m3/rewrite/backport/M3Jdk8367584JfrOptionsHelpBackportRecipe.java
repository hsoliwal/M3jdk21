// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible startup-help leaf split from OpenJDK JDK-8367584 / JEP 536.
 *
 * <p>Upstream donor: {@code openjdk/jdk@39de79eae23410e335d2d1ced8fe3b4d7937a541}.
 * This packet imports only {@code -XX:FlightRecorderOptions:help}: startup help dispatch,
 * formatting of options already present in JDK 21, and the focused jtreg test. JEP 536 redaction
 * filters/events/options are deliberately excluded from this compatibility leaf.</p>
 *
 * <p>The composite is intentionally made from file-local replay atoms. Those leaves can execute in
 * parallel through the M3 backport DAG; the composite recipe is the MODULE-scope semantic join and
 * becomes a fixed point after the leaves have materialized.</p>
 */
public final class M3Jdk8367584JfrOptionsHelpBackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "39de79eae23410e335d2d1ced8fe3b4d7937a541";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8367584 FlightRecorderOptions help leaf";
    }

    @Override
    public String getDescription() {
        return "Composes four file-local hash-pinned replay atoms for the Java-21-compatible "
                + "FlightRecorderOptions:help leaf while excluding the JEP 536 redaction runtime.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8367584",
                "jfr",
                "tooling",
                "help",
                "hash-pinned",
                "compatibility-split",
                "file-atoms",
                "dag-composable",
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
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-jfr-options-help-8367584-dcmd-cpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-jfr-options-help-8367584-dcmd-hpp"),
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "jdk27-jfr-options-help-8367584-option-set"),
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-jfr-options-help-8367584-java"));
    }
}
