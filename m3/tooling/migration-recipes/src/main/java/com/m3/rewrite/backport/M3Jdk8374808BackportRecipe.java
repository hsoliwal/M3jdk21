// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

import java.util.List;
import java.util.Set;
import org.openrewrite.Recipe;

/**
 * Java-21-compatible additive API leaf from upstream JDK-8374808.
 *
 * <p>The donor commit is openjdk/jdk
 * {@code 264fdc5b4ed5f4e35168048533196e670c3dda6c}. M3JDK21 deliberately
 * imports only the compatible API/SPI leaf: KeyStore exposes creation time as
 * {@code Instant}, while KeyStoreSpi provides a default adapter over the
 * existing Java 21 {@code Date} method. Provider storage remains unchanged.</p>
 */
public final class M3Jdk8374808BackportRecipe extends Recipe {
    public static final String UPSTREAM_COMMIT =
            "264fdc5b4ed5f4e35168048533196e670c3dda6c";

    @Override
    public String getDisplayName() {
        return "Backport JDK-8374808 KeyStore creation Instant API";
    }

    @Override
    public String getDescription() {
        return "Replays the additive KeyStore/KeyStoreSpi Instant API from exact JDK21 "
                + "preimages while retaining legacy provider Date storage.";
    }

    @Override
    public Set<String> getTags() {
        return Set.of(
                "m3",
                "jdk21",
                "backport",
                "jdk-8374808",
                "security",
                "keystore",
                "instant",
                "hash-pinned",
                "candidate-only");
    }

    @Override
    public int maxCycles() {
        return 1;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new M3Jdk21HashPinnedSnapshotRecipe(
                        "jdk27-keystore-creation-instant-8374808"));
    }
}
