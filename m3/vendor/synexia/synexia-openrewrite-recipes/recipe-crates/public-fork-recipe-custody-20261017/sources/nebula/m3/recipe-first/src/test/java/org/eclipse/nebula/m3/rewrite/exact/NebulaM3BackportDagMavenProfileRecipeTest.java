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
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

/** XML-LST before/after, fixed-point, target fence and stale-POM tests. */
final class NebulaM3BackportDagMavenProfileRecipeTest {
    private static final String PREIMAGE =
            "/org/eclipse/nebula/m3/rewrite/exact/backport-dag-integrity/pom.before.xml.txt";

    @Test
    void pinnedMavenResourcesAndOptInProfileRemainExact() {
        String before = resource(PREIMAGE);
        String after = resource(NebulaM3BackportDagMavenProfileRecipe.POSTIMAGE);
        assertEquals(
                NebulaM3BackportDagMavenProfileRecipe.BEFORE_SHA256,
                NebulaM3BackportDagMavenProfileRecipe.sha256(before));
        assertEquals(
                NebulaM3BackportDagMavenProfileRecipe.AFTER_SHA256,
                NebulaM3BackportDagMavenProfileRecipe.sha256(after));
        assertTrue(after.contains("<id>backport-dag-integrity</id>"));
        assertTrue(after.contains("<artifactId>rewrite-xml</artifactId>"));
        assertTrue(after.contains(
                "<recipe>org.eclipse.nebula.m3.rewrite.exact.NebulaM3BackportIntegrityConvergenceRecipe</recipe>"));
    }

    @Test
    void exactMavenXmlReplaysThenConverges() {
        var recipe = new NebulaM3BackportDagMavenProfileRecipe();
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile before = xml(
                NebulaM3BackportDagMavenProfileRecipe.REPOSITORY_PATH,
                resource(PREIMAGE), context);
        var first = recipe.run(new InMemoryLargeSourceSet(List.of(before)), context);
        assertTrue(errors.isEmpty(), errors.toString());
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertTrue(after instanceof Xml.Document);
        assertEquals(
                resource(NebulaM3BackportDagMavenProfileRecipe.POSTIMAGE),
                after.printAll());
        var secondErrors = new ArrayList<Throwable>();
        var second = new NebulaM3BackportDagMavenProfileRecipe().run(
                new InMemoryLargeSourceSet(List.of(after)),
                new InMemoryExecutionContext(secondErrors::add));
        assertTrue(secondErrors.isEmpty(), secondErrors.toString());
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleOwnerAndUnrelatedPomRemainUnchanged() {
        var errors = new ArrayList<Throwable>();
        var context = new InMemoryExecutionContext(errors::add);
        SourceFile stale = xml(
                NebulaM3BackportDagMavenProfileRecipe.REPOSITORY_PATH,
                resource(PREIMAGE).replace(
                        "<name>Nebula M3 Recipe First</name>", "<name>changed</name>"),
                context);
        var run = new NebulaM3BackportDagMavenProfileRecipe().run(
                new InMemoryLargeSourceSet(List.of(stale)), context);
        assertTrue(run.getChangeset().getAllResults().isEmpty());
        assertFalse(errors.isEmpty());

        var otherErrors = new ArrayList<Throwable>();
        var otherContext = new InMemoryExecutionContext(otherErrors::add);
        SourceFile unrelated = xml(
                "other/pom.xml",
                resource(PREIMAGE),
                otherContext);
        var noop = new NebulaM3BackportDagMavenProfileRecipe().run(
                new InMemoryLargeSourceSet(List.of(unrelated)),
                otherContext);
        assertTrue(otherErrors.isEmpty(), otherErrors.toString());
        assertTrue(noop.getChangeset().getAllResults().isEmpty());
    }

    private static SourceFile xml(
            String path, String body, InMemoryExecutionContext context) {
        List<SourceFile> files = XmlParser.builder().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), body)),
                        null,
                        context)
                .toList();
        assertEquals(1, files.size());
        assertTrue(files.getFirst() instanceof Xml.Document);
        return files.getFirst();
    }

    private static String resource(String name) {
        try (var input =
                NebulaM3BackportDagMavenProfileRecipeTest.class.getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("missing Maven XML resource " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
