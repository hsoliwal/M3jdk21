// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * FILE-local convergence of SvgLoader absolute-unit DPI lookup.
 *
 * <p>The reviewed postimage extracts one private horizontal-DPI adapter and reuses it for cm/in/mm.
 * The class/package/public surface and all unsupported-unit behavior remain unchanged.</p>
 */
public final class NebulaM3SvgLoaderLengthConvergenceRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String REPOSITORY_PATH =
            "widgets/cwt/org.eclipse.nebula.cwt/src/org/eclipse/nebula/cwt/svg/SvgLoader.java";
    public static final String MODULE_PATH =
            "src/org/eclipse/nebula/cwt/svg/SvgLoader.java";
    public static final String BEFORE =
            "23d730f01826b3d5c3de5d9f5299f2db11574ae3c482956b20928aaa676f9a08";
    public static final String AFTER =
            "d56e584eb16981496046c7dbb67be8a18ff68aa01953084a441329bacc2c78eb";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/svg-loader-length/SvgLoader.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 converge Nebula SvgLoader absolute length DPI atom";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed contract-preserving SvgLoader FILE postimage that extracts "
                + "one private horizontal-DPI adapter and preserves length-unit behavior.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "svg",
                "atomization",
                "patternization",
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
