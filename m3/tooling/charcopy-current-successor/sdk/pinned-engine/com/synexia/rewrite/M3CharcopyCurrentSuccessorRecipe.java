// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;

/** Exact source-only custody; the existing receiver programs remain separate executables. */
public final class M3CharcopyCurrentSuccessorRecipe extends M3GpuToolJoinRecipe {
    @Override protected String resourceRoot() {
        return "/com/synexia/rewrite/charcopy-current-successor-20261019/";
    }
    @Override public String getDisplayName() {
        return "Verify current char-copy successor custody without changing runtime sources";
    }
    @Override public String getDescription() {
        return "Keeps eight exact inputs and refuses either legacy owner before the unchanged "
                + "source-seal scanner; historical replay and runtime qualification remain separate.";
    }
    @Override public TreeVisitor<?, ExecutionContext> getScanner(Inventory inventory) {
        TreeVisitor<?, ExecutionContext> delegate = super.getScanner(inventory);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof SourceFile source) {
                    stopAfterPreVisit();
                    String path = M3GpuToolJoinManifest.normalized(source.getSourcePath());
                    if (path.equals("src/java.base/share/classes/java/lang/MIndexString.java")
                            || path.equals("m3/runtime-integration/tests/MIndexStringInvariant.java")) {
                        throw new IllegalStateException("legacy source forbidden: " + path);
                    }
                    delegate.visit(source, context);
                }
                return tree;
            }
        };
    }
}
