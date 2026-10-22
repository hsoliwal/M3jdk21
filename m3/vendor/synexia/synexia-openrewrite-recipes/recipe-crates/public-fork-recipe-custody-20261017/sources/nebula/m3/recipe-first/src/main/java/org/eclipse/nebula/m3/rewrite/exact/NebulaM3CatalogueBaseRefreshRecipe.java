// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Refreshes only the reviewed base revision of the explicit Nebula catalogue application plan.
 *
 * <p>The selected recipe list, allowed path namespace and schema remain byte-for-byte identical.
 * This recipe is custody plumbing only; it does not itself execute catalogue recipes or grant
 * source/promotion authority.</p>
 */
public final class NebulaM3CatalogueBaseRefreshRecipe
        extends NebulaM3ExactTextSnapshotRecipe {

    static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/catalogue-base-refresh/catalogue-plan.json.after";

    @Override
    protected String repositoryPath() {
        return "m3/convergence/catalogue-plan.json";
    }

    @Override
    protected String moduleRelativePath() {
        return "../convergence/catalogue-plan.json";
    }

    @Override
    protected String beforeSha256() {
        return "30acad6ead40be494080b459c385ac0a1a91052fdfcbb62388ce00605064dbf3";
    }

    @Override
    protected String afterSha256() {
        return "0a68126f234ad1427f4815d712151fd582420de2fbdffaae4ce59f533acd596e";
    }

    @Override
    protected String afterResource() {
        return RESOURCE;
    }

    @Override
    public String getDisplayName() {
        return "Refresh Nebula catalogue application reviewed base";
    }

    @Override
    public String getDescription() {
        return "Moves only the source-custody base revision to current reviewed master while "
                + "preserving the exact recipe order and widgets-only namespace.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "nebula",
                "m3",
                "recipe-first",
                "source-custody",
                "catalogue",
                "file-local",
                "candidate-only");
    }

    public boolean recipeExecutionAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
