// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact FILE-local transfer-CLI proof-receipt addition.
 *
 * <p>This target-specific Nebula leaf delegates source mutation to the existing Java LST snapshot
 * engine. The generalized recipe/proof machinery remains canonically owned by Synexia. It adds a
 * fail-closed HOLD sidecar without altering the existing transfer contract or product widgets.</p>
 */
public final class NebulaM3TransferQualificationRecipe
        extends NebulaM3ExactJavaSnapshotRecipe {

    public static final String REPOSITORY_PATH =
            "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3TransferContractCli.java";
    public static final String MODULE_PATH =
            "src/main/java/org/eclipse/nebula/m3/rewrite/convergence/"
                    + "NebulaM3TransferContractCli.java";
    public static final String BEFORE =
            "aaf3668ec76dbb22c9e79b59214943da5857cd951d6b872f079b7be9c3c02e75";
    public static final String AFTER =
            "ada3f23d0442849e4fb9846b5774829d0e8ed68a4e75a4f33b8902c58c6d4ff4";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/proof-gate/"
                    + "NebulaM3TransferContractCli.after.java.txt";

    @Override
    public String getDisplayName() {
        return "M3 add a fail-closed Nebula transfer qualification receipt";
    }

    @Override
    public String getDescription() {
        return "Replay one reviewed, source-bound transfer-CLI output addition while retaining "
                + "the existing transfer bundle and preventing missing proof from becoming PASS.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "nebula", "proof", "java21", "recipe-first",
                "file-local", "behavior-contract-preserving", "candidate-only");
    }

    @Override
    protected String repositoryPath() { return REPOSITORY_PATH; }

    @Override
    protected String moduleRelativePath() { return MODULE_PATH; }

    @Override
    protected String beforeSha256() { return BEFORE; }

    @Override
    protected String afterSha256() { return AFTER; }

    @Override
    protected String afterResource() { return RESOURCE; }
}
