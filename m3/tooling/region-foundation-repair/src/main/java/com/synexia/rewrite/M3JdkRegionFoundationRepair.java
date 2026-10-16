// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

/** Named task entry; the baseline is read-only and every input is independently hash-sealed. */
public final class M3JdkRegionFoundationRepair extends Recipe {
    @Option(displayName = "Sealed baseline root", description = "Separate checkout of the exact M3JDK baseline.", example = "/work/m3jdk-baseline")
    private final String baselineRoot;

    @JsonCreator
    public M3JdkRegionFoundationRepair(@JsonProperty("baselineRoot") String baselineRoot) {
        this.baselineRoot = Objects.requireNonNull(baselineRoot, "baselineRoot");
    }

    public String getBaselineRoot() { return baselineRoot; }
    @Override public String getDisplayName() { return "Repair M3JDK region and foundation contracts"; }
    @Override public String getDescription() { return "Compose exact existing Java and text recipe owners over a sealed baseline."; }

    @Override public List<Recipe> getRecipeList() {
        try {
            var pairs = RegionFoundationInputs.candidates(Path.of(baselineRoot));
            return List.of(RegionFoundationReplay.javaRecipe(pairs.getFirst()), RegionFoundationReplay.textRecipe());
        } catch (IOException failure) {
            throw new IllegalStateException("sealed baseline unavailable", failure);
        }
    }
}
