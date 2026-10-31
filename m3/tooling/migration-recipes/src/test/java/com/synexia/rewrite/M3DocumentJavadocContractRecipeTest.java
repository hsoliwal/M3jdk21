// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.openrewrite.test.SourceSpecs.text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

/** Exact documenter owner -> reviewed Javadoc-preserving postimage custody. */
final class M3DocumentJavadocContractRecipeTest implements RewriteTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/document-javadoc-contract/";
    private static final String PATH =
            "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/atom/M3DocumentPureIntAtomRecipe.java";

    @Test
    void appliesReviewedDocumenterChangeOnceAndReachesFixedPoint() {
        rewriteRun(
                spec -> spec
                        .recipes(new M3HashPinnedJavaSnapshotRecipe(
                                "document-javadoc-contract", false))
                        .cycles(2)
                        .expectedCyclesThatMakeChanges(1),
                owner(false));
    }

    @Test
    void reviewedPostimageIsAlreadyFixedPoint() {
        rewriteRun(
                spec -> spec.recipes(new M3HashPinnedJavaSnapshotRecipe(
                        "document-javadoc-contract", false)),
                owner(true));
    }

    private static SourceSpecs owner(boolean applied) {
        String source = read(applied ? "after.java.txt" : "before.java.txt");
        return applied
                ? text(source, spec -> spec.noTrim().path(PATH))
                : text(source, read("after.java.txt"), spec -> spec.noTrim().path(PATH));
    }

    private static String read(String name) {
        try (InputStream input =
                M3DocumentJavadocContractRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
