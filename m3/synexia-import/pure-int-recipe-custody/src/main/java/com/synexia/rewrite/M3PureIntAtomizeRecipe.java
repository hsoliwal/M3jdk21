// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

/** Extracts one admitted private-static pure-int return expression into one local M3 atom. */
public final class M3PureIntAtomizeRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Atomize M3 pure-int FILE leaf";
    }

    @Override
    public String getDescription() {
        return "Extracts one proven pure primitive-int return expression into a named local M3 "
                + "atom without changing the member/API surface.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "atomization", "file-local", "behavior-contract-preserving");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate template =
                    JavaTemplate.builder(
                                    "int "
                                            + M3PureIntLeaf.ATOM_NAME
                                            + " = #{any(int)}; return "
                                            + M3PureIntLeaf.ATOM_NAME
                                            + ";")
                            .contextSensitive()
                            .build();

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!M3PureIntLeaf.eligible(candidate)) {
                    return candidate;
                }
                Expression expression = M3PureIntLeaf.returnedExpression(candidate);
                return template.apply(
                        updateCursor(candidate),
                        candidate.getCoordinates().replaceBody(),
                        expression);
            }
        };
    }
}
