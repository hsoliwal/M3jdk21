// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * FILE-local compatibility migration for CDateTimePropertyHandler against the
 * current Eclipse CSS value model.
 *
 * <p>OpenRewrite's stock {@code ChangeType} recipe is the canonical donor for the
 * mechanical Measure -> CSSPrimitiveValue type migration. The reviewed postimage
 * additionally preserves Nebula's historical font-shorthand classification because
 * the newer Eclipse CSS2FontHelper changed that observable behavior.</p>
 */
public final class NebulaM3CDateTimeCssCompatibilityRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String REPOSITORY_PATH =
            "widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/src/"
                    + "org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java";
    public static final String MODULE_PATH =
            "src/org/eclipse/nebula/widgets/cdatetime/css/CDateTimePropertyHandler.java";
    public static final String BEFORE =
            "8b0542080ffeab65e94dc0e05182866559718e5b2c7187bce4835e717f40556a";
    public static final String AFTER =
            "9b95b52df265443f996fc67e0316025bce167281d175478b0070edf56c54d87d";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/cdatetime-css/"
                    + "CDateTimePropertyHandler.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 migrate Nebula CDateTime CSS handler to current Eclipse CSS values";
    }

    @Override
    public String getDescription() {
        return "Replays the reviewed FILE-local compatibility postimage: remove the deleted "
                + "Measure dependency, preserve W3C CSSPrimitiveValue access, and retain the "
                + "historical Nebula font-shorthand classification contract.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "cdatetime",
                "css",
                "openrewrite",
                "change-type-donor",
                "compatibility",
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
