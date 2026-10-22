// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.List;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.style.ImportLayoutStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.style.NamedStyles;

/** Restores the donor's existing Checkstyle import order after the package identity changes. */
public final class M3CollectionsPackageRenameImportOrderRecipe extends Recipe {
    @Override public String getDisplayName() { return "Order renamed Eclipse imports by the existing donor policy"; }

    @Override public String getDescription() {
        return "Delegates to OrderImports with java, javax and other groups, followed by static imports; retains unused imports and prevents new star folding.";
    }

    @Override public TreeVisitor<?, ExecutionContext> getVisitor() {
        var layout = ImportLayoutStyle.builder()
                .classCountToUseStarImport(Integer.MAX_VALUE)
                .nameCountToUseStarImport(Integer.MAX_VALUE)
                .importPackage("java.*").blankLine()
                .importPackage("javax.*").blankLine()
                .importAllOthers().blankLine()
                .importStaticAllOthers().build();
        var style = new NamedStyles(Tree.randomId(), "M3EclipseExistingImportOrder",
                "Eclipse existing import order", "Pinned checkstyle-configuration.xml ImportOrder policy",
                Set.of(), List.of(layout));
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override public J.CompilationUnit visitCompilationUnit(J.CompilationUnit unit, ExecutionContext context) {
                var styled = unit.withMarkers(unit.getMarkers().removeByType(NamedStyles.class).add(style));
                var ordered = J.CompilationUnit.class.cast(orderImports().getVisitor().visit(styled, context));
                if (ordered == null) throw new IllegalStateException("Import ordering deleted a compilation unit");
                if (ordered.printAll().equals(unit.printAll())) return unit;
                return ordered.withMarkers(unit.getMarkers());
            }
        };
    }

    private static OrderImports orderImports() {
        try {
            try {
                return OrderImports.class.getConstructor(Boolean.class).newInstance(false);
            } catch (NoSuchMethodException newerProvider) {
                return OrderImports.class.getConstructor(Boolean.class, String.class).newInstance(false, null);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported existing OrderImports provider API", failure);
        }
    }
}
