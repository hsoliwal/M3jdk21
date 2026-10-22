// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact FILE-local schema-assertion correction for the 2026-10-13 reproof ledger.
 *
 * <p>Only the JUnit test's expected TSV header spelling changes, matching the already committed
 * ledger. Widget/runtime APIs, ledger bytes, JDK contracts and product behavior are untouched.
 * This bounded Nebula proving fixture delegates rewriting to the existing exact Java LST owner;
 * reusable cross-repository M3 recipe authority remains in Synexia.</p>
 */
public final class NebulaM3CurrentMasterReproofHeaderRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {

    public static final String REPOSITORY_PATH =
            "m3/recipe-first/src/test/java/org/eclipse/nebula/m3/"
                    + "NebulaM3CurrentMasterReproofTest.java";
    public static final String MODULE_PATH =
            "src/test/java/org/eclipse/nebula/m3/NebulaM3CurrentMasterReproofTest.java";

    public static final String BEFORE =
            "975600aaa0dc984ac9c73e1d4fc446a0872180047701598bcc475cb9978ee7d0";
    public static final String AFTER =
            "b424349855198f0dab5f0ba393b2195bd0e603f2af6ca82d8cebf6129dad3a8b";

    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/reproof-header/"
                    + "NebulaM3CurrentMasterReproofTest.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 align Nebula current-master reproof header assertion";
    }

    @Override
    public String getDescription() {
        return "Replay one reviewed Java-21 JUnit assertion header correction from the exact "
                + "current-master preimage, preserving all data and product contracts.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "junit",
                "evidence",
                "documentation",
                "schema",
                "file-local",
                "behavior-contract-preserving",
                "candidate-only",
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
