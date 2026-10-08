// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

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

/** Adds idempotent semantic Javadoc to an admitted atomized/patternized pure-int FILE leaf. */
public final class M3PureIntDocumentationRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Document M3 pure-int FILE atom";
    }

    @Override
    public String getDescription() {
        return "Adds semantic memory for the M3 atom and PURE_INT_EXPRESSION pattern/IOP role.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "documentation", "javadoc", "iop",
                "file-local", "behavior-contract-preserving");
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
                if (!M3PureIntLeaf.atomized(candidate) || hasDocumentation(candidate)) {
                    return candidate;
                }

                String whitespace = candidate.getPrefix().getWhitespace();
                int newline =
                        Math.max(
                                whitespace.lastIndexOf('\n'),
                                whitespace.lastIndexOf('\r'));
                String indent = newline < 0 ? whitespace : whitespace.substring(newline + 1);

                List<Comment> comments = new ArrayList<>(candidate.getComments());
                comments.add(
                        new TextComment(
                                true,
                                "* " + M3PureIntLeaf.DOC_MARKER + " ",
                                "\n" + indent,
                                Markers.EMPTY));
                return candidate.withComments(comments);
            }
        };
    }

    private static boolean hasDocumentation(J.MethodDeclaration method) {
        for (Comment comment : method.getComments()) {
            if (comment instanceof TextComment text
                    && text.getText().contains(M3PureIntLeaf.DOC_MARKER)) {
                return true;
            }
            if (comment instanceof Javadoc.DocComment doc
                    && doc.getBody().stream()
                            .filter(Javadoc.Text.class::isInstance)
                            .map(Javadoc.Text.class::cast)
                            .anyMatch(text -> text.getText().contains(M3PureIntLeaf.DOC_MARKER))) {
                return true;
            }
        }
        return false;
    }
}
