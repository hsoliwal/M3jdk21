// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3A3MasteryV6DeliveryTest {
    @Test
    void exactA3PomPreimageAdvancesToMasteryProfileAndFixedPoint() {
        String before = resource("/com/m3/rewrite/a3-mastery-v6/pom.before.xml");
        PlainText pom =
                PlainText.builder()
                        .sourcePath(Path.of("pom.xml"))
                        .text(before)
                        .build();
        var context =
                new InMemoryExecutionContext(
                        error -> {
                            throw new AssertionError(error);
                        });
        var recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "a3-mastery-v6-pom");

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(pom)),
                        context,
                        2);
        var changes = first.getChangeset().getAllResults();
        assertEquals(1, changes.size());
        SourceFile after = changes.getFirst().getAfter();
        assertTrue(after != null);
        assertTrue(after.printAll().contains("<id>m3-a3-mastery-v6</id>"));
        assertTrue(after.printAll().contains("<recipe>com.m3.a3.MasteryV6</recipe>"));

        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(List.of(after)),
                        context,
                        2);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void declarativeDeliveryComposesJavaAndPomRecipes() {
        String yaml = resource("/META-INF/rewrite/m3-a3-mastery-v6.yml");
        assertTrue(yaml.contains("name: com.m3.a3.MasteryV6"));
        assertTrue(yaml.contains("M3A3MasteryV6Recipe"));
        assertTrue(yaml.contains("M3Jdk21HashPinnedTextSnapshotRecipe"));
        assertTrue(yaml.contains("crateName: a3-mastery-v6-pom"));
    }

    private static String resource(String path) {
        try (var input =
                M3A3MasteryV6DeliveryTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("missing A3 V6 resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read A3 V6 resource", failure);
        }
    }
}
