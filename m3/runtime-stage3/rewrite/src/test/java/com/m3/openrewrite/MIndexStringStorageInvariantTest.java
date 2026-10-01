/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class MIndexStringStorageInvariantTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.m3.openrewrite.MIndexStringStorageInvariant");
    }

    @Test
    void rewritesStringStorageOwner() {
        rewriteRun(
            text(
                """
                private final M3StringStorage m3Storage;
                M3StringStorage m3Storage() { return m3Storage; }
                """,
                """
                private final MIndexString mindex;
                MIndexString mindex() { return mindex; }
                """,
                source -> source.path("src/java.base/share/classes/java/lang/String.java")
            )
        );
    }

    @Test
    void rewritesVmSignatureName() {
        rewriteRun(
            text(
                "macro(_mindex_offset, k, \"mindex\", m3_string_storage_signature, false);",
                "macro(_mindex_offset, k, \"mindex\", mindex_string_signature, false);",
                source -> source.path("src/hotspot/share/classfile/javaClasses.cpp")
            )
        );
    }

    @Test
    void currentFormIsFixedPoint() {
        rewriteRun(
            text(
                """
                private final MIndexString mindex;
                MIndexString mindex() { return mindex; }
                """,
                source -> source.path("src/java.base/share/classes/java/lang/String.java")
            )
        );
    }
}
