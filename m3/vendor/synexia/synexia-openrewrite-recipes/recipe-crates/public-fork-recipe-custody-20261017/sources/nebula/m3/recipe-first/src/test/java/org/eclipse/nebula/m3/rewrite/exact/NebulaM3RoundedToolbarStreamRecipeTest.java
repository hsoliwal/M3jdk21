// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class NebulaM3RoundedToolbarStreamRecipeTest {
    private static final String RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/rounded-toolbar-stream/RoundedToolbar.";

    @Test
    void sourceHashesAndPathsArePinned() throws Exception {
        var recipe = new NebulaM3RoundedToolbarStreamRecipe();
        assertEquals(recipe.expectedBeforeSha256(), NebulaM3ExactJavaSnapshotRecipe.sha256(source("before")));
        assertEquals(recipe.expectedAfterSha256(), NebulaM3ExactJavaSnapshotRecipe.sha256(source("after")));
        assertTrue(recipe.matches(Path.of(NebulaM3RoundedToolbarStreamRecipe.REPOSITORY_PATH)));
        assertTrue(recipe.matches(Path.of(NebulaM3RoundedToolbarStreamRecipe.MODULE_PATH)));
        assertFalse(recipe.matches(Path.of("elsewhere/RoundedToolbar.java")));
        assertEquals(1, recipe.maxCycles());
    }

    @Test
    void realFileReplaysToExactPostimageThenStops() throws Exception {
        var recipe = new NebulaM3RoundedToolbarStreamRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var before = parse(source("before"), recipe, context);
        var results = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context)
                .getChangeset().getAllResults();
        assertTrue(errors.isEmpty(), errors.toString());
        assertEquals(1, results.size());
        SourceFile after = results.getFirst().getAfter();
        assertNotNull(after);
        assertEquals(source("after"), after.printAll());
        assertEquals(before.getId(), after.getId());
        assertEquals(before.getSourcePath(), after.getSourcePath());
        assertEquals(before.getCharset(), after.getCharset());
        assertEquals(before.isCharsetBomMarked(), after.isCharsetBomMarked());
        assertTrue(recipe.run(new InMemoryLargeSourceSet(List.of(after)), context)
                .getChangeset().getAllResults().isEmpty());
        assertTrue(errors.isEmpty(), errors.toString());
        String output = System.getProperty("m3.stream.materialized");
        if (output != null && !output.isBlank()) Files.writeString(Path.of(output), after.printAll(), StandardCharsets.UTF_8);
    }

    @Test
    void sourceDriftRefusesBothLanesWithoutEditingThem() throws Exception {
        var recipe = new NebulaM3RoundedToolbarStreamRecipe();
        for (String lane : List.of("before", "after")) {
            var errors = new ArrayList<Throwable>();
            var context = new InMemoryExecutionContext(errors::add);
            var unit = parse(source(lane) + "\n// drift\n", recipe, context);
            Tree result = recipe.getVisitor().visit(unit, context);
            assertSame(unit, result);
            assertEquals(1, errors.size());
            assertTrue(errors.getFirst().getMessage().contains("preimage drift"));
        }
    }

    @Test
    void sameSpellingAtAnotherPathHasNoMutationAuthority() throws Exception {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        var parser = JavaParser.fromJavaVersion().build();
        var unit = parser.parseInputs(List.of(Parser.Input.fromString(
                Path.of("other/RoundedToolbar.java"), source("before"))), null, context).findFirst().orElseThrow();
        Tree result = new NebulaM3RoundedToolbarStreamRecipe().getVisitor().visit(unit, context);
        assertSame(unit, result);
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void officialYamlActivatesTheSingleExistingSnapshotOwner() throws Exception {
        String name = "/META-INF/rewrite/nebula-m3-rounded-toolbar-stream.yml";
        var url = getClass().getResource(name);
        assertNotNull(url);
        try (var input = url.openStream()) {
            var environment = Environment.builder().load(new YamlResourceLoader(input, url.toURI(), new java.util.Properties())).build();
            var recipe = environment.activateRecipes("org.eclipse.nebula.m3.RoundedToolbarStreamSegments");
            assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));
            assertEquals(1, recipe.getRecipeList().size());
            assertInstanceOf(NebulaM3RoundedToolbarStreamRecipe.class, recipe.getRecipeList().getFirst());
        }
    }

    private static SourceFile parse(String text, NebulaM3RoundedToolbarStreamRecipe recipe,
            InMemoryExecutionContext context) {
        return JavaParser.fromJavaVersion().build().parseInputs(List.of(Parser.Input.fromString(
                Path.of(NebulaM3RoundedToolbarStreamRecipe.REPOSITORY_PATH), text)), null, context)
                .findFirst().orElseThrow();
    }

    private static String source(String lane) throws IOException {
        try (var input = NebulaM3RoundedToolbarStreamRecipeTest.class.getResourceAsStream(RESOURCE + lane + ".java.txt")) {
            if (input == null) throw new IOException("missing exact source " + lane);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
