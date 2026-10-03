// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jdk8374808ApiBackportRecipeTest {
    private static final String KEYSTORE =
            "src/java.base/share/classes/java/security/KeyStore.java";
    private static final String SPI =
            "src/java.base/share/classes/java/security/KeyStoreSpi.java";
    private static final String TEST =
            "test/jdk/java/security/KeyStore/TestKeyStoreBasic.java";

    @Test
    void exactApiAtomReplaysAndThenStops() {
        Map<String, String> before = new LinkedHashMap<>();
        before.put(KEYSTORE, resource("before-KeyStore.java.txt"));
        before.put(SPI, resource("before-KeyStoreSpi.java.txt"));
        before.put(TEST, resource("before-TestKeyStoreBasic.java.txt"));
        before.put("test/Unrelated.java", "final class Unrelated {}\n");

        Map<String, String> changed = apply(new M3Jdk8374808ApiBackportRecipe(), before);
        assertEquals(3, changed.size());
        assertEquals(resource("after-KeyStore.java.txt"), changed.get(KEYSTORE));
        assertEquals(resource("after-KeyStoreSpi.java.txt"), changed.get(SPI));
        assertEquals(resource("after-TestKeyStoreBasic.java.txt"), changed.get(TEST));
        assertFalse(changed.containsKey("test/Unrelated.java"));

        Map<String, String> after = new LinkedHashMap<>();
        after.put(KEYSTORE, resource("after-KeyStore.java.txt"));
        after.put(SPI, resource("after-KeyStoreSpi.java.txt"));
        after.put(TEST, resource("after-TestKeyStoreBasic.java.txt"));
        assertTrue(apply(new M3Jdk8374808ApiBackportRecipe(), after).isEmpty());
    }

    @Test
    void recipeMetadataPinsUpstreamAndApiScopeTag() {
        var recipe = new M3Jdk8374808ApiBackportRecipe();
        assertEquals(
                "264fdc5b4ed5f4e35168048533196e670c3dda6c",
                M3Jdk8374808ApiBackportRecipe.UPSTREAM_COMMIT);
        assertTrue(recipe.getTags().contains("library-api"));
        assertTrue(recipe.getTags().contains("candidate-only"));
        assertEquals(3, recipe.getRecipeList().size());
    }

    @Test
    void sourceDriftFailsClosed() {
        String drifted = resource("before-KeyStore.java.txt") + "// drift\n";
        assertThrows(
                IllegalStateException.class,
                () -> apply(
                        new M3Jdk8374808ApiBackportRecipe(),
                        Map.of(KEYSTORE, drifted,
                                SPI, resource("before-KeyStoreSpi.java.txt"),
                                TEST, resource("before-TestKeyStoreBasic.java.txt"))));
    }

    private static String resource(String name) {
        return M3Jdk8374808ApiBackportRecipe.resource(name);
    }

    private static Map<String, String> apply(Recipe recipe, Map<String, String> sources) {
        var context = new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
        List<Parser.Input> inputs = new ArrayList<>(sources.size());
        sources.forEach((path, source) ->
                inputs.add(Parser.Input.fromString(Path.of(path), source)));
        List<SourceFile> parsed = JavaParser.fromJavaVersion()
                .build()
                .parseInputs(inputs, null, context)
                .toList();
        var result = recipe.run(new InMemoryLargeSourceSet(parsed), context, 3);
        Map<String, String> changed = new LinkedHashMap<>();
        result.getChangeset().getAllResults().forEach(change -> {
            SourceFile file = change.getAfter();
            changed.put(
                    file.getSourcePath().toString().replace('\\', '/'),
                    file.printAll());
        });
        return changed;
    }
}
