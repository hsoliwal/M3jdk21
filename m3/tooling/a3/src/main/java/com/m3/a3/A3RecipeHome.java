// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import com.synexia.rewrite.atom.M3PureIntConvergenceRecipe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.openrewrite.Recipe;

/**
 * Thin receiver for the Synexia-owned reusable A3 recipe.
 *
 * <p>M3JDK21 owns JDK-specific inventory/planning/application policy only. The reusable source
 * transformation is mastered in hsoliwal/com.synexia and arrives as a pinned Maven artifact.
 */
final class A3RecipeHome {
    static final String SYNEXIA_REPOSITORY = "hsoliwal/com.synexia";
    static final String SYNEXIA_COMMIT = "4172b7ea5bb35f74ee19c578bddfb582eb210b5d";
    static final String ARTIFACT = "com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT";
    static final String RECIPE_CLASS =
            "com.synexia.rewrite.atom.M3PureIntConvergenceRecipe";
    static final String EXPORT_MANIFEST =
            "META-INF/m3/jdk-a3-recipe-export.tsv";
    static final String EXPORT_MANIFEST_GIT_BLOB =
            "42976645dab8b578ead93fc1a2be0a9f007b3dcd";
    static final String CONVERGENCE_SOURCE_GIT_BLOB =
            "b11aff3ace8e77684cfa8cc13fbbb445a05c8a09";

    private A3RecipeHome() {
    }

    static Recipe recipe() {
        requireCanonicalExport();
        Recipe recipe = new M3PureIntConvergenceRecipe();
        if (!RECIPE_CLASS.equals(recipe.getClass().getName())) {
            throw new IllegalStateException("A3 canonical recipe class drift");
        }
        return recipe;
    }

    static void requireCanonicalExport() {
        byte[] bytes;
        ClassLoader loader = M3PureIntConvergenceRecipe.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(EXPORT_MANIFEST)) {
            if (input == null) {
                throw new IllegalStateException(
                        "missing Synexia A3 export manifest: " + EXPORT_MANIFEST);
            }
            bytes = input.readAllBytes();
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "cannot read Synexia A3 export manifest", failure);
        }

        String blob = gitBlob(bytes);
        if (!EXPORT_MANIFEST_GIT_BLOB.equals(blob)) {
            throw new IllegalStateException(
                    "Synexia A3 export manifest drift: " + blob);
        }

        String text = new String(bytes, StandardCharsets.UTF_8);
        String expectedRow =
                RECIPE_CLASS
                        + "\tsrc/main/java/com/synexia/rewrite/atom/"
                        + "M3PureIntConvergenceRecipe.java\t"
                        + CONVERGENCE_SOURCE_GIT_BLOB;
        if (!text.lines().anyMatch(expectedRow::equals)) {
            throw new IllegalStateException(
                    "Synexia A3 convergence source identity is not pinned");
        }
    }

    private static String gitBlob(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(
                    ("blob " + bytes.length + "\0")
                            .getBytes(StandardCharsets.UTF_8));
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
