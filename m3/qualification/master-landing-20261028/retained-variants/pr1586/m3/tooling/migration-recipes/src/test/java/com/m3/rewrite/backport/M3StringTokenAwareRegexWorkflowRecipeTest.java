// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3StringTokenAwareRegexWorkflowRecipeTest {
    private static final String FORWARD = "m3-string-token-aware-regex-workflow-20261020";
    private static final String INVERSE = FORWARD + "-inverse";
    private static final String PATH = ".github/workflows/mindex-string-backing.yml";

    @Test
    void forwardInverseAndFixedPointsAreExact() {
        PlainText before = source(INVERSE);
        PlainText after = source(FORWARD);
        var forward = new M3Jdk21HashPinnedTextSnapshotRecipe(FORWARD);
        var applied = forward.run(new InMemoryLargeSourceSet(List.of(before)), context(), 1);
        assertEquals(1, applied.getChangeset().getAllResults().size());
        assertEquals(after.getText(),
                ((PlainText) applied.getChangeset().getAllResults().getFirst().getAfter()).getText());
        assertTrue(forward.run(new InMemoryLargeSourceSet(List.of(after)), context(), 1)
                .getChangeset().getAllResults().isEmpty());

        var inverse = new M3Jdk21HashPinnedTextSnapshotRecipe(INVERSE);
        var rolled = inverse.run(new InMemoryLargeSourceSet(List.of(after)), context(), 1);
        assertEquals(1, rolled.getChangeset().getAllResults().size());
        assertEquals(before.getText(),
                ((PlainText) rolled.getChangeset().getAllResults().getFirst().getAfter()).getText());
        assertTrue(inverse.run(new InMemoryLargeSourceSet(List.of(before)), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void driftRefuses() {
        PlainText before = source(INVERSE);
        PlainText drift = before.withText(before.getText() + "\n# deliberate drift\n");
        var recipe = new M3Jdk21HashPinnedTextSnapshotRecipe(FORWARD);
        assertThrows(RuntimeException.class,
                () -> recipe.run(new InMemoryLargeSourceSet(List.of(drift)), context(), 1));
    }

    private static PlainText source(String crate) {
        String text = resource(crate, "workflow.yml.txt");
        return PlainText.builder().sourcePath(Path.of(PATH)).text(text).build();
    }

    private static String resource(String crate, String file) {
        String path = "/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + crate + "/" + file;
        try (var stream =
                M3StringTokenAwareRegexWorkflowRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }
}
