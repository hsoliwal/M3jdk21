// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Applies the only reviewed text delta from the final upstream Nebula sync.
 *
 * <p>The upstream CSS/API changes are already represented by stronger recipe-owned
 * postimages on this fork. This recipe therefore owns only the missing .polyglot*
 * ignore entry while the merge commit records upstream ancestry.</p>
 */
public final class NebulaM3UpstreamSyncIgnoreRecipe
        extends NebulaM3ExactTextSnapshotRecipe {

    static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/upstream-sync-20261006/gitignore.after.txt";

    @Override
    protected String repositoryPath() {
        return ".gitignore";
    }

    @Override
    protected String moduleRelativePath() {
        return "../../.gitignore";
    }

    @Override
    protected String beforeSha256() {
        return "596a571cc4da0d7023f002138e4ea1537a120a117d1267786020fa1c1e18c50b";
    }

    @Override
    protected String afterSha256() {
        return "818ec5cb08b6173dc1489d6364c6e9b5384c4649eaee57f2746a5f375ce820f7";
    }

    @Override
    protected String afterResource() {
        return RESOURCE;
    }

    @Override
    public String getDisplayName() {
        return "Admit final Nebula upstream-sync ignore leaf";
    }

    @Override
    public String getDescription() {
        return "Adds only the reviewed .polyglot* ignore entry after upstream CSS/API "
                + "changes have been independently subsumed by the fork's qualified recipes.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("nebula", "m3", "upstream-sync", "exact-text", "file-local");
    }

    public boolean sourceMutationAuthority() {
        return true;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
