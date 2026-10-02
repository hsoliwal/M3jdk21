// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * LIBRARY_API-scope non-mutating API surface inventory.
 *
 * <p>Public/protected declarations are admitted only when every owning type in the nesting chain is
 * itself exported. A public member inside a package-private outer type therefore cannot leak into
 * the library surface. Module and library roots are deterministic and source-order independent.</p>
 */
public final class M3LibraryApiSurfaceRecipe
        extends ScanningRecipe<M3LibraryApiSurfaceRecipe.Accumulator> {
    private transient ApiSurfaceTable surfaces = new ApiSurfaceTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory M3 library API surface";
    }

    @Override
    public String getDescription() {
        return "Computes deterministic public/protected module and library API roots without modifying source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "library-api",
                "public-api",
                "protected-api",
                "inventory",
                "admission",
                "non-mutating");
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator accumulator) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration declaration,
                    ExecutionContext ctx) {
                J.ClassDeclaration value =
                        super.visitClassDeclaration(declaration, ctx);
                JavaType.FullyQualified type = value.getType();
                if (type == null) return value;

                M3VisibilityLevel level = M3VisibilityLevel.of(value.getModifiers());
                String fqn = type.getFullyQualifiedName();
                if (level.librarySurface()) {
                    accumulator.exportedTypes.add(fqn);
                }
                if (level.librarySurface()) {
                    accumulator.candidates.add(
                            new Candidate(
                                    module(),
                                    ownerChain(type),
                                    "TYPE|" + fqn + "|" + level));
                }
                return value;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method,
                    ExecutionContext ctx) {
                J.MethodDeclaration value =
                        super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration owner =
                        getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (owner == null || owner.getType() == null) return value;

                M3VisibilityLevel level = M3VisibilityLevel.ofMember(
                        value.getModifiers(),
                        owner.getKind() == J.ClassDeclaration.Kind.Type.Interface);
                if (level.librarySurface()) {
                    accumulator.candidates.add(
                            new Candidate(
                                    module(),
                                    ownerChain(owner.getType()),
                                    "METHOD|" + owner.getType().getFullyQualifiedName() + "|"
                                            + signature(value) + "|" + level));
                }
                return value;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(
                    J.VariableDeclarations declarations,
                    ExecutionContext ctx) {
                J.VariableDeclarations value =
                        super.visitVariableDeclarations(declarations, ctx);
                if (getCursor().firstEnclosing(J.MethodDeclaration.class) != null) {
                    return value;
                }
                J.ClassDeclaration owner =
                        getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (owner == null || owner.getType() == null) return value;

                M3VisibilityLevel level = M3VisibilityLevel.ofMember(
                        value.getModifiers(),
                        owner.getKind() == J.ClassDeclaration.Kind.Type.Interface);
                if (level.librarySurface()) {
                    List<String> chain = ownerChain(owner.getType());
                    for (J.VariableDeclarations.NamedVariable variable : value.getVariables()) {
                        accumulator.candidates.add(
                                new Candidate(
                                        module(),
                                        chain,
                                        "FIELD|" + owner.getType().getFullyQualifiedName() + "|"
                                                + variable.getSimpleName() + "|" + level));
                    }
                }
                return value;
            }

            private String module() {
                J.CompilationUnit unit =
                        getCursor().firstEnclosing(J.CompilationUnit.class);
                if (unit == null) {
                    throw new IllegalStateException("API declaration without compilation unit");
                }
                return moduleName(unit.getSourcePath());
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(
            Accumulator accumulator,
            ExecutionContext ctx) {
        Map<String, TreeSet<String>> modules = new HashMap<>();
        for (Candidate candidate : accumulator.candidates) {
            if (candidate.ownerChain().stream()
                    .allMatch(accumulator.exportedTypes::contains)) {
                modules.computeIfAbsent(candidate.module(), ignored -> new TreeSet<>())
                        .add(candidate.declaration());
            }
        }

        ArrayList<String> libraryComponents = new ArrayList<>();
        modules.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<String> declarations = List.copyOf(entry.getValue());
                    String root = surfaceRoot(
                            "MODULE_API:" + entry.getKey(),
                            declarations);
                    libraryComponents.add(entry.getKey() + ":" + root);
                    surfaces.insertRow(
                            ctx,
                            new Row(
                                    "MODULE",
                                    entry.getKey(),
                                    declarations.size(),
                                    root,
                                    "M3:MODULE_API_SURFACE"));
                });

        String libraryRoot = surfaceRoot("LIBRARY_API", libraryComponents);
        int symbols = modules.values().stream().mapToInt(Set::size).sum();
        surfaces.insertRow(
                ctx,
                new Row(
                        "LIBRARY",
                        "<library>",
                        symbols,
                        libraryRoot,
                        "M3:LIBRARY_API_SURFACE"));
        return List.of();
    }

    private static String surfaceRoot(String domain, List<String> values) {
        return M3IndexDbSemanticFingerprint.leaf(
                        domain,
                        List.of("COUNT:" + values.size()),
                        values,
                        String.join("\n", values))
                .logicSha256();
    }

    private static List<String> ownerChain(JavaType.FullyQualified type) {
        ArrayList<String> chain = new ArrayList<>();
        for (JavaType.FullyQualified current = type;
                current != null;
                current = current.getOwningClass()) {
            chain.add(current.getFullyQualifiedName());
        }
        return List.copyOf(chain);
    }

    private static String signature(J.MethodDeclaration method) {
        ArrayList<String> parameters = new ArrayList<>();
        for (J parameter : method.getParameters()) {
            if (parameter instanceof J.VariableDeclarations declarations
                    && declarations.getTypeExpression() != null) {
                @SuppressWarnings("deprecation")
                String type = declarations.getTypeExpression().printTrimmed();
                parameters.add(type);
            } else {
                parameters.add("?");
            }
        }
        return method.getSimpleName() + "(" + String.join(",", parameters) + ")";
    }

    private static String moduleName(Path sourcePath) {
        String path = sourcePath.toString().replace('\\', '/');
        if (path.startsWith("src/")) {
            String[] parts = path.split("/");
            return parts.length > 1 ? parts[1] : "<root>";
        }
        int marker = path.indexOf("/src/main/java/");
        if (marker > 0) {
            String prefix = path.substring(0, marker);
            int slash = prefix.lastIndexOf('/');
            return slash < 0 ? prefix : prefix.substring(slash + 1);
        }
        return "<root>";
    }

    static final class Accumulator {
        private final TreeSet<String> exportedTypes = new TreeSet<>();
        private final List<Candidate> candidates = new ArrayList<>();
    }

    private record Candidate(
            String module,
            List<String> ownerChain,
            String declaration) {
        private Candidate {
            ownerChain = List.copyOf(ownerChain);
        }
    }

    public static final class ApiSurfaceTable extends DataTable<Row> {
        ApiSurfaceTable(ScanningRecipe<?> recipe) {
            super(
                    recipe,
                    "M3 module/library API surface roots",
                    "Deterministic public/protected API roots for library admission.");
        }
    }

    public static final class Row {
        @Column(displayName = "Scope kind", description = "MODULE or LIBRARY.")
        private final String scopeKind;
        @Column(displayName = "Scope name", description = "Module/library scope name.")
        private final String scopeName;
        @Column(displayName = "Symbol count", description = "Exported declarations in this scope.")
        private final int symbolCount;
        @Column(displayName = "API root", description = "Deterministic API surface SHA-256.")
        private final String apiRoot;
        @Column(displayName = "Pattern role", description = "M3 API-surface semantic role.")
        private final String patternRole;

        Row(
                String scopeKind,
                String scopeName,
                int symbolCount,
                String apiRoot,
                String patternRole) {
            this.scopeKind = scopeKind;
            this.scopeName = scopeName;
            this.symbolCount = symbolCount;
            this.apiRoot = apiRoot;
            this.patternRole = patternRole;
        }

        public String scopeKind() { return scopeKind; }
        public String scopeName() { return scopeName; }
        public int symbolCount() { return symbolCount; }
        public String apiRoot() { return apiRoot; }
        public String patternRole() { return patternRole; }
    }
}
