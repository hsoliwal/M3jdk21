// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** M3 recipe: inventory every admitted SourceFile, then generate L0–L5 derived documentation. */
public final class M3HierarchicalDocumentationRecipe
        extends ScanningRecipe<M3HierarchicalDocumentationRecipe.State> {
    private final String projectName;
    private final String repositoryName;

    /** Default names signal unresolved binding; the runner should supply explicit scope labels. */
    public M3HierarchicalDocumentationRecipe() {
        this("UNBOUND_PROJECT", "UNBOUND_REPOSITORY");
    }

    public M3HierarchicalDocumentationRecipe(String projectName, String repositoryName) {
        this.projectName = projectName;
        this.repositoryName = repositoryName;
    }

    @Override public String getDisplayName() { return "M3 FLLDCIM six-level structural documentation"; }
    @Override public String getDescription() {
        return "Create deterministic, provenance-hashed L0 file through L5 repository documentation "
                + "from admitted OpenRewrite SourceFiles, without touching production source.";
    }
    @Override public int maxCycles() { return 1; }
    @Override public State getInitialValue(ExecutionContext ctx) { return new State(); }

    public static final class State {
        private final SortedMap<String, M3HierarchicalDocumentation.Source> originals = new TreeMap<>();
        private final SortedMap<String, String> existing = new TreeMap<>();
        private SortedMap<String, String> planned;
        synchronized void record(SourceFile file, ExecutionContext ctx) {
            String path = file.getSourcePath().normalize().toString().replace('\\', '/');
            if (path.startsWith(M3HierarchicalDocumentation.OUTPUT)) {
                if (existing.putIfAbsent(path, file.printAll()) != null) {
                    throw new IllegalStateException("duplicate generated document: " + path);
                }
                if (!(file instanceof PlainText)
                        || !file.printAll().startsWith(M3HierarchicalDocumentation.MARKER)) {
                    throw new IllegalStateException("unowned generated document: " + path);
                }
                return;
            }
            // OpenRewrite controls the parser; a binary file cannot be represented as a text SourceFile.
            String printed = file.printAll();
            if (printed.indexOf('\0') >= 0) {
                throw new IllegalStateException("binary/NUL input cannot be documented as text: " + path);
            }
            String packageName = "";
            List<String> symbols = List.of();
            String kind = file instanceof J.CompilationUnit ? "JAVA_AST" : "SOURCEFILE_STRUCTURAL_ONLY";
            if (file instanceof J.CompilationUnit unit) {
                if (unit.getPackageDeclaration() != null) {
                    packageName = unit.getPackageDeclaration().getExpression().printTrimmed();
                }
                List<String> recognized = new ArrayList<>();
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration decl, ExecutionContext context) {
                        recognized.add("type:" + decl.getSimpleName());
                        return super.visitClassDeclaration(decl, context);
                    }
                    @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext context) {
                        recognized.add("method:" + method.getSimpleName() + "/" + method.getParameters().size());
                        return super.visitMethodDeclaration(method, context);
                    }
                }.visit(unit, ctx);
                symbols = List.copyOf(recognized);
            }
            var fact = new M3HierarchicalDocumentation.Source(path, M3HierarchicalDocumentation.sha256(printed),
                    printed.getBytes(StandardCharsets.UTF_8).length, kind, packageName, symbols);
            if (originals.putIfAbsent(path, fact) != null) {
                throw new IllegalStateException("duplicate SourceFile path: " + path);
            }
        }
        synchronized SortedMap<String, String> plan(String projectName, String repositoryName) {
            if (planned == null) {
                planned = M3HierarchicalDocumentation.render(originals.values(), projectName, repositoryName);
                for (Map.Entry<String, String> old : existing.entrySet()) {
                    if (!planned.containsKey(old.getKey())) {
                        throw new IllegalStateException("stale generated document requires explicit review: " + old.getKey());
                    }
                }
            }
            return planned;
        }
    }

    @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile source) state.record(source, ctx);
                return tree;
            }
        };
    }

    @Override public Collection<? extends SourceFile> generate(State state, ExecutionContext ctx) {
        SortedMap<String, String> docs = state.plan(projectName, repositoryName);
        List<SourceFile> additions = new ArrayList<>();
        for (Map.Entry<String, String> doc : docs.entrySet()) {
            if (state.existing.containsKey(doc.getKey())) continue;
            additions.add(PlainText.builder().id(Tree.randomId()).sourcePath(Path.of(doc.getKey()))
                    .charsetName(StandardCharsets.UTF_8.name()).text(doc.getValue()).build());
        }
        return List.copyOf(additions);
    }

    @Override public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        SortedMap<String, String> docs = state.plan(projectName, repositoryName);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile file)) return tree;
                String path = file.getSourcePath().normalize().toString().replace('\\', '/');
                String desired = docs.get(path);
                if (desired == null || desired.equals(file.printAll())) return tree;
                if (!(file instanceof PlainText existing)
                        || !file.printAll().startsWith(M3HierarchicalDocumentation.MARKER)) {
                    throw new IllegalStateException("unowned overwrite refused: " + path);
                }
                return existing.withText(desired);
            }
        };
    }

    public boolean replacementAuthority() { return false; }
    public boolean promotionAuthority() { return false; }
}
