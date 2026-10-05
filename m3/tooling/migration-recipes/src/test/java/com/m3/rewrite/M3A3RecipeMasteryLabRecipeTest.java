// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

class M3A3RecipeMasteryLabRecipeTest {
    private static final String LAB =
            "src/test/java/com/m3/rewrite/M3RecipeMasteryLab.java";
    private static final String TEST =
            "src/test/java/com/m3/rewrite/M3Java21RecipeMasteryLabTest.java";

    @Test
    void generatesExactReviewedLabAndReachesSecondRunFixedPoint() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);

        var first =
                new M3A3RecipeMasteryLabRecipe()
                        .run(new InMemoryLargeSourceSet(List.of()), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());

        List<SourceFile> generated =
                changes.stream().map(change -> change.getAfter()).toList();
        assertEquals(
                M3A3RecipeMasteryLabRecipe.resourceText(LAB),
                require(generated, LAB).printAll());
        assertEquals(
                M3A3RecipeMasteryLabRecipe.resourceText(TEST),
                require(generated, TEST).printAll());

        var replay =
                new M3A3RecipeMasteryLabRecipe()
                        .run(new InMemoryLargeSourceSet(generated), context);
        assertTrue(errors.isEmpty(), errors.toString());
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void reviewedResourcesRoundTripAsJava21CompilationUnits() {
        var context = new InMemoryExecutionContext();
        for (String path : List.of(LAB, TEST)) {
            String source = M3A3RecipeMasteryLabRecipe.resourceText(path);
            try (var parsed =
                    JavaParser.fromJavaVersion()
                            .build()
                            .parseInputs(
                                    List.of(
                                            Parser.Input.fromString(
                                                    Path.of(path), source)),
                                    null,
                                    context)) {
                List<SourceFile> files = parsed.toList();
                assertEquals(1, files.size());
                assertEquals(source, files.getFirst().printAll());
            }
        }
    }

    @Test
    void driftFailsClosedAndRecipeHasNoProductAuthority() {
        var context =
                new InMemoryExecutionContext(
                        failure -> {
                            throw new AssertionError(failure);
                        });
        SourceFile drift =
                parse(
                        LAB,
                        """
                        package com.m3.rewrite;
                        final class M3RecipeMasteryLab {}
                        """,
                        context);

        assertThrows(
                RuntimeException.class,
                () ->
                        new M3A3RecipeMasteryLabRecipe()
                                .run(
                                        new InMemoryLargeSourceSet(List.of(drift)),
                                        context)
                                .getChangeset()
                                .getAllResults());
        assertFalse(new M3A3RecipeMasteryLabRecipe().productMutationAuthority());
        assertFalse(new M3A3RecipeMasteryLabRecipe().promotionAuthority());
    }

    private static SourceFile require(List<SourceFile> files, String path) {
        return files.stream()
                .filter(file -> file.getSourcePath().toString().replace('\\', '/').equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static SourceFile parse(
            String path, String source, InMemoryExecutionContext context) {
        try (var parsed =
                JavaParser.fromJavaVersion()
                        .build()
                        .parseInputs(
                                List.of(Parser.Input.fromString(Path.of(path), source)),
                                null,
                                context)) {
            return parsed.findFirst().orElseThrow();
        }
    }
}
