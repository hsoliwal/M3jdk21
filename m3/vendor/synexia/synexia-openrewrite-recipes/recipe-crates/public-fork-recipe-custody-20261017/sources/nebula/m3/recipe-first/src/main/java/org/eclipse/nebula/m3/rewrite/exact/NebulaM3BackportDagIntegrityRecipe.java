// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Replays the reviewed fail-closed backport DAG validation atom from one exact Java 21 preimage.
 *
 * <p>OpenRewrite owns the transformation. The retained original and postimage are content-pinned;
 * a changed source, a non-Java target or an unrelated path receives no candidate replacement.
 * This recipe does not execute any backport or grant mutation/promotion authority downstream.</p>
 */
public final class NebulaM3BackportDagIntegrityRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {
    public static final String PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3BackportPlan.java";
    public static final String MODULE_PATH =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3BackportPlan.java";
    public static final String BEFORE_SHA256 =
            "ca88fb0e5ff160f3f24f149487f4c8de7c2cc675f67902565551e7e63c6aafb6";
    public static final String AFTER_SHA256 =
            "56f1b274579ada8ac7d7ae92dbd4717c3cef7aaff1e2231527fe54691699d787";
    public static final String POSTIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/backport-dag-integrity/"
                    + "NebulaM3BackportPlan.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 fail-closed backport dependency DAG atom";
    }

    @Override
    public String getDescription() {
        return "Install the exact reviewed Java 21 validator for the Nebula-to-Synexia-to-M3JDK21 "
                + "backport plan, refusing changed preimages and preserving a fixed-point postimage.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "nebula", "backport", "jdk21", "recipe-first",
                "file-local", "contract-preserving", "candidate-only");
    }

    @Override
    protected String repositoryPath() {
        return PATH;
    }

    @Override
    protected String moduleRelativePath() {
        return MODULE_PATH;
    }

    @Override
    protected String beforeSha256() {
        return BEFORE_SHA256;
    }

    @Override
    protected String afterSha256() {
        return AFTER_SHA256;
    }

    @Override
    protected String afterResource() {
        return POSTIMAGE;
    }
}
