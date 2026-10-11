// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Canonical Synexia donor-convergence workspace program.
 *
 * <p>This read-only composite is the repository-level invariant entry point. It first requires
 * complete source-specific FILE -> PACKAGE -> MODULE -> PROJECT -> REPOSITORY atom/pattern
 * coverage for every Maven owner, inventories the Apache-only downstream delivery registry, and
 * projects the canonical all-target registry/handoff policy without applying target mutations.
 * Source-changing work is deliberately excluded and remains owned by
 * {@link M3EveryModuleRecipeFirstTaskRecipe} plus a separately sealed task recipe crate.</p>
 */
public final class M3DonorConvergenceWorkspaceProgramRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "M3 Synexia donor-convergence workspace program";
    }

    @Override
    public String getDescription() {
        return "Runs strict every-module atomization/patternization with source-specific routing, "
                + "inventories Apache-only delivery prefixes, and projects every resolved receiving "
                + "repository through the canonical handoff policy without granting source mutation, "
                + "donor-copy, delivery, merge, or promotion authority.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "synexia",
                "m3",
                "donor-convergence-workspace",
                "canonical-owner",
                "every-project",
                "every-module",
                "file-package-module-project-repository",
                "source-specific",
                "documentation-specific",
                "apache-2.0",
                "delivery-target",
                "recipe-first",
                "read-only");
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3EveryModuleAtomPatternApplicationRecipe(),
                new M3DeliveryTargetExportPlanRecipe(),
                new M3AllTargetConvergencePlanRecipe());
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public boolean causesAnotherCycle() {
        return false;
    }

    public boolean canonicalDonorConvergenceOwner() {
        return true;
    }

    public boolean requiresFiveScopeCoverage() {
        return true;
    }

    public boolean automaticDeliveryApacheOnly() {
        return true;
    }

    public boolean coversAllResolvedTargets() {
        return true;
    }

    public String sourceChangingTaskOwner() {
        return M3EveryModuleRecipeFirstTaskRecipe.class.getName();
    }

    public boolean mutationAuthority() {
        return false;
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean deliveryAuthority() {
        return false;
    }

    public boolean mergeAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
