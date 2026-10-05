// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import org.openrewrite.config.Environment;
import org.openrewrite.Recipe;

/** Canonical activation boundary for the distributed declarative A3 Java-21 convergence DAG. */
public final class M3Java21ConvergenceCatalog {
    public static final String RECIPE = "com.m3.rewrite.M3Java21Convergence";

    private static final Environment ENVIRONMENT =
            Environment.builder().scanRuntimeClasspath().build();

    private M3Java21ConvergenceCatalog() {
        throw new AssertionError("No instances");
    }

    /**
     * Activates the named recipe through the same managed Environment model used by OpenRewrite
     * integrations. The Environment is scanned once; each serial FILE pass activates the named DAG
     * explicitly, so a recipe is never applied merely because it exists on the classpath.
     */
    public static Recipe activate() {
        Recipe recipe = ENVIRONMENT.activateRecipes(RECIPE);
        if (recipe == null) {
            throw new IllegalStateException("missing declarative A3 convergence recipe: " + RECIPE);
        }
        return recipe;
    }
}
