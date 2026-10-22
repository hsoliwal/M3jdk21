// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite;

import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

/** Canonical managed-Environment activation boundary for Nebula FILE-local convergence. */
public final class NebulaM3ConvergenceCatalog {
    public static final String RECIPE = "org.eclipse.nebula.m3.ConvergeFileAtoms";

    private static final Environment ENVIRONMENT =
            Environment.builder().scanRuntimeClasspath().build();

    private NebulaM3ConvergenceCatalog() {
        throw new AssertionError("No instances");
    }

    /** Explicitly activates the distributed declarative recipe; classpath presence alone is inert. */
    public static Recipe activate() {
        Recipe recipe = ENVIRONMENT.activateRecipes(RECIPE);
        if (recipe == null) {
            throw new IllegalStateException("missing Nebula declarative convergence recipe: " + RECIPE);
        }
        return recipe;
    }
}
