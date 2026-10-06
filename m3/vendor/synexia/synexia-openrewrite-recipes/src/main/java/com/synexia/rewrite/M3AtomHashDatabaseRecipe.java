// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.synexia.rewrite.atomdb.AtomCatalog;
import com.synexia.rewrite.atomdb.AtomFingerprints;
import com.synexia.rewrite.atomdb.JavaAtomCatalogScanner;
import com.synexia.rewrite.sealed.SealHash;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;
import org.openrewrite.tree.ParseError;

/** Source-preserving catalog observation plus one deterministic SQLite SQL artifact. */
public final class M3AtomHashDatabaseRecipe extends ScanningRecipe<M3AtomHashDatabaseRecipe.State> {
    public static final String SCOPE_BINDINGS = "com.synexia.atomdb.scopeBindings.v1";
    // Existing inventory monitor protocol; a host adapts its monitor through these callbacks.
    public static final String CANCELLATION = "com.synexia.m3.sourceVisibility.canceled";
    public static final String PROGRESS = "com.synexia.m3.sourceVisibility.worked";
    @Option(displayName="Repository", description="Explicit repository identity.", example="com.synexia")
    private final String repository;
    @Option(displayName="Project", description="Default project; hosts can bind exact paths.", example="synexia")
    private final String project;
    @Option(displayName="Library", description="Owned library grouping, not inferred dependency resolution.", example="synexia")
    private final String library;
    @Option(displayName="Module", description="Default module; hosts can bind exact paths.", example="core")
    private final String module;
    @Option(displayName="Source revision", description="Caller-supplied revision or UNSPECIFIED.", example="UNSPECIFIED")
    private final String sourceRevision;
    @Option(displayName="Output SQL", description="Safe relative SQL catalog path.", example="atom-catalog.sql")
    private final String outputPath;
    @Option(displayName="Maximum catalog nodes", description="Explicit resource bound for one snapshot.", example="250000", required=false)
    private final int maxNodes;
    public M3AtomHashDatabaseRecipe() { this("local", "root", "owned", "root", "UNSPECIFIED", "atom-catalog.sql"); }
    public M3AtomHashDatabaseRecipe(String repository, String project, String library, String module,
                                    String sourceRevision, String outputPath) {
        this(repository, project, library, module, sourceRevision, outputPath, null);
    }
    @JsonCreator public M3AtomHashDatabaseRecipe(
            @JsonProperty("repository") String repository, @JsonProperty("project") String project,
            @JsonProperty("library") String library, @JsonProperty("module") String module,
            @JsonProperty("sourceRevision") String sourceRevision, @JsonProperty("outputPath") String outputPath,
            @JsonProperty("maxNodes") Integer maxNodes) {
        this.repository=repository; this.project=project; this.library=library; this.module=module;
        this.sourceRevision=sourceRevision; this.outputPath=safePath(outputPath);
        this.maxNodes = maxNodes == null ? AtomCatalog.DEFAULT_MAX_NODES : maxNodes;
        new AtomCatalog.Scope(project, library, module);
        new AtomCatalog(repository, sourceRevision, this.maxNodes);
        if (!outputPath.endsWith(".sql")) throw new IllegalArgumentException("SQL output required");
    }
    public String getRepository() { return repository; }
    public String getProject() { return project; }
    public String getLibrary() { return library; }
    public String getModule() { return module; }
    public String getSourceRevision() { return sourceRevision; }
    public String getOutputPath() { return outputPath; }
    public int getMaxNodes() { return maxNodes; }
    @Override public String getDisplayName() { return "Generate organized atom hash database"; }
    @Override public String getDescription() {
        return "Catalog Java AST atoms, fields, methods, declarations and documents with source, logic, "
                + "structure, SimHash and normalized containment composition identities; export SQLite SQL.";
    }
    @Override public int maxCycles() { return 1; }
    @Override public boolean causesAnotherCycle() { return false; }
    @Override public State getInitialValue(ExecutionContext context) {
        return new State(new AtomCatalog(repository, sourceRevision, maxNodes));
    }
    @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree visit(Tree tree, ExecutionContext context) {
                if (!(tree instanceof SourceFile source)) return tree;
                try { synchronized (state) { scan(source, state, context); } }
                catch (RuntimeException failure) {
                    state.refused=true;
                    // Report observation failure without adding error markers to the input tree.
                    context.getOnError().accept(failure);
                }
                return tree;
            }
        };
    }
    private void scan(SourceFile source, State state, ExecutionContext context) {
        checkCanceled(context);
        String path=safePath(source.getSourcePath().toString().replace('\\','/'));
        if (path.equals(outputPath)) {
            String prior=state.existing;
            state.existing=source.printAll();
            if (prior!=null && !prior.equals(state.existing)) throw new IllegalArgumentException("conflicting catalog output");
            return;
        }
        if (source instanceof ParseError) throw new IllegalArgumentException("parse error: "+path);
        String text=source.printAll();
        if (SealHash.utf8(text).length>4*1024*1024) throw new IllegalArgumentException("source byte bound exceeded");
        if (!state.catalog.observeFile(path, SealHash.text(text))) return;
        Object configured=context.getMessage(SCOPE_BINDINGS);
        AtomCatalog.Scope binding=new AtomCatalog.Scope(project,library,module);
        if (configured!=null) {
            if (!(configured instanceof Map<?,?> bindings)) throw new IllegalArgumentException("scope bindings must be a map");
            Object value=bindings.get(path);
            if (value!=null) {
                if (!(value instanceof AtomCatalog.Scope scope)) throw new IllegalArgumentException("typed source scope required");
                binding=scope;
            }
        }
        String packageName=source instanceof J.CompilationUnit unit && unit.getPackageDeclaration()!=null
                ? unit.getPackageDeclaration().getExpression().printTrimmed(
                        new org.openrewrite.Cursor(new org.openrewrite.Cursor(null, unit),
                                unit.getPackageDeclaration().getExpression())) : "<default>";
        AtomCatalog.Node parent=state.catalog.fileParent(binding,packageName);
        boolean javaSource=source instanceof J.CompilationUnit;
        boolean parseRefused=!javaSource && path.endsWith(".java");
        boolean documentSource=path.endsWith(".md") || path.endsWith(".txt") || path.endsWith(".rst");
        var fp=javaSource ? AtomFingerprints.javaTree((J)source)
                : documentSource ? AtomFingerprints.document(text) : AtomFingerprints.opaque(text);
        var file=state.catalog.occurrence("FILE",path,path,0,
                javaSource?"AST_CANDIDATE":parseRefused?"PARSE_REFUSED":documentSource?"DOC_CANDIDATE":"OPAQUE",fp,parent);
        if (javaSource) {
            new JavaAtomCatalogScanner(state.catalog,path,file,state.interfaces,state.implementations,
                    context.getMessage(CANCELLATION), context.getMessage(PROGRESS)).visit((J)source,context);
        } else if (documentSource) {
            state.catalog.occurrence("DOC",path,path,1,"DOC_CANDIDATE",fp,file);
        }
    }
    @Override public Collection<? extends SourceFile> generate(State state, Collection<SourceFile> generated,
                                                               ExecutionContext context) {
        synchronized (state) {
            if (state.refused || state.exported) return List.of();
            try {
                checkCanceled(context);
                for (SourceFile file:generated) {
                    if (file.getSourcePath().toString().replace('\\','/').equals(outputPath)) {
                        String text=file.printAll();
                        if (state.existing!=null && !state.existing.equals(text)) throw new IllegalArgumentException("catalog output conflict");
                        state.existing=text;
                    }
                }
                for (var implementation:state.implementations) for (String target:implementation.interfaceNames()) {
                    var declaration=state.interfaces.get(target);
                    if (declaration!=null) state.catalog.relate(implementation.node(),declaration,"IMPLEMENTS");
                }
                String sql=state.catalog.sql();
                if (SealHash.utf8(sql).length>256*1024*1024) throw new IllegalArgumentException("SQL byte bound exceeded");
                if (state.existing!=null && !state.existing.equals(sql)) throw new IllegalArgumentException("existing catalog differs; use a fresh output path");
                state.exported=true;
                return state.existing==null ? List.of(PlainText.builder().sourcePath(Path.of(outputPath)).text(sql).build()) : List.of();
            } catch (RuntimeException failure) {
                state.refused=true;
                context.getOnError().accept(failure);
                return List.of();
            }
        }
    }
    private static void checkCanceled(ExecutionContext context) {
        BooleanSupplier canceled=context.getMessage(CANCELLATION);
        if (canceled!=null && canceled.getAsBoolean()) throw new IllegalStateException("atom catalog canceled");
    }
    private static String safePath(String path) {
        if (path==null || path.isBlank() || path.startsWith("/") || path.indexOf(':')>=0
                || path.indexOf('\\')>=0 || path.indexOf('\0')>=0) throw new IllegalArgumentException("relative path required");
        for (String part:path.split("/",-1)) if (part.isEmpty() || part.equals(".") || part.equals("..")) {
            throw new IllegalArgumentException("normalized relative path required");
        }
        return path;
    }
    public static final class State {
        private final AtomCatalog catalog;
        private final Map<String,AtomCatalog.Node> interfaces=new HashMap<>();
        private final List<JavaAtomCatalogScanner.Implementation> implementations=new ArrayList<>();
        private String existing;
        private boolean refused, exported;
        private State(AtomCatalog catalog) { this.catalog=catalog; }
    }
}



