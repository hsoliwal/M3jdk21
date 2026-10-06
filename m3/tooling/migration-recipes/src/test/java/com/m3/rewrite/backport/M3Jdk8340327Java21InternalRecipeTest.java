// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jdk8340327Java21InternalRecipeTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/jdk-8340327-java21-internal/";

    @Test
    void exactCurrentPreimagesReplayToTenFileJava21ClosureAndFixedPoint() {
        List<SourceFile> before = List.of(
                parse(
                        "src/java.base/share/classes/sun/security/util/KeyUtil.java",
                        resource("before-05-KeyUtil.java.after")),
                parse(
                        "src/java.base/share/classes/sun/security/util/SignatureUtil.java",
                        resource("before-06-SignatureUtil.java.after")));

        var first = new M3Jdk8340327Java21InternalRecipe().run(
                new InMemoryLargeSourceSet(before),
                context(),
                1);
        List<Result> changes = first.getChangeset().getAllResults();
        assertEquals(10, changes.size());

        List<SourceFile> after = changes.stream()
                .map(Result::getAfter)
                .sorted(Comparator.comparing(file -> file.getSourcePath().toString()))
                .toList();
        assertEquals(10, after.size());
        assertTrue(after.stream().anyMatch(file -> file.getSourcePath().toString()
                .replace('\\', '/')
                .equals("test/jdk/sun/security/provider/NamedKeyFactoryTest.java")));

        String joined = after.stream()
                .map(SourceFile::printAll)
                .reduce("", (left, right) -> left + "\n" + right);
        assertFalse(joined.contains("import java.security.AsymmetricKey;"));
        assertFalse(joined.contains("instanceof AsymmetricKey"));
        assertTrue(joined.contains("Java 21 has no public AsymmetricKey#getParams contract."));
        assertTrue(joined.contains("key instanceof NamedPKCS8Key n8k"));
        assertTrue(joined.contains("key instanceof NamedX509Key nk"));

        var second = new M3Jdk8340327Java21InternalRecipe().run(
                new InMemoryLargeSourceSet(new ArrayList<>(after)),
                context(),
                1);
        assertTrue(second.getChangeset().getAllResults().isEmpty());
    }

    @Test
    void metadataNamesTheInternalJava21Adaptation() {
        var recipe = new M3Jdk8340327Java21InternalRecipe();
        assertTrue(recipe.getDisplayName().contains("8340327"));
        assertTrue(recipe.getDescription().contains("AsymmetricKey"));
        assertEquals(1, recipe.getRecipeList().size());
    }

    private static SourceFile parse(String path, String source) {
        return JavaParser.fromJavaVersion()
                .build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static String resource(String name) {
        try (var stream =
                M3Jdk8340327Java21InternalRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (stream == null) throw new AssertionError("missing resource " + name);
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }
}
