// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

/**
 * Non-mutating visibility inventory used before any accessibility-changing refactor.
 *
 * <p>Each source file can be scanned independently. The recipe records exactly which declarations
 * remain FILE-local and which declarations cross into PACKAGE/PROTECTED/PUBLIC authority.</p>
 */
public final class M3VisibilityInventoryRecipe extends Recipe {
    private transient VisibilityTable visibility = new VisibilityTable(this);

    @Override
    public String getDisplayName() {
        return "Inventory M3 Java visibility boundaries";
    }

    @Override
    public String getDescription() {
        return "Emits class, method and field visibility boundaries without changing source.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "visibility",
                "inventory",
                "scope-aware",
                "non-mutating",
                "file-local-analysis");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration declaration,
                    ExecutionContext ctx) {
                J.ClassDeclaration value = super.visitClassDeclaration(declaration, ctx);
                M3VisibilityLevel level = M3VisibilityLevel.of(value.getModifiers());
                emit(
                        ctx,
                        sourcePath(),
                        ownerName(value),
                        "TYPE",
                        value.getSimpleName(),
                        level);
                return value;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(
                    J.MethodDeclaration method,
                    ExecutionContext ctx) {
                J.MethodDeclaration value = super.visitMethodDeclaration(method, ctx);
                J.ClassDeclaration owner =
                        getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (owner != null) {
                    emit(
                            ctx,
                            sourcePath(),
                            ownerName(owner),
                            "METHOD",
                            signature(value),
                            M3VisibilityLevel.ofMember(
                                    value.getModifiers(),
                                    owner.getKind() == J.ClassDeclaration.Kind.Type.Interface));
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

                M3VisibilityLevel level = M3VisibilityLevel.ofMember(
                        value.getModifiers(),
                        owner.getKind() == J.ClassDeclaration.Kind.Type.Interface);
                for (J.VariableDeclarations.NamedVariable variable : value.getVariables()) {
                    emit(
                            ctx,
                            sourcePath(),
                            ownerName(owner),
                            "FIELD",
                            variable.getSimpleName(),
                            level);
                }
                return value;
            }

            private String sourcePath() {
                J.CompilationUnit unit =
                        getCursor().firstEnclosing(J.CompilationUnit.class);
                return unit == null ? "" : normalized(unit.getSourcePath());
            }
        };
    }

    private void emit(
            ExecutionContext ctx,
            String sourcePath,
            String owner,
            String declarationKind,
            String symbol,
            M3VisibilityLevel level) {
        visibility.insertRow(
                ctx,
                new Row(
                        sourcePath,
                        owner,
                        declarationKind,
                        symbol,
                        level.name(),
                        level.escapesFile(),
                        level.librarySurface(),
                        "M3:VISIBILITY:" + level.name()));
    }

    private static String ownerName(J.ClassDeclaration declaration) {
        return declaration.getType() == null
                ? declaration.getSimpleName()
                : declaration.getType().getFullyQualifiedName();
    }

    private static String signature(J.MethodDeclaration method) {
        List<String> parameters = new ArrayList<>();
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

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    public static final class VisibilityTable extends DataTable<Row> {
        VisibilityTable(Recipe recipe) {
            super(
                    recipe,
                    "M3 Java visibility inventory",
                    "Declaration visibility and the narrowest boundary it can escape.");
        }
    }

    public static final class Row {
        @Column(displayName = "Source path", description = "Repository-relative source path.")
        private final String sourcePath;
        @Column(displayName = "Owner", description = "Owning Java type.")
        private final String owner;
        @Column(displayName = "Declaration kind", description = "TYPE, METHOD or FIELD.")
        private final String declarationKind;
        @Column(displayName = "Symbol", description = "Type/member symbol.")
        private final String symbol;
        @Column(displayName = "Visibility", description = "PRIVATE, PACKAGE, PROTECTED or PUBLIC.")
        private final String visibility;
        @Column(displayName = "Escapes FILE", description = "Whether the declaration is visible outside its declaring file/type boundary.")
        private final boolean escapesFile;
        @Column(displayName = "Library surface", description = "Whether the declaration is public/protected library surface.")
        private final boolean librarySurface;
        @Column(displayName = "Pattern role", description = "M3 semantic visibility role.")
        private final String patternRole;

        Row(
                String sourcePath,
                String owner,
                String declarationKind,
                String symbol,
                String visibility,
                boolean escapesFile,
                boolean librarySurface,
                String patternRole) {
            this.sourcePath = sourcePath;
            this.owner = owner;
            this.declarationKind = declarationKind;
            this.symbol = symbol;
            this.visibility = visibility;
            this.escapesFile = escapesFile;
            this.librarySurface = librarySurface;
            this.patternRole = patternRole;
        }

        public String sourcePath() { return sourcePath; }
        public String owner() { return owner; }
        public String declarationKind() { return declarationKind; }
        public String symbol() { return symbol; }
        public String visibility() { return visibility; }
        public boolean escapesFile() { return escapesFile; }
        public boolean librarySurface() { return librarySurface; }
        public String patternRole() { return patternRole; }
    }
}
