// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Repairs the actual compilation and failed-result admission blockers before running the existing full recipe crate.
 * This composes the existing exact-Java engine; it is not another parser or rewrite engine.
 * Each leaf remains source-pinned and one-cycle. Repository-wide atomization is a later gate.
 */
public final class NebulaM3CompileBootstrapRecipe extends Recipe {
    static final String RESOURCE = "/org/eclipse/nebula/m3/rewrite/exact/compile-bootstrap/";

    static List<NebulaM3ExactJavaSnapshotRecipe> snapshots() {
        return List.of(new ReviewTable(), new ReviewComposition(), new FailedResultAdmission());
    }

    @Override public String getDisplayName() { return "Restore Nebula recipe-crate compilation"; }
    @Override public String getDescription() {
        return "Restores the existing ordered review composition and declared pass-order accessor "
                + "from exact preimages before compiling and executing the full atom-pattern crate.";
    }
    @Override public Set<String> getTags() { return Set.of("m3", "recipe-first", "exact-source", "bootstrap"); }
    @Override public int maxCycles() { return 1; }
    @Override public List<Recipe> getRecipeList() {
        return snapshots().stream().map(Recipe.class::cast).toList();
    }

    public static final class ReviewTable extends NebulaM3ExactJavaSnapshotRecipe {
        @Override protected String repositoryPath() { return "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewTable.java"; }
        @Override protected String moduleRelativePath() { return "src/main/java/org/eclipse/nebula/m3/NebulaM3FastSearchReviewTable.java"; }
        @Override protected String beforeSha256() { return "8873857934e27684ed66c8de97a7232a4011a69c9e8363d1d911c233999f8e71"; }
        @Override protected String afterSha256() { return "aaaeb0e63a6a15382e3303c960d24e6da8a56b751800fb9ca82d75543837ed57"; }
        @Override protected String afterResource() { return RESOURCE + "NebulaM3FastSearchReviewTable.java.after.txt"; }
        @Override public String getDisplayName() { return "Restore compiled Nebula ReviewTable"; }
        @Override public String getDescription() {
            return "Replays one exact reviewed compile repair, preserving existing public signatures.";
        }
    }

    public static final class ReviewComposition extends NebulaM3ExactJavaSnapshotRecipe {
        @Override protected String repositoryPath() { return "m3/recipe-first/src/main/java/org/eclipse/nebula/m3/NebulaM3RepositoryReviewRecipe.java"; }
        @Override protected String moduleRelativePath() { return "src/main/java/org/eclipse/nebula/m3/NebulaM3RepositoryReviewRecipe.java"; }
        @Override protected String beforeSha256() { return "1c74e3420b318a7836da9de9f29529ce3f2daf34ea82d5bb018bdc8f6db97037"; }
        @Override protected String afterSha256() { return "dc4e1c354b8eeb5962971aaacbe0b5ee4ec45c3729c3488e840d74290ba5738c"; }
        @Override protected String afterResource() { return RESOURCE + "NebulaM3RepositoryReviewRecipe.java.after.txt"; }
        @Override public String getDisplayName() { return "Restore compiled Nebula ReviewComposition"; }
        @Override public String getDescription() {
            return "Replays one exact reviewed compile repair, preserving existing public signatures.";
        }
    }

    public static final class FailedResultAdmission extends NebulaM3ExactJavaSnapshotRecipe {
        @Override protected String repositoryPath() { return "m3/recipe-first/src/test/java/org/eclipse/nebula/m3/rewrite/exact/NebulaM3SvgLoaderLengthConvergenceRecipeTest.java"; }
        @Override protected String moduleRelativePath() { return "src/test/java/org/eclipse/nebula/m3/rewrite/exact/NebulaM3SvgLoaderLengthConvergenceRecipeTest.java"; }
        @Override protected String beforeSha256() { return "7dfb38bd50cfedef2ec3e5084afa4c4c7a0a97a423f6fe82f918e391c185a6fd"; }
        @Override protected String afterSha256() { return "9c6521e155d89b5f6c20e66355d59a5af3c6f00e1b7564f3d4558bbcb6dff08a"; }
        @Override protected String afterResource() { return RESOURCE + "NebulaM3SvgLoaderLengthConvergenceRecipeTest.java.after.txt"; }
        @Override public String getDisplayName() { return "Reject failed recipe diagnostic results"; }
        @Override public String getDescription() {
            return "Makes the existing replay test host fail immediately on execution errors, "
                    + "retaining the no-candidate-on-source-drift contract.";
        }
    }
}
