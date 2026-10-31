// SPDX-License-Identifier: Apache-2.0
package jdk.m3.a3;

import jdk.internal.m3.rewrite.M3RecipeMasteryPortableReceipt;
import jdk.internal.m3.rewrite.atom.M3PureIntConvergenceRecipe;
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
 * <p>M3JDK 21 owns JDK-specific inventory/planning/application policy only. Reusable source
 * transformations and portable mastery evidence validation are mastered in hsoliwal/com.synexia
 * and arrive through one pinned slim Maven artifact.</p>
 */
final class A3RecipeHome {
    static final String M3_REPOSITORY = "hsoliwal/com.synexia";
    static final String M3_COMMIT = "a062661e91e769a00c2d027e1f04a7a0c8f9dc28";
    static final String M3_PR = "9599";
    static final String ARTIFACT = "com.synexia:m3jdk-a3-recipes:1.0.0-SNAPSHOT";
    static final String RECIPE_CLASS =
            "jdk.internal.m3.rewrite.atom.M3PureIntConvergenceRecipe";
    static final String PORTABLE_MASTERY_CLASS =
            "jdk.internal.m3.rewrite.M3RecipeMasteryPortableReceipt";
    static final String EXPORT_MANIFEST =
            "META-INF/m3/jdk-a3-recipe-export.tsv";
    static final String EXPORT_MANIFEST_GIT_BLOB =
            "2c51473e7adb6fa50fb0031ac6314a050b90feec";
    static final String CONVERGENCE_SOURCE_GIT_BLOB =
            "7dc6cea0a969a1bd094e324c47a1bb724b5ff1dc";
    static final String PORTABLE_MASTERY_SOURCE_GIT_BLOB =
            "618a0dc7dcd6c4ab6b3361e09022f5c1ff82f0c4";

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
                "src/main/java/jdk/internal/m3/rewrite/atom/M3PureIntConvergenceRecipe.java",
                CONVERGENCE_SOURCE_GIT_BLOB);
        requireRow(
                text,
                PORTABLE_MASTERY_CLASS,
                "src/main/java/jdk/internal/m3/rewrite/M3RecipeMasteryPortableReceipt.java",
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
