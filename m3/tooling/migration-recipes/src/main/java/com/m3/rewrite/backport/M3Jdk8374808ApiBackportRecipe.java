// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/** Java-21 additive API atom for upstream JDK-8374808. */
public final class M3Jdk8374808ApiBackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "264fdc5b4ed5f4e35168048533196e670c3dda6c";
    private static final String ROOT = "/com/m3/rewrite/backport/jdk8374808-api/";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8374808 KeyStore Instant API atom";
    }

    @Override
    public String getDescription() {
        return "Adds the Java-21-compatible KeyStore/KeyStoreSpi Instant API bridge and focused regression proof from exact JDK21 preimages.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3", "jdk21", "backport", "jdk-8374808", "keystore", "security",
                "instant", "library-api", "verbatim-preimage", "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                pair(
                        "src/java.base/share/classes/java/security/KeyStore.java",
                        "before-KeyStore.java.txt",
                        "after-KeyStore.java.txt"),
                pair(
                        "src/java.base/share/classes/java/security/KeyStoreSpi.java",
                        "before-KeyStoreSpi.java.txt",
                        "after-KeyStoreSpi.java.txt"),
                pair(
                        "test/jdk/java/security/KeyStore/TestKeyStoreBasic.java",
                        "before-TestKeyStoreBasic.java.txt",
                        "after-TestKeyStoreBasic.java.txt"));
    }

    private static Recipe pair(String path, String before, String after) {
        return new M3VerbatimJavaPairRecipe(
                path,
                "jdk-21+35",
                "openjdk/jdk@" + UPSTREAM_COMMIT,
                "JDK-8374808",
                resource(before),
                resource(after));
    }

    static String resource(String name) {
        try (var input =
                M3Jdk8374808ApiBackportRecipe.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("missing JDK-8374808 resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }
}
