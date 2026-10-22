// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.eclipse.nebula.m3.rewrite.convergence.NebulaM3UiBehaviorDonorCatalog;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

/**
 * Read-only repository review atom that emits the content-addressed Java2s UI behavior obligations.
 *
 * <p>The recipe deliberately emits one catalogue ledger per run, not one copy per source file.
 * It never changes source and grants no source-copy, native-execution, or promotion authority.</p>
 */
public final class NebulaM3UiBehaviorReviewRecipe
        extends ScanningRecipe<NebulaM3UiBehaviorReviewRecipe.State> {
    private final transient NebulaM3UiBehaviorReviewTable review =
            new NebulaM3UiBehaviorReviewTable(this);

    static final class State {
        boolean javaSeen;
        boolean emitted;
    }

    @Override
    public String getDisplayName() {
        return "Review Nebula against SWT/Swing observable UI behavior donors";
    }

    @Override
    public String getDescription() {
        return "Emits the four supplied Java2s SWT, SWT 2D Graphics, Swing and Swing Event "
                + "catalogues as deterministic observable-behavior obligations without editing source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "nebula",
                "m3",
                "read-only",
                "ui-behavior",
                "donor-review",
                "java2s",
                "recipe-first",
                "no-source-copy");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public State getInitialValue(ExecutionContext context) {
        return new State();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext context) {
                if (tree instanceof J.CompilationUnit) {
                    synchronized (state) {
                        state.javaSeen = true;
                    }
                }
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(
            State state,
            Collection<SourceFile> generatedInThisCycle,
            ExecutionContext context) {
        synchronized (state) {
            if (!state.javaSeen || state.emitted) return List.of();
            for (NebulaM3UiBehaviorDonorCatalog.Source source :
                    NebulaM3UiBehaviorDonorCatalog.sources()) {
                review.insertRow(context, new NebulaM3UiBehaviorReviewTable.Row(source));
            }
            state.emitted = true;
        }
        return List.of();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {};
    }

    public boolean sourceCopyAuthority() {
        return false;
    }

    public boolean sourceMutationAuthority() {
        return false;
    }

    public boolean nativeExecutionAuthority() {
        return false;
    }

    public boolean promotionAuthority() {
        return false;
    }
}
