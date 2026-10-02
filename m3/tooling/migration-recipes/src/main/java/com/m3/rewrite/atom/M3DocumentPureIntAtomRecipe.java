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
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/** Converges semantic documentation for an admitted atomized/patternized pure-int FILE leaf. */
public final class M3DocumentPureIntAtomRecipe extends Recipe {
    private static final String DOC_ID =
            "M3-ATOM: " + M3PureIntAtomEligibility.ATOM_NAME
                    + "; Pattern/IOP: " + M3PureIntAtomEligibility.PATTERN_ROLE + ".";

    @Override
    public String getDisplayName() {
        return "M3 document pure-int atom";
    }

    @Override
    public String getDescription() {
        return "Adds idempotent Javadoc semantic memory for the admitted pure-int atom and IOP role.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "documentation", "javadoc", "iop", "file-local", "behavior-contract-preserving");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!M3PureIntAtomEligibility.atomized(candidate) || hasDocumentation(candidate)) {
                    return candidate;
                }

                String whitespace = candidate.getPrefix().getWhitespace();
                int newline = Math.max(whitespace.lastIndexOf('\n'), whitespace.lastIndexOf('\r'));
                String indent = newline < 0 ? whitespace : whitespace.substring(newline + 1);
                List<Comment> comments = new ArrayList<>(candidate.getComments());
                comments.add(new TextComment(true, "* " + DOC_ID + " ", "\n" + indent, Markers.EMPTY));
                return candidate.withComments(comments);
            }
        };
    }

    private static boolean hasDocumentation(J.MethodDeclaration method) {
        return method.getComments().stream()
                .filter(TextComment.class::isInstance)
                .map(TextComment.class::cast)
                .anyMatch(comment -> comment.getText().contains(DOC_ID));
    }
}
