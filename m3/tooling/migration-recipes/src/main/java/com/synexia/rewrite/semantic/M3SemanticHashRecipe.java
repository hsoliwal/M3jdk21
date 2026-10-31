// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.semantic;

import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/** Non-mutating OpenRewrite semantic-source-hash evidence generator for Java 21 source. */
public final class M3SemanticHashRecipe extends Recipe {
    private transient M3SemanticHashTable hashes = new M3SemanticHashTable(this);

    @Override
    public String getDisplayName() {
        return "Generate M3 Java 21 semantic hashes";
    }

    @Override
    public String getDescription() {
        return "Emits layered METHOD and FILE semantic-source fingerprints without modifying source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of("m3", "java21", "semantic-hash", "contract-hash", "logic-hash",
                "patternization", "iop", "inventory", "non-mutating");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration candidate = super.visitMethodDeclaration(method, ctx);
                var fp = M3SemanticHasher.method(candidate.printTrimmed(getCursor()));
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                hashes.insertRow(ctx, row(
                        cu == null ? "" : normalized(cu.getSourcePath().toString()),
                        "METHOD",
                        candidate.getSimpleName(),
                        fp));
                return candidate;
            }

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                J.CompilationUnit candidate = super.visitCompilationUnit(cu, ctx);
                var fp = M3SemanticHasher.file(candidate.printAll());
                hashes.insertRow(ctx, row(
                        normalized(candidate.getSourcePath().toString()),
                        "FILE",
                        "<file>",
                        fp));
                return candidate;
            }
        };
    }

    private static M3SemanticHashTable.Row row(
            String path,
            String level,
            String symbol,
            M3SemanticHasher.Fingerprint fp) {
        return new M3SemanticHashTable.Row(
                path, level, symbol, fp.contractHash(), fp.logicHash(), fp.architectureHash(),
                fp.behavioralHash(), fp.wholeHash());
    }

    private static String normalized(String path) {
        return path.replace('\\', '/');
    }
}
