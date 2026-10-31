// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/** Ensures the admitted M3-IOP role marker exists on an already atomized pure-int leaf. */
public final class M3PatternizePureIntAtomRecipe extends Recipe {
    private static final String MARKER = "M3-IOP: " + M3PureIntAtomEligibility.PATTERN_ROLE;

    @Override
    public String getDisplayName() {
        return "M3 patternize pure-int atom";
    }

    @Override
    public String getDescription() {
        return "Adds the admitted M3-IOP role marker to an already atomized FILE-local pure-int leaf.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "patternization", "iop", "file-local", "behavior-contract-preserving");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!M3PureIntAtomEligibility.atomized(candidate)) {
                    return candidate;
                }

                J.VariableDeclarations atom = M3PureIntAtomEligibility.atomizedVariable(candidate);
                if (hasMarker(atom)) {
                    return candidate;
                }

                List<Comment> comments = new ArrayList<>(atom.getComments());
                comments.add(new TextComment(true, " " + MARKER + " ", " ", Markers.EMPTY));
                J.VariableDeclarations patternized = atom.withComments(comments);
                List<Statement> statements = new ArrayList<>(candidate.getBody().getStatements());
                statements.set(0, patternized);
                return candidate.withBody(candidate.getBody().withStatements(statements));
            }
        };
    }

    private static boolean hasMarker(J.VariableDeclarations atom) {
        return atom.getComments().stream()
                .filter(TextComment.class::isInstance)
                .map(TextComment.class::cast)
                .anyMatch(comment -> comment.getText().contains(MARKER));
    }
}
