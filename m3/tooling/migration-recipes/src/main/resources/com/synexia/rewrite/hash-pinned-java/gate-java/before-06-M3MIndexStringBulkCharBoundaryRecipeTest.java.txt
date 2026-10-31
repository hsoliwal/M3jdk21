// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.java;

/** Exact current java.base owner -> reviewed MIndex bulk-character boundary candidate. */
class M3MIndexStringBulkCharBoundaryRecipeTest implements RewriteTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/mindex-string-bulk-char-boundary/";
    private static final String PATH =
            "src/java.base/share/classes/java/lang/MIndexString.java";

    private static String read(String name) {
        try (InputStream input =
                M3MIndexStringBulkCharBoundaryRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static SourceSpecs owner(boolean applied) {
        String before = read(applied ? "MIndexString.java.txt" : "MIndexString.java.before.txt");
        return applied
                ? java(before, spec -> spec.path(PATH))
                : java(before, read("MIndexString.java.txt"), spec -> spec.path(PATH));
    }

    @Test
    void appliesExactJdkOwnerAndReachesFixedPoint() {
        rewriteRun(
                spec -> spec
                        .recipe(new M3HashPinnedJavaSnapshotRecipe(
                                "mindex-string-bulk-char-boundary"))
                        .cycles(2)
                        .expectedCyclesThatMakeChanges(1),
                owner(false));
    }

    @Test
    void reviewedPostimageIsAlreadyFixedPoint() {
        rewriteRun(
                spec -> spec.recipe(new M3HashPinnedJavaSnapshotRecipe(
                        "mindex-string-bulk-char-boundary")),
                owner(true));
    }
}
