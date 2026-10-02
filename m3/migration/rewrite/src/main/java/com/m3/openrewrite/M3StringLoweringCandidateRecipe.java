/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.openrewrite;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

/**
 * Candidate-only typed inventory for String operations that may be eligible for M3 lowering.
 * This pass deliberately does not rewrite source.
 */
public final class M3StringLoweringCandidateRecipe extends Recipe {
    private static final String STRING = "java.lang.String";
    private static final Set<String> STRUCTURAL = Set.of("concat", "substring", "repeat");

    @Override
    public String getDisplayName() {
        return "Find String operations eligible for M3 lowering review";
    }

    @Override
    public String getDescription() {
        return "Marks typed java.lang.String concat, substring and repeat calls for explicit "
                + "M3 lowering classification without changing source behavior.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3","mindex","compiler-lowering","candidate-only","recipe-first");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(
                    J.MethodInvocation method, ExecutionContext context) {
                J.MethodInvocation visited = super.visitMethodInvocation(method, context);
                if (!STRUCTURAL.contains(visited.getSimpleName())
                        || visited.getSelect() == null
                        || !TypeUtils.isOfClassType(visited.getSelect().getType(), STRING)) {
                    return visited;
                }
                return SearchResult.found(
                        visited,
                        "M3 candidate only: prove evaluation/escape/identity/exception preconditions before lowering");
            }
        };
    }
}
