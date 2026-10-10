// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.synexia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Proves the current M3JDK consumer surface does not depend on the removed legacy PlainText mode
 * of Synexia's hash-pinned Java recipe.
 */
final class M3SynexiaHashPinnedConsumerTest {
    private static final Pattern TWO_ARGUMENT_CONSTRUCTOR =
            Pattern.compile(
                    "new\\s+M3HashPinnedJavaSnapshotRecipe\\s*\\([^;\\n]*,",
                    Pattern.MULTILINE);
    private static final Pattern YAML_LST_OPTION =
            Pattern.compile("(?m)^\\s*lst\\s*:");

    @Test
    void targetDoesNotConsumeRemovedPlainTextModeApi() throws Exception {
        Path root = repositoryRoot();
        AtomicInteger canonicalUses = new AtomicInteger();

        for (Path start : new Path[] {
                root.resolve("m3/tooling/migration-recipes"),
                root.resolve("m3/collections/recipe")
        }) {
            if (!Files.exists(start)) continue;
            try (var files = Files.walk(start)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (!(name.endsWith(".java")
                            || name.endsWith(".yml")
                            || name.endsWith(".yaml"))) {
                        continue;
                    }
                    String normalized =
                            root.relativize(file).toString().replace('\\', '/');
                    if (normalized.endsWith(
                            "/src/main/java/com/synexia/rewrite/"
                                    + "M3HashPinnedJavaSnapshotRecipe.java")) {
                        continue;
                    }

                    String text = Files.readString(file);
                    if (text.contains("M3HashPinnedJavaSnapshotRecipe")) {
                        canonicalUses.incrementAndGet();
                    }
                    assertFalse(
                            TWO_ARGUMENT_CONSTRUCTOR.matcher(text).find(),
                            "legacy two-argument hash-pinned constructor consumed by "
                                    + normalized);
                    assertFalse(
                            text.contains(".isLst()"),
                            "legacy isLst() consumed by " + normalized);
                    assertFalse(
                            YAML_LST_OPTION.matcher(text).find()
                                    && text.contains("M3HashPinnedJavaSnapshotRecipe"),
                            "legacy lst YAML option consumed by " + normalized);
                }
            }
        }

        assertTrue(canonicalUses.get() > 0, "consumer scan found no hash-pinned recipe uses");
    }

    private static Path repositoryRoot() {
        Path module = Path.of("").toAbsolutePath().normalize();
        if ("migration-recipes".equals(module.getFileName().toString())) {
            return module.getParent().getParent().getParent();
        }
        return Path.of(System.getProperty("maven.multiModuleProjectDirectory", "."))
                .toAbsolutePath()
                .normalize();
    }
}
