// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class NebulaM3FinalTransferInventoryRootRecipeTest {
    private static final String BEFORE_RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/final-transfer-inventory-root/"
                    + "m3-final-proven-transfer.before.yml.txt";
    private static final String AFTER_RESOURCE =
            "/org/eclipse/nebula/m3/rewrite/exact/final-transfer-inventory-root/"
                    + "m3-final-proven-transfer.after.yml.txt";

    @Test
    void exactWorkflowPreimageMovesToIsolatedInventoryPropertyCustody() throws Exception {
        String before = resource(BEFORE_RESOURCE);
        String after = resource(AFTER_RESOURCE);
        var recipe = new NebulaM3FinalTransferInventoryRootRecipe();

        assertEquals(
                NebulaM3FinalTransferInventoryRootRecipe.BEFORE,
                NebulaM3ExactTextSnapshotRecipe.sha256(before));
        assertEquals(
                NebulaM3FinalTransferInventoryRootRecipe.AFTER,
                NebulaM3ExactTextSnapshotRecipe.sha256(after));
        assertTrue(before.contains("-Dm3.nebula.root=\"$GITHUB_WORKSPACE\""));
        assertFalse(after.contains("-Dm3.nebula.root=\"$GITHUB_WORKSPACE\""));
        assertTrue(after.contains("-Dm3.nebula.out=\"$GITHUB_WORKSPACE/"));

        Replay first = run(recipe, before);
        assertTrue(first.errors().isEmpty(), first.errors().toString());
        assertEquals(1, first.results().size());
        assertEquals(after, first.results().getFirst().getAfter().printAll());

        Replay second = run(recipe, after);
        assertTrue(second.errors().isEmpty(), second.errors().toString());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void staleWorkflowFailsClosed() throws Exception {
        String drift =
                resource(BEFORE_RESOURCE).replace(
                        "name: Nebula M3 final proven transfer",
                        "name: drift");

        Replay result =
                run(new NebulaM3FinalTransferInventoryRootRecipe(), drift);

        assertFalse(result.errors().isEmpty());
        assertTrue(result.results().isEmpty());
    }

    @Test
    void recipeOwnsOnlyTheFinalTransferWorkflow() {
        var recipe = new NebulaM3FinalTransferInventoryRootRecipe();

        assertTrue(recipe.matches(Path.of(NebulaM3FinalTransferInventoryRootRecipe.PATH)));
        assertFalse(recipe.matches(Path.of(".github/workflows/other.yml")));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    private static Replay run(
            NebulaM3FinalTransferInventoryRootRecipe recipe,
            String body) {
        PlainText source =
                PlainText.builder()
                        .sourcePath(Path.of(NebulaM3FinalTransferInventoryRootRecipe.PATH))
                        .text(body)
                        .build();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<Result> results =
                recipe.run(
                                new InMemoryLargeSourceSet(List.of(source)),
                                context)
                        .getChangeset()
                        .getAllResults();
        return new Replay(results, errors);
    }

    private static String resource(String name) throws IOException {
        try (var input =
                NebulaM3FinalTransferInventoryRootRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Replay(List<Result> results, List<Throwable> errors) {}
}
