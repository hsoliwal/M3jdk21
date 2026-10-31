// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainText;

/** Actual existing recipe execution; DB runtime and coverage are independent gates. */
final class DbRecipeTest {
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-java/db-parse/";
    private static final String CODEC = "src/main/java/com/m3/indexdb/M3IndexDbSemanticCodec.java";
    private static final List<String> PATHS = List.of(CODEC,
            "src/test/java/com/m3/indexdb/DbParseTest.java",
            "src/test/java/com/m3/indexdb/DbProbe.java");
    private static final List<String> FILES =
            List.of("00-Codec.java.txt", "01-DbParseTest.java.txt", "02-DbProbe.java.txt");

    @Test
    void parseRepairGeneratesExactTestsAndReachesFixedPoint() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("db-parse");
        var results = recipe.run(new InMemoryLargeSourceSet(before()), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(3, results.size());
        var after = new TreeMap<String, SourceFile>();
        results.forEach(result -> {
            SourceFile file = result.getAfter();
            assertNotNull(file);
            assertInstanceOf(J.CompilationUnit.class, file);
            after.put(file.getSourcePath().toString().replace('\\', '/'), file);
        });
        for (int i = 0; i < PATHS.size(); i++) {
            assertEquals(resource(FILES.get(i)), after.get(PATHS.get(i)).printAll());
        }
        assertTrue(recipe.run(new InMemoryLargeSourceSet(new ArrayList<>(after.values())),
                context(), 1).getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftMissingDuplicateAndWrongTreeAreRefused() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("db-parse");
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults());
        SourceFile original = before().getFirst();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(original, original)), context(), 1)
                .getChangeset().getAllResults());
        var text = PlainText.builder().sourcePath(Path.of(CODEC))
                .text(resource("before-00-Codec.java.txt")).build();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(text)), context(), 1)
                .getChangeset().getAllResults());
        var changed = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(CODEC),
                        resource("before-00-Codec.java.txt") + "\n// drift\n")),
                null, context()).toList();
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(changed), context(), 1)
                .getChangeset().getAllResults());
    }

    @Test
    void unrelatedSourceIsNotReplacedOrDeleted() {
        List<SourceFile> inputs = new ArrayList<>(before());
        var unrelated = PlainText.builder().sourcePath(Path.of("README.md")).text("keep\n").build();
        inputs.add(unrelated);
        var results = new M3HashPinnedJavaSnapshotRecipe("db-parse")
                .run(new InMemoryLargeSourceSet(inputs), context(), 1).getChangeset().getAllResults();
        assertEquals(3, results.size());
        assertTrue(results.stream().noneMatch(result -> result.getBefore() == unrelated));
    }

    @Test
    void fullRecoveryAndIndividualRepairsComposeToAStableModule() {
        var restore = new M3HashPinnedJavaSnapshotRecipe("db-java");
        var results = restore.run(new InMemoryLargeSourceSet(List.of()), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(18, results.size());
        List<SourceFile> after = results.stream().map(result -> result.getAfter()).toList();
        after.forEach(file -> assertInstanceOf(J.CompilationUnit.class, file));
        assertTrue(restore.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        for (String crate : List.of("db-parse", "db-check")) {
            assertTrue(new M3HashPinnedJavaSnapshotRecipe(crate)
                    .run(new InMemoryLargeSourceSet(after), context(), 1)
                    .getChangeset().getAllResults().isEmpty());
        }
    }

    @Test
    void staleParentExpectationHasAnExactIndependentRepair() {
        String root = "/com/synexia/rewrite/hash-pinned-java/db-check/";
        String path = "src/test/java/com/m3/indexdb/M3IndexDbSemanticFailClosedTest.java";
        var before = JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(path), resourceAt(root + "before-Test.java.txt"))),
                null, context()).toList();
        var recipe = new M3HashPinnedJavaSnapshotRecipe("db-check");
        var results = recipe.run(new InMemoryLargeSourceSet(before), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(1, results.size());
        SourceFile after = results.getFirst().getAfter();
        assertNotNull(after);
        assertEquals(resourceAt(root + "00-Test.java.txt"), after.printAll());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void metadataRecoveryPreservesTheReactorAndRejectsUnreviewedPoms() {
        String root = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/db-meta/";
        var before = PlainText.builder().sourcePath(Path.of("m3/pom.xml"))
                .text(resourceAt(root + "before-pom.xml.txt")).build();
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe("db-meta");
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(5, results.size());
        List<SourceFile> after = results.stream().map(result -> result.getAfter()).toList();
        after.forEach(file -> assertInstanceOf(PlainText.class, file));
        assertTrue(recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
        var drift = before.withText(before.getText() + "<!-- drift -->");
        assertThrows(RuntimeException.class, () -> recipe.run(
                new InMemoryLargeSourceSet(List.of(drift)), context(), 1)
                .getChangeset().getAllResults());
    }

    private static List<SourceFile> before() {
        return JavaParser.fromJavaVersion().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(CODEC),
                        resource("before-00-Codec.java.txt"))), null, context()).toList();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(failure -> { throw new IllegalStateException(failure); });
    }

    private static String resource(String name) {
        return resourceAt(ROOT + name);
    }

    private static String resourceAt(String name) {
        try (var input = DbRecipeTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
