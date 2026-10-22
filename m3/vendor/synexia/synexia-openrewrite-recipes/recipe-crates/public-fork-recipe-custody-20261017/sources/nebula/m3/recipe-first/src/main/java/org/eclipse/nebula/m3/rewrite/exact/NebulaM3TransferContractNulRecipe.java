// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/** Exact FILE-local byte repair for the final transfer contract source. */
public final class NebulaM3TransferContractNulRecipe
        extends NebulaM3ExactTextSnapshotRecipe {
    public static final String PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3TransferContract.java";
    public static final String BEFORE =
            "8e8fdb01129f4b21d785df56c4ec8ac4620e72dd3424f785bfbd64fada90df10";
    public static final String AFTER =
            "65667689fd845716ae470c82f7f1df1f98c1d21de173415243b32c01a5b789c8";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/transfer-contract-nul/"
                    + "NebulaM3TransferContract.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 remove embedded NUL from Nebula transfer contract source";
    }

    @Override
    public String getDescription() {
        return "Replaces the exact malformed Java source preimage containing an embedded NUL "
                + "character with the equivalent source expression checked.indexOf(0), while "
                + "preserving the validator contract and refusing any unknown source drift.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "openrewrite",
                "recipe-first",
                "java-source-byte-repair",
                "file-local",
                "candidate-only",
                "behavior-contract-preserving",
                "exact-source");
    }

    @Override
    protected String repositoryPath() {
        return PATH;
    }

    @Override
    protected String moduleRelativePath() {
        return PATH;
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
