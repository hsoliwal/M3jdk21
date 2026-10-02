// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import com.m3.indexdb.M3IndexDbSemanticFingerprint;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

/**
 * PACKAGE-scope non-mutating boundary inventory.
 *
 * <p>It groups package/protected declarations from independent files into a deterministic package
 * root. The result is proof input for deciding whether a refactor can remain package-local or must
 * promote to MODULE.</p>
 */
public final class M3PackageBoundaryRecipe
        extends ScanningRecipe<M3PackageBoundaryRecipe.Accumulator> {
    private transient PackageTable packages = new PackageTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory M3 package boundaries";
    }

    @Override
    public String getDescription() {
        return "Groups package/protected declaration surfaces across Java files into deterministic package roots.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "package",
                "visibility",
                "inventory",
                "scope-aware",
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
            public J.CompilationUnit visitCompilationUnit(
                    J.CompilationUnit unit,
                    ExecutionContext ctx) {
                String packageName = packageName(unit);
                PackageSurface surface =
                        accumulator.packages.computeIfAbsent(
                                packageName,
                                ignored -> new PackageSurface());
                surface.files.add(normalized(unit.getSourcePath()));
                return super.visitCompilationUnit(unit, ctx);
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration declaration,
                    ExecutionContext ctx) {
                J.ClassDeclaration value =
                        super.visitClassDeclaration(declaration, ctx);
                M3VisibilityLevel level = M3VisibilityLevel.of(value.getModifiers());
                if (level == M3VisibilityLevel.PACKAGE
                        || level == M3VisibilityLevel.PROTECTED) {
                    surface().declarations.add(
                            "TYPE|" + ownerName(value) + "|" + level);
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
                if (owner == null) return value;

                M3VisibilityLevel level = M3VisibilityLevel.of(value.getModifiers());
                if (level == M3VisibilityLevel.PACKAGE
                        || level == M3VisibilityLevel.PROTECTED) {
                    surface().declarations.add(
                            "METHOD|" + ownerName(owner) + "|"
                                    + value.getSimpleName() + "|" + level);
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
                if (owner == null) return value;
                M3VisibilityLevel level = M3VisibilityLevel.of(value.getModifiers());
                if (level == M3VisibilityLevel.PACKAGE
                        || level == M3VisibilityLevel.PROTECTED) {
                    for (J.VariableDeclarations.NamedVariable variable : value.getVariables()) {
                        surface().declarations.add(
                                "FIELD|" + ownerName(owner) + "|"
                                        + variable.getSimpleName() + "|" + level);
                    }
                }
                return value;
            }

            private PackageSurface surface() {
                J.CompilationUnit unit =
                        getCursor().firstEnclosing(J.CompilationUnit.class);
                if (unit == null) throw new IllegalStateException("Java declaration without compilation unit");
                return accumulator.packages.computeIfAbsent(
                        packageName(unit),
                        ignored -> new PackageSurface());
            }
        };
    }

    @Override
    public Collection<SourceFile> generate(
            Accumulator accumulator,
            ExecutionContext ctx) {
        accumulator.packages.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    PackageSurface surface = entry.getValue();
                    List<String> declarations = List.copyOf(surface.declarations);
                    String root = M3IndexDbSemanticFingerprint.leaf(
                                    "PACKAGE_BOUNDARY",
                                    List.of(
                                            "FILES:" + surface.files.size(),
                                            "DECLARATIONS:" + declarations.size()),
                                    declarations,
                                    String.join("\n", declarations))
                            .logicSha256();
                    packages.insertRow(
                            ctx,
                            new Row(
                                    entry.getKey(),
                                    surface.files.size(),
                                    declarations.size(),
                                    root,
                                    "M3:PACKAGE_BOUNDARY"));
                });
        return List.of();
    }

    private static String packageName(J.CompilationUnit unit) {
        if (unit.getPackageDeclaration() == null) return "<default>";
        @SuppressWarnings("deprecation")
        String value = unit.getPackageDeclaration().getExpression().printTrimmed();
        return value.isBlank() ? "<default>" : value;
    }

    private static String ownerName(J.ClassDeclaration declaration) {
        return declaration.getType() == null
                ? declaration.getSimpleName()
                : declaration.getType().getFullyQualifiedName();
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    static final class Accumulator {
        private final Map<String, PackageSurface> packages = new HashMap<>();
    }

    private static final class PackageSurface {
        private final TreeSet<String> files = new TreeSet<>();
        private final TreeSet<String> declarations = new TreeSet<>();
    }

    public static final class PackageTable extends DataTable<Row> {
        PackageTable(ScanningRecipe<?> recipe) {
            super(
                    recipe,
                    "M3 package boundary roots",
                    "Deterministic package/protected declaration surface roots.");
        }
    }

    public static final class Row {
        @Column(displayName = "Package", description = "Java package name.")
        private final String packageName;
        @Column(displayName = "File count", description = "Files observed in the package.")
        private final int fileCount;
        @Column(displayName = "Boundary declaration count", description = "Package/protected declarations in the package.")
        private final int declarationCount;
        @Column(displayName = "Boundary root", description = "Deterministic normalized package boundary SHA-256.")
        private final String boundaryRoot;
        @Column(displayName = "Pattern role", description = "M3 package-boundary semantic role.")
        private final String patternRole;

        Row(
                String packageName,
                int fileCount,
                int declarationCount,
                String boundaryRoot,
                String patternRole) {
            this.packageName = packageName;
            this.fileCount = fileCount;
            this.declarationCount = declarationCount;
            this.boundaryRoot = boundaryRoot;
            this.patternRole = patternRole;
        }

        public String packageName() { return packageName; }
        public int fileCount() { return fileCount; }
        public int declarationCount() { return declarationCount; }
        public String boundaryRoot() { return boundaryRoot; }
        public String patternRole() { return patternRole; }
    }
}
