// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/** Read-only inventory for the exact FILE domain admitted by {@link M3PureIntAtomizeRecipe}. */
public final class M3PureIntInventoryRecipe extends Recipe {
    private final transient CandidateTable candidates = new CandidateTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory M3 pure-int FILE atoms";
    }

    @Override
    public String getDescription() {
        return "Emits deterministic rows for private static int methods admitted by the M3 "
                + "pure-int atomization contract; source is never modified.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "inventory", "atomization", "patternization", "iop",
                "file-local", "behavior-contract-preserving", "non-mutating");
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
                if (!M3PureIntLeaf.eligible(candidate)) {
                    return candidate;
                }
                J.CompilationUnit unit = getCursor().firstEnclosing(J.CompilationUnit.class);
                Path path = unit == null ? Path.of("") : unit.getSourcePath();
                candidates.insertRow(
                        context,
                        new CandidateRow(
                                normalize(path),
                                candidate.getSimpleName(),
                                "FILE",
                                M3PureIntLeaf.PATTERN_ROLE,
                                M3PureIntAtomizeRecipe.class.getName()));
                return candidate;
            }
        };
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    static final class CandidateTable extends DataTable<CandidateRow> {
        CandidateTable(Recipe owner) {
            super(
                    owner,
                    "M3 pure-int atom candidates",
                    "Read-only FILE-local pure-int atomization candidates.");
        }
    }

    static final class CandidateRow {
        @Column(displayName = "Source path", description = "Normalized Java source path.")
        private final String sourcePath;

        @Column(displayName = "Method", description = "Candidate method name.")
        private final String method;

        @Column(displayName = "Scope", description = "Required semantic edit scope.")
        private final String scope;

        @Column(displayName = "Pattern role", description = "Admitted M3 pattern/IOP role.")
        private final String patternRole;

        @Column(displayName = "Recipe", description = "Canonical source-changing recipe class.")
        private final String recipe;

        CandidateRow(
                String sourcePath,
                String method,
                String scope,
                String patternRole,
                String recipe) {
            this.sourcePath = sourcePath;
            this.method = method;
            this.scope = scope;
            this.patternRole = patternRole;
            this.recipe = recipe;
        }

        public String getSourcePath() { return sourcePath; }
        public String getMethod() { return method; }
        public String getScope() { return scope; }
        public String getPatternRole() { return patternRole; }
        public String getRecipe() { return recipe; }
    }
}
