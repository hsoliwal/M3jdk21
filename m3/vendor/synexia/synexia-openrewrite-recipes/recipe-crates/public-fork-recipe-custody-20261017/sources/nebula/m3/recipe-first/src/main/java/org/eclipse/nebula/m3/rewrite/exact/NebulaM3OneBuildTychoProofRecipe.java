// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Two-leaf, exact-preimage OpenRewrite DAG for postimage Nebula Tycho qualification.
 *
 * <p>One leaf updates the disposable one-build workflow, the other its documentation. Both are
 * single-cycle, hash-pinned text transformations. A recipe result never grants ABI/behavioral
 * equivalence, publication or transfer authority; the actual Java 21/Tycho CI job is the oracle.</p>
 */
public final class NebulaM3OneBuildTychoProofRecipe extends Recipe {
    public static final String WORKFLOW_PATH =
            ".github/workflows/m3-one-build-openrewrite-convergence.yml";
    public static final String DOCUMENT_PATH =
            "m3/recipe-first/ONE_BUILD_CONVERGENCE_20261014.md";
    public static final String RESOURCE_ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/one-build-tycho-proof/";

    @Override
    public String getDisplayName() {
        return "Require Tycho verification after M3 one-build recipe convergence";
    }

    @Override
    public String getDescription() {
        return "Replays exact workflow and documentation postimages that require product Tycho "
                + "verification and byte-identical FILE candidates after OpenRewrite fixed point.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("nebula", "m3", "java21", "recipe-first", "candidate-only", "tycho");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(new WorkflowLeaf(), new DocumentationLeaf());
    }

    /** Exact GitHub Actions workflow transformation; source identity is not inferred from name. */
    public static final class WorkflowLeaf extends NebulaM3ExactTextSnapshotRecipe {
        public static final String BEFORE =
                "640ca84efe2bcbce5df5d53ef9d33f0ba471492812324f0fdb9f4dd254c0d38d";
        public static final String AFTER =
                "1f01f98ba3208d0c4ea4add5382b34eeb05f648995ee1b0c1c8ef4a1bd2b6b7f";

        @Override
        public String getDisplayName() {
            return "Seal one-build Tycho and unchanged-candidate gates";
        }

        @Override
        public String getDescription() {
            return "Admit only the reviewed one-build workflow preimage or its exact postimage.";
        }

        @Override
        protected String repositoryPath() {
            return WORKFLOW_PATH;
        }

        @Override
        protected String moduleRelativePath() {
            return WORKFLOW_PATH;
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
            return RESOURCE_ROOT + "workflow.after.txt";
        }
    }

    /** Exact documentation transformation; no broader source or library edit authority. */
    public static final class DocumentationLeaf extends NebulaM3ExactTextSnapshotRecipe {
        public static final String BEFORE =
                "4c983e6083a93810c992f89f19fbf3ada36547322a0adc7207ef463ee4462779";
        public static final String AFTER =
                "c10d1e7591f94e2175dbb110ba3d344208045b0f55d8dd96d75fa9d53fa2b153";

        @Override
        public String getDisplayName() {
            return "Document postimage Tycho qualification boundary";
        }

        @Override
        public String getDescription() {
            return "Bind the workflow's product-build and candidate-diff gates to exact documentation.";
        }

        @Override
        protected String repositoryPath() {
            return DOCUMENT_PATH;
        }

        @Override
        protected String moduleRelativePath() {
            return "ONE_BUILD_CONVERGENCE_20261014.md";
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
            return RESOURCE_ROOT + "documentation.after.txt";
        }
    }
}
