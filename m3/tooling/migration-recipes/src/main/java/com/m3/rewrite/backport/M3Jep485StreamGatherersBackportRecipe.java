// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import java.util.Set;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/**
 * Opt-in Java 21 extension packet for final JEP 485, Stream Gatherers.
 *
 * <p>The reviewed donor state is OpenJDK JDK 24 GA ({@code jdk-24+36}). This recipe is intentionally
 * not a default Java 21 backport: it adds public java.base API and therefore requires explicit
 * LIBRARY_API contract-change authority. Stock M3JDK21 identity remains unchanged unless the caller
 * deliberately materializes this packet.</p>
 */
public final class M3Jep485StreamGatherersBackportRecipe extends Recipe {
    public static final String OPT_IN_TOKEN = "JEP485";
    public static final String DONOR_TAG = "jdk-24+36";
    public static final String IMPLEMENTATION_COMMIT =
            "33b26f79a986d015abdcd84b89842adc0a4bde64";
    public static final String GRADUATION_COMMIT =
            "ef0dc2518e7636cc8a9ca580613ff5edeb4c19fd";
    public static final String MAP_CONCURRENT_FIX =
            "450636ae28b84ded083b6861c6cba85fbf87e16e";

    @Option(
            displayName = "Explicit opt-in token",
            description = "Must be JEP485 to materialize the opt-in SE API extension.",
            example = "JEP485",
            required = false)
    private final String optInToken;

    public M3Jep485StreamGatherersBackportRecipe() {
        this(null);
    }

    @JsonCreator
    public M3Jep485StreamGatherersBackportRecipe(String optInToken) {
        this.optInToken = optInToken;
    }

    public boolean optedIn() {
        return OPT_IN_TOKEN.equals(optInToken);
    }

    @Override
    public String getDisplayName() {
        return "Materialize opt-in final JEP 485 Stream Gatherers";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed JDK24-GA Stream Gatherers API, stream-pipeline integration "
                + "and GA tests from exact Java 21 preimages as an explicit opt-in SE API extension.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jep-485",
                "stream-gatherers",
                "java-base",
                "hash-pinned",
                "library-api",
                "explicit-contract-change",
                "opt-in-se-api",
                "jdk24-ga",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        if (!optedIn()) {
            return List.of();
        }
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe("jdk24-jep485-stream-gatherers"));
    }
}
