// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainText;

final class M3A3NativeJniLaneRecipeTest {
    private static final String RESOURCE_ROOT =
            "/com/m3/rewrite/backport/a3-native-jni-lane/pre/";

    private static final Map<String, String> PREIMAGES =
            Map.of(
                    "m3/backports/README.md",
                    "README.md.before.txt",
                    "m3/backports/file_delta_inventory.py",
                    "file_delta_inventory.py.before.txt",
                    "m3/backports/generate_recipe_crates.py",
                    "generate_recipe_crates.py.before.txt",
                    "m3/backports/test_file_delta_inventory.py",
                    "test_file_delta_inventory.py.before.txt",
                    "m3/backports/test_generate_recipe_crates.py",
                    "test_generate_recipe_crates.py.before.txt",
                    "m3/docs/a3.md",
                    "a3.md.before.txt");

    @Test
    void exactToolingPreimagesConvergeThenReachFixedPoint() {
        var recipe =
                new M3Jdk21HashPinnedTextSnapshotRecipe(
                        "a3-native-jni-lane");
        List<SourceFile> before = new ArrayList<>();
        PREIMAGES.forEach(
                (path, resource) ->
                        before.add(
                                PlainText.builder()
                                        .sourcePath(Path.of(path))
                                        .text(resource(resource))
                                        .build()));

        var first =
                recipe.run(
                        new InMemoryLargeSourceSet(before),
                        context(),
                        1);
        var results = first.getChangeset().getAllResults();
        assertEquals(7, results.size());

        Map<String, SourceFile> after = new LinkedHashMap<>();
        for (var result : results) {
            SourceFile source = result.getAfter();
            assertTrue(source != null);
            after.put(normalized(source.getSourcePath()), source);
        }

        assertTrue(
                after.get("m3/backports/file_delta_inventory.py")
                        .printAll()
                        .contains("SOURCE_SEALED_NATIVE_PAIR"));
        assertTrue(
                after.get("m3/backports/generate_recipe_crates.py")
                        .printAll()
                        .contains("--include-native"));
        assertTrue(
                after.get("m3/backports/test_generate_recipe_crates.py")
                        .printAll()
                        .contains("jdk24-native-0001"));
        assertTrue(after.containsKey("m3/docs/a3-native-jni.md"));
        assertTrue(
                after.get("m3/backports/README.md")
                        .printAll()
                        .contains("--include-native"));
        assertTrue(
                after.get("m3/docs/a3.md")
                        .printAll()
                        .contains("a3-native-jni.md"));

        var second =
                recipe.run(
                        new InMemoryLargeSourceSet(
                                List.copyOf(after.values())),
                        context(),
                        1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
        assertTrue(recipe.getTags().contains("hash-pinned"));
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    private static String resource(String name) {
        try (var input =
                M3A3NativeJniLaneRecipeTest.class.getResourceAsStream(
                        RESOURCE_ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing A3 native/JNI preimage: " + name);
            }
            return new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read A3 native/JNI preimage",
                    failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(
                failure -> {
                    throw new AssertionError(failure);
                });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
