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
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

/** Adds the admitted IOP role marker to an already atomized pure-int FILE leaf. */
public final class M3PureIntPatternizeRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Patternize M3 pure-int FILE atom";
    }

    @Override
    public String getDescription() {
        return "Adds the deterministic PURE_INT_EXPRESSION M3-IOP marker to an admitted atom.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "patternization", "iop", "file-local", "behavior-contract-preserving");
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
                if (!M3PureIntLeaf.atomized(candidate)) {
                    return candidate;
                }

                J.VariableDeclarations atom = M3PureIntLeaf.atomVariable(candidate);
                if (hasMarker(atom)) {
                    return candidate;
                }

                List<Comment> comments = new ArrayList<>(atom.getComments());
                comments.add(
                        new TextComment(
                                true,
                                " " + M3PureIntLeaf.IOP_MARKER + " ",
                                " ",
                                Markers.EMPTY));
                J.VariableDeclarations marked = atom.withComments(comments);
                List<Statement> statements =
                        new ArrayList<>(candidate.getBody().getStatements());
                statements.set(0, marked);
                return candidate.withBody(candidate.getBody().withStatements(statements));
            }
        };
    }

    private static boolean hasMarker(J.VariableDeclarations atom) {
        return atom.getComments().stream()
                .filter(TextComment.class::isInstance)
                .map(TextComment.class::cast)
                .anyMatch(comment -> comment.getText().contains(M3PureIntLeaf.IOP_MARKER));
    }
}
