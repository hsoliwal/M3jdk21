// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/** Converges semantic documentation for an admitted atomized/patternized pure-int FILE leaf. */
public final class NebulaM3DocumentPureIntAtomRecipe extends Recipe {
    private static final String DOC_ID =
            "M3-ATOM: " + NebulaM3PureIntAtomEligibility.ATOM_NAME
                    + "; Pattern/IOP: " + NebulaM3PureIntAtomEligibility.PATTERN_ROLE + ".";

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
    public int maxCycles() {
        return 1;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext context) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, context);
                if (!NebulaM3PureIntAtomEligibility.atomized(candidate) || hasDocumentation(candidate)) {
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
        for (Comment comment : method.getComments()) {
            if (comment instanceof TextComment textComment
                    && textComment.getText().contains(DOC_ID)) {
                return true;
            }
            if (comment instanceof Javadoc.DocComment docComment
                    && docComment.getBody().stream()
                            .filter(Javadoc.Text.class::isInstance)
                            .map(Javadoc.Text.class::cast)
                            .anyMatch(text -> text.getText().contains(DOC_ID))) {
                return true;
            }
        }
        return false;
    }
}
