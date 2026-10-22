// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact FILE-local lowering of three identical RoundedToolbar stream searches.
 *
 * <p>The reviewed postimage derives one private method name from the stream segments and unfolds
 * the filter into an ordinary callback method. Deferred event listeners and the public surface
 * remain unchanged. ArrayList tryAdvance preserves the post-predicate mutation check.
 * Names provide metadata only; exact pre/post hashes and executed differential proof bind this candidate.</p>
 */
public final class NebulaM3RoundedToolbarStreamRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String REPOSITORY_PATH =
            "widgets/opal/roundedtoolbar/org.eclipse.nebula.widgets.opal.roundedtoolbar/src/org/eclipse/nebula/widgets/opal/roundedtoolbar/RoundedToolbar.java";
    public static final String MODULE_PATH =
            "src/org/eclipse/nebula/widgets/opal/roundedtoolbar/RoundedToolbar.java";
    public static final String BEFORE =
            "fd00029ccb595dfedd0ec1c0604856359e065c9d2ca3fcbbe86382b15bbc0c59";
    public static final String AFTER =
            "c02686f975553876c50392f013759d95bf44b792b2b77bc7bba5cb9eddb10407";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/rounded-toolbar-stream/RoundedToolbar.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 unfold Nebula RoundedToolbar stream search atoms";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed source-bound RoundedToolbar postimage that shares "
                + "one segment-named search and retains event timing and traversal checks.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "stream-segments",
                "atomization",
                "ordinary-methods",
                "iop",
                "documentation",
                "adapter",
                "file-local",
                "behavior-contract-preserving",
                "exact-source");
    }

    @Override
    protected String repositoryPath() {
        return REPOSITORY_PATH;
    }

    @Override
    protected String moduleRelativePath() {
        return MODULE_PATH;
    }

    @Override
    protected String beforeSha256() {
        return BEFORE;
    }

    @Override
    protected String afterSha256() {
        return AFTER;
    }

    @Override
    protected String afterResource() {
        return RESOURCE;
    }
}
