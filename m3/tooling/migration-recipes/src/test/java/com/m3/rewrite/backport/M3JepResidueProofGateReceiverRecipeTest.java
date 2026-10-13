// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

/** JUnit acceptance gate for the exact Synexia-to-M3JDK21 text receiver. */
final class M3JepResidueProofGateReceiverRecipeTest {
    private static final String SOURCE = "m3/backports/jep_residue.py";
    private static final String TEST = "m3/backports/test_jep_residue.py";
    private static final String PRE =
            "/com/m3/rewrite/backport/jep-residue-proof/pre/";

    @Test
    void exactSynexiaPreimagesMaterializeAndReachFixedPoint() throws IOException {
        var wrapper = new M3JepResidueProofGateReceiverRecipe();
        assertEquals(1, wrapper.maxCycles());
        assertEquals(1, wrapper.getRecipeList().size());
        var receiver = (M3Jdk21HashPinnedTextSnapshotRecipe) wrapper.getRecipeList().getFirst();
        assertEquals("jdk21-jep-residue-proof", receiver.getCrateName());
        assertEquals(List.of(SOURCE, TEST), receiver.targetPaths());

        var ctx = context();
        var before = List.<SourceFile>of(
                file(SOURCE, resource(PRE + "jep_residue.py.txt")),
                file(TEST, resource(PRE + "test_jep_residue.py.txt")));
        var first = wrapper.run(new InMemoryLargeSourceSet(before), ctx, 1);
        var changes = first.getChangeset().getAllResults();
        assertEquals(2, changes.size());
        assertTrue(changes.stream().allMatch(change -> change.getBefore() != null));
        List<SourceFile> after = changes.stream().map(change -> {
            assertNotNull(change.getAfter());
            return change.getAfter();
        }).toList();
        assertTrue(wrapper.run(new InMemoryLargeSourceSet(after), context(), 1)
                .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleMissingAndUnrelatedInputAreNotPromoted() throws IOException {
        var wrapper = new M3JepResidueProofGateReceiverRecipe();
        var before = List.<SourceFile>of(
                file(SOURCE, resource(PRE + "jep_residue.py.txt")),
                file(TEST, resource(PRE + "test_jep_residue.py.txt")));
        var unrelated = file("m3/backports/README.unrelated", "unchanged\n");
        var passed = new ArrayList<SourceFile>(before);
        passed.add(unrelated);

        var changes = wrapper.run(new InMemoryLargeSourceSet(passed), context(), 1)
                .getChangeset().getAllResults();
        assertEquals(2, changes.size());
        assertTrue(changes.stream().noneMatch(result ->
                unrelated.getSourcePath().equals(result.getAfter().getSourcePath())));

        assertThrows(RuntimeException.class, () -> wrapper.run(
                new InMemoryLargeSourceSet(List.of(
                        file(SOURCE, resource(PRE + "jep_residue.py.txt") + "# drift\n"),
                        before.get(1))), context(), 1).getChangeset().getAllResults());
        assertThrows(RuntimeException.class, () -> wrapper.run(
                new InMemoryLargeSourceSet(List.of(before.getFirst())), context(), 1)
                .getChangeset().getAllResults());
    }

    private static PlainText file(String path, String text) {
        return PlainText.builder().sourcePath(Path.of(path)).text(text).build();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String resource(String path) throws IOException {
        try (var in = M3JepResidueProofGateReceiverRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing exact before-fixture: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
