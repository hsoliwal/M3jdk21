// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.openrewrite.Recipe;

/** Exact scope receiver test repair without widening Synexia canonical registry API. */
public final class M3ScopeRegistryApiRepairRecipe extends Recipe {
    private static final String TARGET =
            "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/scope/M3ScopeCoverageClosureTest.java";
    private static final String ROOT =
            "/com/m3/rewrite/backport/scope-registry-api-repair/";

    @Override public String getDisplayName() { return "Repair M3 scope registry test API"; }
    @Override public String getDescription() {
        return "Replaces obsolete canonical registry-map access with public registered/require "
                + "checks on exact hash-sealed source; retains the target scope and failure tests.";
    }
    @Override public List<Recipe> getRecipeList() {
        return List.of(new M3VerbatimJavaPairRecipe(
                TARGET, "M3jdk21-master-20261013", "M3jdk21-scope-public-api-20261013",
                "M3_SCOPE_RECEIVER_PUBLIC_API_V1", resource("before.java.txt"), resource("after.java.txt")));
    }
    @Override public int maxCycles() { return 1; }
    public boolean promotionAuthority() { return false; }
    private static String resource(String path) {
        try (var input = M3ScopeRegistryApiRepairRecipe.class.getResourceAsStream(ROOT + path)) {
            if (input == null) throw new IllegalStateException("Missing scope repair resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read scope repair source", e);
        }
    }
}
