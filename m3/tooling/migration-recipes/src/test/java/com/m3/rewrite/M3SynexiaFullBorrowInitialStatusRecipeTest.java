// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

final class M3SynexiaFullBorrowInitialStatusRecipeTest {
    private static final String PATH = "m3/synexia-import/current-full-borrow/receiver-status.tsv";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/"
            + "synexia-full-borrow-initial-status/";

    @Test
    void statusRecipePreservesSourceOnlyAndReachesFixedPoint() {
        String before = resource("before-receiver-status.tsv.txt");
        String expected = resource("receiver-status.tsv.txt");
        assertEquals(Map.of(PATH, expected), apply(before));
        assertTrue(expected.contains("MINDEX_STRING_RUNTIME\\t1840f12a3ea64750d829f6dd2bda7916fc25d9f5\\tSOURCE_PIN_ONLY\\tm3/ports/indexstring + java.lang.M3String* runtime adapter\\tNONE"));
        assertTrue(apply(expected).isEmpty());
        assertThrows(RuntimeException.class, () -> apply(before.replace("TARGET_MAPPING_PINNED", "SOURCE_PIN_ONLY")));
    }

    private static Map<String, String> apply(String source) {
        ExecutionContext context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        Parser.Input input = Parser.Input.fromString(Path.of(PATH), source);
        List<SourceFile> parsed = PlainTextParser.builder().build()
                .parseInputs(List.of(input), null, context).toList();
        var run = new M3Jdk21HashPinnedTextSnapshotRecipe("synexia-full-borrow-initial-status")
                .run(new InMemoryLargeSourceSet(parsed), context, 8);
        Map<String, String> output = new LinkedHashMap<>();
        run.getChangeset().getAllResults().forEach(change -> {
            SourceFile after = Objects.requireNonNull(change.getAfter(), "after");
            output.put(after.getSourcePath().toString().replace('\\\\', '/'), after.printAll());
        });
        return output;
    }

    private static String resource(String name) {
        try (var input = M3SynexiaFullBorrowInitialStatusRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing resource: " + name);
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read resource: " + name, e);
        }
    }
}
