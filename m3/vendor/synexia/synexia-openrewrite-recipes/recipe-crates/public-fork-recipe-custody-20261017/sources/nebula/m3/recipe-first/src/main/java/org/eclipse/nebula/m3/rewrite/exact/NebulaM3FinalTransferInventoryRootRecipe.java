// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import java.util.Set;

/**
 * Exact FILE-local repair for final-transfer inventory property custody.
 *
 * <p>The final-transfer inventory invocation must use the recipe-first module's default
 * repository-root value instead of exporting a global {@code m3.nebula.root} user property. The
 * global override leaks into the pinned OpenRewrite proof module, where the same property names the
 * immutable original-Nebula fixture root. Removing only that CLI override restores both owners
 * without changing product source or fixture identity.</p>
 */
public final class NebulaM3FinalTransferInventoryRootRecipe
        extends NebulaM3ExactTextSnapshotRecipe {
    public static final String PATH =
            ".github/workflows/m3-final-proven-transfer.yml";
    public static final String BEFORE =
            "c939eb22eee298378e9f99d82e88590523605c5597d04be4a96e3f407de9af28";
    public static final String AFTER =
            "bb9bd3638acf4d34a555a619b66372a95f06523a1bc89c7b546e302c6213c2ab";
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/final-transfer-inventory-root/"
                    + "m3-final-proven-transfer.after.yml.txt";

    @Override
    public String getDisplayName() {
        return "M3 isolate final-transfer inventory root from pinned fixture root";
    }

    @Override
    public String getDescription() {
        return "Removes the exact global m3.nebula.root CLI override from the final-transfer "
                + "inventory step so repository inventory uses its module default while the "
                + "pinned parser proof retains its immutable fixture root.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "nebula",
                "openrewrite",
                "recipe-first",
                "workflow",
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
