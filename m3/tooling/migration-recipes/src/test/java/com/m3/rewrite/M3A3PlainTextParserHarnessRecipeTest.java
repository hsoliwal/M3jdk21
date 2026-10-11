// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3A3PlainTextParserHarnessRecipeTest {
    private static final String PATH =
            "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/M3SynexiaFullDeliveryA3RecipeTest.java";
    private static final String ROOT =
            "/com/m3/rewrite/backport/jdk21-hash-pinned/a3-plaintext-parser-harness/";

    @Test
    void generatedHarnessUsesPlainTextParserForMarkdownAndIsFixedPoint() {
        String before = resource("before-M3SynexiaFullDeliveryA3RecipeTest.java.txt");
        String expected = resource("M3SynexiaFullDeliveryA3RecipeTest.java.txt");
        assertEquals(Map.of(PATH, expected), apply(before));
        assertEquals(Map.of(), apply(expected));
        assertThrows(RuntimeException.class,
                () -> apply(before.replace("JavaParser.fromJavaVersion()", "JavaParser.fromJavaVersion() /* drift */")));
    }

    private static Map<String, String> apply(String source) {
        ExecutionContext context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        SourceFile parsed = JavaParser.fromJavaVersion().build()
                .parseInputs(List.of(Parser.Input.fromString(Path.of(PATH), source)), null, context)
                .findFirst().orElseThrow();
        var run = new M3Jdk21HashPinnedSnapshotRecipe("a3-plaintext-parser-harness")
                .run(new InMemoryLargeSourceSet(List.of(parsed)), context, 8);
        var output = new java.util.LinkedHashMap<String, String>();
        run.getChangeset().getAllResults().forEach(change ->
                output.put(change.getAfter().getSourcePath().toString(), change.getAfter().printAll()));
        return output;
    }

    private static String resource(String name) {
        try (var input = M3A3PlainTextParserHarnessRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing resource: " + name);
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read resource: " + name, e);
        }
    }
}
