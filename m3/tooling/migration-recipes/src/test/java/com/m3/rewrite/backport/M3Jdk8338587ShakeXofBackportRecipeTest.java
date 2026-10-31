// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

final class M3Jdk8338587ShakeXofBackportRecipeTest {
    private static final String SHA3 =
            "src/java.base/share/classes/sun/security/provider/SHA3.java";
    private static final String TEST =
            "test/jdk/sun/security/provider/MessageDigest/SHAKEXofCompatibility.java";

    @Test
    void bindsExactUpstreamAuthorityAndTwoFileJavaCrate() {
        Recipe recipe = new M3Jdk8338587ShakeXofBackportRecipe();

        assertEquals(
                "c54fc08aa3c63e4b26dc5edb2436844dfd3bab7c",
                M3Jdk8338587ShakeXofBackportRecipe.UPSTREAM_COMMIT);
        assertEquals(1, recipe.getRecipeList().size());
        assertEquals(
                "jdk24-jdk8338587-shake-xof",
                assertInstanceOf(
                                M3Jdk21HashPinnedSnapshotRecipe.class,
                                recipe.getRecipeList().getFirst())
                        .getCrateName());
        assertTrue(recipe.getTags().contains("candidate-only"));
    }

    @Test
    void exactJava21Sha3PreimageProducesAdaptedXofAndThenFixedPoint() throws Exception {
        Recipe recipe = new M3Jdk8338587ShakeXofBackportRecipe();
        var first = recipe.run(
                new InMemoryLargeSourceSet(List.of(java(SHA3, currentSha3()))),
                context(),
                1);
        List<Result> changes = first.getChangeset().getAllResults();

        assertEquals(2, changes.size());
        SourceFile sha3 = after(changes, SHA3);
        assertTrue(sha3.printAll().contains("public static final class SHAKE128 extends SHA3XOF"));
        assertTrue(sha3.printAll().contains("public static final class SHAKE256 extends SHA3XOF"));
        assertTrue(sha3.printAll().contains("public static void keccak(long[] stateArr)"));
        assertTrue(sha3.printAll().contains("private byte[] state = new byte[WIDTH]"));
        assertTrue(sha3.printAll().contains("protected int squeezeOffset = -1"));
        assertTrue(after(changes, TEST).printAll().contains("nested/legacy SHAKE256 mismatch"));

        List<SourceFile> after = changes.stream().map(Result::getAfter).toList();
        assertTrue(
                recipe.run(new InMemoryLargeSourceSet(after), context(), 1)
                        .getChangeset().getAllResults().isEmpty());
    }

    @Test
    void staleSha3PreimageFailsClosed() throws Exception {
        String drift = currentSha3() + "\n// drift\n";
        assertThrows(
                RuntimeException.class,
                () -> new M3Jdk8338587ShakeXofBackportRecipe()
                        .run(
                                new InMemoryLargeSourceSet(List.of(java(SHA3, drift))),
                                context(),
                                1)
                        .getChangeset().getAllResults());
    }

    private static SourceFile after(List<Result> results, String path) {
        return results.stream()
                .map(Result::getAfter)
                .filter(source -> source != null && normalized(source.getSourcePath()).equals(path))
                .findFirst()
                .orElseThrow();
    }

    private static String currentSha3() throws IOException {
        try (var input =
                M3Jdk8338587ShakeXofBackportRecipeTest.class.getResourceAsStream(
                        "/com/m3/rewrite/backport/jdk24-jdk8338587-shake-xof/"
                                + "pre-SHA3.java.before.txt")) {
            if (input == null) throw new IOException("missing SHA3 preimage");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static SourceFile java(String path, String source) {
        return JavaParser.fromJavaVersion().build()
                .parseInputs(
                        List.of(Parser.Input.fromString(Path.of(path), source)),
                        null,
                        context())
                .findFirst()
                .orElseThrow();
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> {
            throw new AssertionError(error);
        });
    }

    private static String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }
}
