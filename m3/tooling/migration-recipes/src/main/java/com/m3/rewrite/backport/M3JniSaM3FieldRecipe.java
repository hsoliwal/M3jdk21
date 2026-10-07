// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import org.openrewrite.Recipe;

/**
 * M3JDK-owned installer that re-points the stale {@code mindex}/{@code MIndexString} lookups in
 * {@code libjava/jni_util.c} and the Serviceability Agent string decoder to the current
 * {@code java.lang.String.m3} / {@code java.lang.M3String} layout.
 *
 * <p>Without this fix {@code InitializeEncoding} leaves a pending {@code NoSuchFieldError} on every
 * VM start, so no {@code -XX:+UseM3StringStorage} evidence can be collected from master. The actual
 * source mutation is delegated to the exact hash-pinned Java and native/text snapshot crates named
 * {@link #CRATE}; this wrapper only gives the task a stable Maven/OpenRewrite recipe identity.</p>
 */
public final class M3JniSaM3FieldRecipe extends Recipe {
    public static final String CRATE = "m3-jni-sa-m3-field";

    @Override
    public String getDisplayName() {
        return "M3 JNI and SA String.m3 field alignment";
    }

    @Override
    public String getDescription() {
        return "Replaces the removed String.mindex/MIndexString JNI field lookup and SA decoder with "
                + "the java.lang.String.m3/M3String owner+coordinate layout from exact hash-pinned "
                + "preimages. No String semantics, HotSpot flag or public API changes.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(CRATE),
                new M3Jdk21HashPinnedTextSnapshotRecipe(CRATE));
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    public boolean bootstrapPromotionAuthority() {
        return false;
    }

    public boolean semanticAuthority() {
        return false;
    }
}
