// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite;

import com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.test.SourceSpecs.text;

/** Exact, reversible-source-pinned candidate for M3 String range ingress. */
class M3StringCharRangeIngressRecipeTest implements RewriteTest {
    private static final String ROOT =
            "/com/synexia/rewrite/hash-pinned-java/m3-string-char-range-ingress-v1/";
    private static final String CRATE = "m3-string-char-range-ingress-v1";

    private static String read(String name) {
        try (InputStream input =
                M3StringCharRangeIngressRecipeTest.class.getResourceAsStream(ROOT + name)) {
            if (input == null) throw new IllegalStateException("missing recipe resource: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static SourceSpecs file(String name, String path, boolean after) {
        String contents = read(name + (after ? ".txt" : ".before.txt"));
        return after
                ? text(contents, spec -> spec.noTrim().path(path))
                : text(contents, read(name + ".txt"), spec -> spec.noTrim().path(path));
    }

    private static SourceSpecs[] sources(boolean after) {
        return new SourceSpecs[] {
            file("M3String.java", "src/java.base/share/classes/java/lang/M3String.java", after),
            file("String.java", "src/java.base/share/classes/java/lang/String.java", after),
        };
    }

    @Test
    void appliesExactOwnerAndEvidencePostimagesAndReachesFixedPoint() {
        rewriteRun(
                spec -> spec
                        .recipes(new M3HashPinnedJavaSnapshotRecipe(CRATE, false))
                        .cycles(2)
                        .expectedCyclesThatMakeChanges(1),
                sources(false));
    }

    @Test
    void reviewedCandidateIsAlreadyFixedPoint() {
        rewriteRun(
                spec -> spec.recipes(new M3HashPinnedJavaSnapshotRecipe(CRATE, false)),
                sources(true));
    }
}
