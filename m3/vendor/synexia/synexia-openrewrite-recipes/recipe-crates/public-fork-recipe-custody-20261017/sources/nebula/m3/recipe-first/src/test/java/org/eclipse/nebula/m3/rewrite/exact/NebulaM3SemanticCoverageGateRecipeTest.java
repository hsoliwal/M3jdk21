// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/** Exact Maven XML transformation, scope, JUnit proof and second-pass fixed-point tests. */
final class NebulaM3SemanticCoverageGateRecipeTest {
    @Test
    void pinnedPolicyRequiresBothSemanticCoverageCounters() {
        String before = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.PREIMAGE);
        String after = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.POSTIMAGE);

        assertEquals(
                NebulaM3SemanticCoverageGateRecipe.BEFORE_SHA256,
                NebulaM3SemanticCoverageGateRecipe.sha256(before));
        assertEquals(
                NebulaM3SemanticCoverageGateRecipe.AFTER_SHA256,
                NebulaM3SemanticCoverageGateRecipe.sha256(after));
        assertTrue(before.contains("<counter>LINE</counter>"));
        assertFalse(before.contains("<counter>BRANCH</counter>"));
        assertEquals(1, occurrences(after, "<counter>LINE</counter>"));
        assertEquals(1, occurrences(after, "<counter>BRANCH</counter>"));
        assertEquals(2, occurrences(after, "<minimum>0.99</minimum>"));
        assertTrue(after.contains("<id>m3-semantic-branch-coverage-recipe</id>"));
        assertTrue(after.contains(
                "<recipe>org.eclipse.nebula.m3.rewrite.exact."
                        + "NebulaM3SemanticCoverageGateRecipe</recipe>"));
        assertFalse(new NebulaM3SemanticCoverageGateRecipe().promotionAuthority());
    }

    @Test
    void exactJava21MavenPolicyReplaysOnceThenConverges() {
        String before = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.PREIMAGE);
        String after = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.POSTIMAGE);
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile input = xml(
                NebulaM3SemanticCoverageGateRecipe.REPOSITORY_PATH, before, context);

        var run = new NebulaM3SemanticCoverageGateRecipe().run(
                new InMemoryLargeSourceSet(List.of(input)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = run.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile postimage = changes.getFirst().getAfter();
        assertTrue(postimage instanceof Xml.Document);
        assertEquals(after, postimage.printAll());

        var replayErrors = new ArrayList<Throwable>();
        var replay = new NebulaM3SemanticCoverageGateRecipe().run(
                new InMemoryLargeSourceSet(List.of(postimage)),
                new InMemoryExecutionContext(replayErrors::add));
        assertTrue(replayErrors.isEmpty(), replayErrors.toString());
        assertTrue(replay.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleMavenOwnerFailsAndUnrelatedPomRemainsUntouched() {
        String before = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.PREIMAGE);
        String stale = before.replace(
                "<name>Nebula M3 Recipe First</name>",
                "<name>unreviewed</name>");
        var staleErrors = new ArrayList<Throwable>();
        var staleContext = new InMemoryExecutionContext(staleErrors::add);
        SourceFile changed = xml(
                NebulaM3SemanticCoverageGateRecipe.REPOSITORY_PATH,
                stale, staleContext);

        boolean rejected = false;
        try {
            var run = new NebulaM3SemanticCoverageGateRecipe().run(
                    new InMemoryLargeSourceSet(List.of(changed)), staleContext);
            rejected = !staleErrors.isEmpty();
            assertTrue(run.getChangeset().getAllResults().isEmpty());
        } catch (RuntimeException failure) {
            rejected = true;
        }
        assertTrue(rejected, "a stale exact Maven owner may never pass silently");

        var unrelatedErrors = new ArrayList<Throwable>();
        var unrelatedContext = new InMemoryExecutionContext(unrelatedErrors::add);
        SourceFile other = xml("other/pom.xml", before, unrelatedContext);
        var untouched = new NebulaM3SemanticCoverageGateRecipe().run(
                new InMemoryLargeSourceSet(List.of(other)), unrelatedContext);
        assertTrue(unrelatedErrors.isEmpty(), unrelatedErrors.toString());
        assertTrue(untouched.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void policyChangeRetainsTychoAndDoesNotBindMutationToLifecycle() {
        String before = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.PREIMAGE);
        String after = NebulaM3SemanticCoverageGateRecipe.resource(
                NebulaM3SemanticCoverageGateRecipe.POSTIMAGE);
        assertTrue(before.contains("<id>semantic-coverage</id>"));
        assertTrue(after.contains("<id>semantic-coverage</id>"));
        assertTrue(after.contains("<id>m3-semantic-branch-coverage-recipe</id>"));
        assertFalse(after.contains("<goal>runNoFork</goal>"));
        assertFalse(after.contains("<goal>rewrite:run</goal>"));
        assertEquals(
                occurrences(before, "<id>m3-semantic-coverage-agent</id>"),
                occurrences(after, "<id>m3-semantic-coverage-agent</id>"));
        assertEquals(
                occurrences(before, "<id>m3-semantic-coverage-check</id>"),
                occurrences(after, "<id>m3-semantic-coverage-check</id>"));
    }

    private static int occurrences(String text, String needle) {
        return (text.length() - text.replace(needle, "").length()) / needle.length();
    }

    private static SourceFile xml(
            String path,
            String content,
            InMemoryExecutionContext context) {
        List<SourceFile> files = XmlParser.builder().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), content)),
                        null,
                        context)
                .toList();
        assertEquals(1, files.size());
        assertTrue(files.getFirst() instanceof Xml.Document);
        return files.getFirst();
    }
}
