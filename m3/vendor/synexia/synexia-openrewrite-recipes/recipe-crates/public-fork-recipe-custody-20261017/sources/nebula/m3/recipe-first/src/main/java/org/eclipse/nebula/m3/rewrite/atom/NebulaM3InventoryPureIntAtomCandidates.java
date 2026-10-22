// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.atom;

import java.nio.file.Path;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Non-mutating OpenRewrite inventory for the exact FILE domain handled by
 * {@link NebulaM3AtomizePureIntReturnRecipe}.
 */
public final class NebulaM3InventoryPureIntAtomCandidates extends Recipe {
    private transient NebulaM3FileAtomCandidateTable candidates = new NebulaM3FileAtomCandidateTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory M3 FILE pure-int atom candidates";
    }

    @Override
    public String getDescription() {
        return "Emits deterministic candidate rows for the same contract-preserving FILE leaves "
                + "accepted by the pure-int atomization recipe; source is not modified.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "inventory",
                "atomization",
                "patternization",
                "iop",
                "file-local",
                "non-mutating",
                "behavior-contract-preserving");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, ctx);
                if (!NebulaM3PureIntAtomEligibility.eligible(candidate)) {
                    return candidate;
                }

                J.CompilationUnit compilationUnit =
                        getCursor().firstEnclosing(J.CompilationUnit.class);
                Path sourcePath = compilationUnit == null
                        ? Path.of("")
                        : compilationUnit.getSourcePath();

                candidates.insertRow(
                        ctx,
                        new NebulaM3FileAtomCandidateTable.Row(
                                normalized(sourcePath),
                                candidate.getSimpleName(),
                                "FILE",
                                "BEHAVIOR_AND_CONTRACT_PRESERVING",
                                NebulaM3PureIntAtomEligibility.PATTERN_ROLE,
                                NebulaM3AtomizePureIntReturnRecipe.class.getName()));
                return candidate;
            }
        };
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
