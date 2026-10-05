// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.openrewrite.test.SourceSpecs.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

/** Exact malformed scope owner -> corrected Java source, held in PlainText custody mode. */
final class M3ScopeLiteralRepairRecipeTest implements RewriteTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/scope-backslash-literal/";
    private static final String PATH =
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/scope/M3ScopeInference.java";

    @Test
    void repairsExactMalformedPreimageAndReachesFixedPoint() {
        rewriteRun(
                spec -> spec
                        .recipes(new M3HashPinnedJavaSnapshotRecipe(
                                "scope-backslash-literal", false))
                        .cycles(2)
                        .expectedCyclesThatMakeChanges(1),
                owner(false));
    }

    @Test
    void reviewedPostimageIsAlreadyFixedPoint() {
        rewriteRun(
                spec -> spec.recipes(new M3HashPinnedJavaSnapshotRecipe(
                        "scope-backslash-literal", false)),
                owner(true));
    }

    private static SourceSpecs owner(boolean applied) {
        String before = read(applied ? "after.java.txt" : "before.java.txt");
        return applied
                ? text(before, spec -> spec.noTrim().path(PATH))
                : text(before, read("after.java.txt"), spec -> spec.noTrim().path(PATH));
    }

    private static String read(String name) {
        try (InputStream input =
                M3ScopeLiteralRepairRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing recipe resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
