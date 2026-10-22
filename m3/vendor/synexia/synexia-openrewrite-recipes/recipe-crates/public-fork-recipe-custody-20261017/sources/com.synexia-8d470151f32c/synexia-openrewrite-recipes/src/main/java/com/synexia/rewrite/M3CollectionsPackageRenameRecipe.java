// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangePackage;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/** The admitted donor-family namespace mapping, delegated to the existing Java LST recipe. */
public final class M3CollectionsPackageRenameRecipe extends Recipe {
    @Option(displayName = "Collection family", description = "The inventoried donor family: apache, eclipse or guava.", example = "apache")
    private final String family;

    @JsonCreator
    public M3CollectionsPackageRenameRecipe(@JsonProperty("family") String family) {
        if (!List.of("apache", "eclipse", "guava").contains(family)) {
            throw new IllegalArgumentException("unreviewed collection family: " + family);
        }
        this.family = family;
    }

    public String getFamily() { return family; }

    @Override public String getDisplayName() { return "M3 collection family package and import rename"; }

    @Override public String getDescription() {
        return "Applies the admitted collection donor namespace mappings using OpenRewrite ChangePackage; metadata remains separately admitted.";
    }

    @Override public List<Recipe> getRecipeList() {
        return switch (family) {
            case "apache" -> List.of(new BoundaryCheckedChangePackage("org.apache.commons.collections4", "com.m3.collections.apache"));
            case "eclipse" -> List.of(new BoundaryCheckedChangePackage("org.eclipse.collections", "com.m3.collections.eclipse"));
            case "guava" -> List.of(new BoundaryCheckedChangePackage("com.google.common", "com.m3.collections.guava"),
                    new BoundaryCheckedChangePackage("com.google.thirdparty", "com.m3.collections.guava.thirdparty"));
            default -> throw new IllegalStateException("unreviewed family");
        };
    }

    /** 8.17.1 uses startsWith in its recursive predicate; reject ambiguous prefix neighbors. */
    private static final class BoundaryCheckedChangePackage extends Recipe {
        private final String oldPackage;
        private final String newPackage;

        private BoundaryCheckedChangePackage(String oldPackage, String newPackage) {
            this.oldPackage = oldPackage;
            this.newPackage = newPackage;
        }

        @Override public String getDisplayName() { return "Rename an admitted package family with exact boundaries"; }
        @Override public String getDescription() { return "Delegates to ChangePackage after refusing mixed prefix-neighbor package identities."; }

        @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new JavaIsoVisitor<ExecutionContext>() {
                @Override public J.CompilationUnit visitCompilationUnit(J.CompilationUnit unit, ExecutionContext ctx) {
                    List<String> packages = new ArrayList<>();
                    if (unit.getPackageDeclaration() != null) {
                        packages.add(unit.getPackageDeclaration().getExpression().printTrimmed(getCursor()).replaceAll("\\s", ""));
                    }
                    for (J.Import anImport : unit.getImports()) packages.add(anImport.getPackageName());
                    for (JavaType type : unit.getTypesInUse().getTypesInUse()) {
                        if (type instanceof JavaType.FullyQualified fq) packages.add(fq.getPackageName());
                    }
                    boolean admitted = packages.stream().anyMatch(p -> p.equals(oldPackage) || p.startsWith(oldPackage + "."));
                    if (!admitted) return unit;
                    for (String p : packages) {
                        if (p.startsWith(oldPackage) && !p.equals(oldPackage) && !p.startsWith(oldPackage + ".")) {
                            throw new IllegalArgumentException("Ambiguous prefix neighbor requires separate admission: " + p);
                        }
                    }
                    return (J.CompilationUnit) new ChangePackage(oldPackage, newPackage, true).getVisitor().visit(unit, ctx);
                }
            };
        }
    }
}
