// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class NebulaM3TransferContractNulRecipeTest {
    private static final String ROOT =
            "/org/eclipse/nebula/m3/rewrite/exact/transfer-contract-nul/";

    @Test
    void exactMalformedPreimageMovesToNulFreeEquivalentAndFixedPoint() throws Exception {
        String before = resource("NebulaM3TransferContract.before.java.txt");
        String after = resource("NebulaM3TransferContract.after.java.txt");
        var recipe = new NebulaM3TransferContractNulRecipe();

        assertEquals(NebulaM3TransferContractNulRecipe.BEFORE,
                NebulaM3ExactTextSnapshotRecipe.sha256(before));
        assertEquals(NebulaM3TransferContractNulRecipe.AFTER,
                NebulaM3ExactTextSnapshotRecipe.sha256(after));
        assertTrue(before.indexOf(0) >= 0, "fixture must retain the malformed NUL byte");
        assertEquals(-1, after.indexOf(0), "postimage must be raw-source NUL free");
        assertTrue(after.contains("checked.indexOf(0) >= 0"));

        Replay first = run(recipe, before);
        assertTrue(first.errors().isEmpty(), first.errors().toString());
        assertEquals(1, first.results().size());
        assertEquals(after, first.results().getFirst().getAfter().printAll());

        Replay second = run(recipe, after);
        assertTrue(second.errors().isEmpty(), second.errors().toString());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void unknownTransferContractSourceDriftFailsClosed() throws Exception {
        String drift = resource("NebulaM3TransferContract.before.java.txt") + "// drift\n";
        Replay result = run(new NebulaM3TransferContractNulRecipe(), drift);
        assertFalse(result.errors().isEmpty());
        assertTrue(result.results().isEmpty());
    }

    @Test
    void recipeOwnsOnlyTheTransferContractSource() {
        var recipe = new NebulaM3TransferContractNulRecipe();
        assertTrue(recipe.matches(Path.of(NebulaM3TransferContractNulRecipe.PATH)));
        assertFalse(recipe.matches(Path.of("other/NebulaM3TransferContract.java")));
        assertEquals(1, recipe.maxCycles());
        assertTrue(recipe.getTags().contains("file-local"));
        assertTrue(recipe.getTags().contains("behavior-contract-preserving"));
    }

    private static Replay run(NebulaM3TransferContractNulRecipe recipe, String body) {
        PlainText source = PlainText.builder()
                .sourcePath(Path.of(NebulaM3TransferContractNulRecipe.PATH))
                .text(body)
                .build();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        List<Result> results = recipe.run(new InMemoryLargeSourceSet(List.of(source)), context)
                .getChangeset().getAllResults();
        return new Replay(results, errors);
    }

    private static String resource(String name) throws IOException {
        try (var input = NebulaM3TransferContractNulRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IOException("missing resource " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private record Replay(List<Result> results, List<Throwable> errors) {}
}
