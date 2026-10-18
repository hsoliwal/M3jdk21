// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.text.Assertions.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

/** Actual Java LST + plaintext Maven recipe replay; refuse source drift or unordered admission. */
final class M3FiveRealSdkFactories20261018RecipeTest implements RewriteTest {
    private static final String CRATE = "m3jdk21-five-real-sdk-factories-20261018";
    private static final String MAIN = "m3/collections/interop/src/main/java/com/m3/collections/interop/M3LibraryCollections.java";
    private static final String TEST = "m3/collections/interop/src/test/java/com/m3/collections/interop/M3FiveLibraryInteropContractTest.java";
    private static final String CENSUS = "m3/collections/interop/qualification/current-census-20261017/";
    private static final String POM = "m3/collections/qualification/m3-five-provider-current-census-20261017/recipe/pom.xml";
    private static final String DOC = "m3/collections/qualification/five-real-sdk-factories-20261018/";

    private static String resource(String kind, String name) throws IOException {
        String url = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/" + name + ".txt";
        try (InputStream input = M3FiveRealSdkFactories20261018RecipeTest.class.getResourceAsStream(url)) {
            if (input == null) throw new IOException("missing exact recipe source " + url);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void javaLstsAndSecondCycleFixedPoint() throws IOException {
        rewriteRun(spec -> spec.recipe(new M3HashPinnedJavaSnapshotRecipe(CRATE))
                    .cycles(2).expectedCyclesThatMakeChanges(1),
            java(resource("java", "pre/M3LibraryCollections.java"),
                 resource("java", "M3LibraryCollections.java"),
                 s -> s.path(MAIN).noTrim()),
            java(resource("java", "pre/M3FiveLibraryInteropContractTest.java"),
                 resource("java", "M3FiveLibraryInteropContractTest.java"),
                 s -> s.path(TEST).noTrim()));
    }

    @Test void exactMetadataAndMavenSourceSealFixedPoint() throws IOException {
        rewriteRun(spec -> spec.recipe(new M3HashPinnedTextSnapshotRecipe(CRATE))
                    .cycles(2).expectedCyclesThatMakeChanges(1),
            text(null, resource("text","m3-five-real-sdk-factories-20261018.yml"),
                 s -> s.path(".github/workflows/m3-five-real-sdk-factories-20261018.yml").noTrim()),
            text(resource("text","pre/current-source-census.tsv"),
                 resource("text","current-source-census.tsv"),
                 s -> s.path(CENSUS+"current-source-census.tsv").noTrim()),
            text(resource("text","pre/verify.py"),
                 resource("text","verify.py"),
                 s -> s.path(CENSUS+"verify.py").noTrim()),
            text(null, resource("text","CATALOGUE.tsv"),
                 s -> s.path(DOC+"CATALOGUE.tsv").noTrim()),
            text(null, resource("text","README.md"),
                 s -> s.path(DOC+"README.md").noTrim()),
            text(resource("text","pre/pom.xml"),
                 resource("text","pom.xml"),
                 s -> s.path(POM).noTrim()));
    }

    @Test void manifestDeclaresExactlyTwoJavaAndSixTextCandidates() {
        var javaRecipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        assertEquals(List.of(MAIN, TEST), javaRecipe.targetPaths());
        assertEquals("f93bc46a69911eb8a05b13b94bb4e4d77b42e2a534576e90c1243b7cbe779fb6",
                javaRecipe.preimageManifest().get(MAIN));
        var textRecipe = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(6, textRecipe.targetPaths().size());
        assertEquals("e9353beec98ea0b6a8ec78447146fae7f3095c852e23822d97ef7ab1a58b74c6",
                textRecipe.preimageManifest().get(POM));
    }

    @Test void refusesOccupiedOrDriftedExistingJavaWithoutOutput() {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        var units = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(MAIN),
                        "package com.m3.collections.interop; public final class M3LibraryCollections {}")),
                        null, context).toList();
        assertThrows(RuntimeException.class, () -> new M3HashPinnedJavaSnapshotRecipe(CRATE)
                .run(new InMemoryLargeSourceSet(units), context, 1));
    }
}
