// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.tree.ParseError;

/** Qualified source-layout extension; it does not claim full Java 25 parser support. */
final class M3JdtDomSourceLayoutTest {
    private static final String CRATE = "jdt-dom-layout-20261019";
    private static final String DOM = "org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/DomFixture.java";
    private static final String TEST = "org.eclipse.jdt.core.tests.model/src/org/eclipse/jdt/core/tests/dom/DomFixtureContract.java";

    @Test void preservesActualJdtModuleAndPackageScopes() {
        assertEquals(new M3ScopeInference.JavaLocation("org.eclipse.jdt.core", "org.eclipse.jdt.core.dom"),
                M3ScopeInference.describeJavaTarget(DOM));
        assertEquals(new M3ScopeInference.JavaLocation("org.eclipse.jdt.core.tests.model", "org.eclipse.jdt.core.tests.dom"),
                M3ScopeInference.describeJavaTarget(TEST));
        assertEquals(M3EditScope.MULTI_MODULE, new M3HashPinnedJavaSnapshotRecipe(CRATE).requiredScope());
        assertEquals(new M3ScopeInference.JavaLocation("org.eclipse.jdt.core", "org.eclipse.jdt.internal.core.builder"),
                M3ScopeInference.describeJavaTarget(
                        "org.eclipse.jdt.core/model/org/eclipse/jdt/internal/core/builder/JavaBuilder.java"));
    }

    @Test void runsTypedJdtLayoutFixturesAndReplaysWithoutChanges() throws Exception {
        var context = context();
        String before = text("pre-DomFixture.java.txt");
        SourceFile input = JavaParser.fromJavaVersion().build().parse(context, before).findFirst().orElseThrow()
                .withSourcePath(Path.of(DOM));
        assertInstanceOf(J.CompilationUnit.class, input);
        assertEquals(before, input.printAll());
        var recipe = new M3HashPinnedJavaSnapshotRecipe(CRATE);
        var changes = recipe.run(new InMemoryLargeSourceSet(List.of(input)), context, 1).getChangeset().getAllResults();
        assertEquals(2, changes.size());
        List<SourceFile> after = changes.stream().map(change -> change.getAfter()).toList();
        for (SourceFile source : after) {
            assertInstanceOf(J.CompilationUnit.class, source);
            assertEquals(text(source.getSourcePath().getFileName() + ".txt"), source.printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context, 1).getChangeset().getAllResults().isEmpty());
    }

    @Test void refusesUnreviewedSiblingPackagesAndPathEscapes() {
        for (String path : List.of(
                "org.eclipse.jdt.core/dom/org/eclipse/jdt/internal/DomFixture.java",
                "org.eclipse.jdt.core/unknown/org/eclipse/jdt/core/dom/DomFixture.java",
                "org.eclipse.jdt.core.tests.compiler/src/org/eclipse/jdt/core/tests/dom/DomFixture.java",
                "org.eclipse.jdt.core.tests.model/src/org/eclipse/jdt/core/tests/model/DomFixture.java",
                "org.eclipse.jdt.core/model/org/eclipse/jdt/internal/core/builder/OtherBuilder.java",
                "org.eclipse.jdt.core/dom/org/eclipse/jdt/core/dom/../DomFixture.java")) {
            assertThrows(IllegalArgumentException.class, () -> M3ScopeInference.describeJavaTarget(path), path);
        }
        for (int index = 0; index < 8; index++) {
            var recipe = new M3HashPinnedJavaSnapshotRecipe("jdt-dom-layout-refusal-" + index + "-20261019");
            assertThrows(IllegalStateException.class, recipe::targetPaths, "manifest refusal " + index);
        }
    }

    @Test void retainsUnsupportedPreimageWitnessAndRequiresTypedSuccessor() throws Exception {
        String path = "org.eclipse.jdt.core/model/org/eclipse/jdt/internal/core/builder/JavaBuilder.java";
        String root = "/com/synexia/rewrite/jdt-java21-bootstrap-20261019/";
        String before;
        String after;
        try (var pre = getClass().getResourceAsStream(root + "pre-JavaBuilder.java.txt");
             var post = getClass().getResourceAsStream(root + "JavaBuilder.java.txt")) {
            assertNotNull(pre);
            assertNotNull(post);
            before = new String(pre.readAllBytes(), StandardCharsets.UTF_8);
            after = new String(post.readAllBytes(), StandardCharsets.UTF_8);
        }
        SourceFile rejected = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), before)), null, context())
                .findFirst().orElseThrow();
        assertInstanceOf(ParseError.class, rejected);
        assertEquals(before, rejected.printAll(), "unsupported source remains intact as evidence");
        assertTrue(rejected.getMarkers().getMarkers().toString().contains("not print idempotent"));
        SourceFile successor = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(path), after)), null, context())
                .findFirst().orElseThrow();
        assertInstanceOf(J.CompilationUnit.class, successor);
        assertEquals(after, successor.printAll(), "sealed compatibility successor is typed and lossless");
        var unrelatedCrate = new M3HashPinnedTextSnapshotRecipe("jdt-java21-bootstrap-refusal-20261019");
        var failure = assertThrows(IllegalStateException.class, unrelatedCrate::targetPaths);
        assertTrue(failure.getMessage().contains("unauthorized JDT Java 21 syntax bootstrap"));
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String text(String file) throws Exception {
        try (var stream = M3JdtDomSourceLayoutTest.class.getResourceAsStream(
                "/com/synexia/rewrite/hash-pinned-java/" + CRATE + "/" + file)) {
            assertNotNull(stream, file);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
