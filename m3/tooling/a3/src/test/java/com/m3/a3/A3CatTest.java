// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class A3CatTest {

    @TempDir
    Path root;

    @Test
    void cataloguePinsExecutableM3DagAndOfficialOpenRewriteEvidence() throws Exception {
        List<A3Cat.Row> rows = A3Cat.load();

        assertEquals(14, rows.size());

        A3Cat.Row convergence =
                A3Cat.require(rows, "com.m3.rewrite.M3Java21ConvergenceRecipe");
        assertEquals(A3Cat.Origin.M3, convergence.origin());
        assertEquals(A3Cat.Activation.ADMITTED, convergence.activation());
        assertTrue(convergence.executableByA3());
        assertEquals(
                "a7549d2f8c70728faff4f8c05baaf5f5193dee7a",
                convergence.revision());

        A3Cat.Row cleanup =
                A3Cat.require(rows, "org.openrewrite.java.RemoveUnusedImports");
        assertEquals(A3Cat.Origin.OPENREWRITE, cleanup.origin());
        assertEquals(A3Cat.Activation.OPT_IN, cleanup.activation());
        assertEquals("Apache-2.0", cleanup.license());
        assertFalse(cleanup.executableByA3());

        A3Cat.Row staticAnalysis =
                A3Cat.require(rows, "org.openrewrite.staticanalysis.CommonStaticAnalysis");
        assertEquals(A3Cat.Activation.REFERENCE_ONLY, staticAnalysis.activation());
        assertEquals("Moderne-Source-Available", staticAnalysis.license());

        A3Cat.Row migration =
                A3Cat.require(rows, "org.openrewrite.java.migrate.UpgradeToJava21");
        assertEquals(A3Cat.Activation.REFERENCE_ONLY, migration.activation());
        assertEquals("Moderne-Source-Available", migration.license());

        A3Cat.Row findRecipes =
                A3Cat.require(rows, "org.openrewrite.java.recipes.FindRecipes");
        assertEquals(A3Cat.Activation.REFERENCE_ONLY, findRecipes.activation());
        assertTrue(findRecipes.reference().contains("docs.openrewrite.org"));
    }

    @Test
    void rootIsStableIndependentOfCallerRowOrder() throws Exception {
        List<A3Cat.Row> rows = A3Cat.load();
        String first = A3Cat.root(rows);
        ArrayList<A3Cat.Row> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);

        assertEquals(first, A3Cat.root(reversed));
        assertTrue(first.matches("[0-9a-f]{64}"));
    }

    @Test
    void writesNormalizedCatalogueOnlyUnderM3Build() throws Exception {
        A3Cat.write(root, Path.of("m3/build/a3/catalogue.tsv"));

        Path output = root.resolve("m3/build/a3/catalogue.tsv");
        String tsv = Files.readString(output);
        assertTrue(
                tsv.startsWith(
                        "catalogRoot\torigin\tcategory\trecipe\trole\tactivation\tlicense"));
        assertTrue(tsv.contains("M3Java21ConvergenceRecipe"));
        assertTrue(tsv.contains("org.openrewrite.maven.AddPlugin"));
        assertTrue(tsv.contains("Moderne-Source-Available"));
        assertEquals(15, tsv.lines().count());

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Cat.write(root, Path.of("catalogue.tsv")));
    }

    @Test
    void sourceAvailableDonorCannotAcquireExecutionAuthority() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new A3Cat.Row(
                                A3Cat.Origin.OPENREWRITE,
                                "StaticAnalysis",
                                "example.Recipe",
                                "STATIC_ANALYSIS",
                                A3Cat.Activation.OPT_IN,
                                "Moderne-Source-Available",
                                "openrewrite/rewrite-static-analysis",
                                "eca2b4c1f8cef1c503b1259e95dc280e2942d08b",
                                "https://docs.openrewrite.org/recipes",
                                "must remain reference-only"));
    }
}
