// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import com.synexia.rewrite.M3RecipeMasteryPortableReceipt;
import com.synexia.rewrite.atom.M3PureIntConvergenceRecipe;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.openrewrite.Recipe;

/**
 * Thin receiver for the Synexia-owned reusable A3 recipe and mastery evidence verifier.
 *
 * <p>M3JDK21 owns JDK-specific inventory/planning/application policy only. Reusable source
 * transformations and portable mastery evidence validation are mastered in hsoliwal/com.synexia
 * and arrive through one pinned slim Maven artifact.</p>
 */
final class A3RecipeHome {
    static final String SYNEXIA_REPOSITORY = "hsoliwal/com.synexia";
    static final String SYNEXIA_COMMIT = "3357736011d1d3e3173433d1a8bdb2795f71599e";
    static final String SYNEXIA_PR = "9599";
    static final String ARTIFACT = "com.synexia:synexia-jdk-a3-recipes:1.0.0-SNAPSHOT";
    static final String RECIPE_CLASS =
            "com.synexia.rewrite.atom.M3PureIntConvergenceRecipe";
    static final String PORTABLE_MASTERY_CLASS =
            "com.synexia.rewrite.M3RecipeMasteryPortableReceipt";
    static final String EXPORT_MANIFEST =
            "META-INF/m3/jdk-a3-recipe-export.tsv";
    static final String EXPORT_MANIFEST_GIT_BLOB =
            "8a3e3d6e802e95dbcc7b0bf83a347887b02d6717";
    static final String CONVERGENCE_SOURCE_GIT_BLOB =
            "b11aff3ace8e77684cfa8cc13fbbb445a05c8a09";
    static final String PORTABLE_MASTERY_SOURCE_GIT_BLOB =
            "cd54b351f444a497c759a009681cc97d3e79c5ba";

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

    static M3RecipeMasteryPortableReceipt.Verified verifyMastery(
            String tsv,
            String expectedRoot) {
        requireCanonicalExport();
        return M3RecipeMasteryPortableReceipt.verify(tsv, expectedRoot);
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
        requireRow(
                text,
                RECIPE_CLASS,
                "src/main/java/com/synexia/rewrite/atom/M3PureIntConvergenceRecipe.java",
                CONVERGENCE_SOURCE_GIT_BLOB);
        requireRow(
                text,
                PORTABLE_MASTERY_CLASS,
                "src/main/java/com/synexia/rewrite/M3RecipeMasteryPortableReceipt.java",
                PORTABLE_MASTERY_SOURCE_GIT_BLOB);
    }

    private static void requireRow(
            String manifest,
            String className,
            String path,
            String blob) {
        String expected = className + "\t" + path + "\t" + blob;
        if (!manifest.lines().anyMatch(expected::equals)) {
            throw new IllegalStateException(
                    "Synexia A3 export identity is not pinned: " + className);
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
