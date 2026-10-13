// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

final class M3CanonicalScopeCustodyTest {
    @Test void restoredExactHistoricalSourceBlobs() throws Exception {
        Path root = Path.of(System.getProperty("basedir", "."))
                .toAbsolutePath().normalize().resolve("src/main/java/com/synexia/rewrite");
        assertEquals("c79d2e9798596dcf6001a5545b034b75b37d0dba",
                gitBlob(Files.readAllBytes(root.resolve("M3RecipeScopePolicy.java"))));
        assertEquals("136869bb06a0f62c7c3f3dbd91fb7c488b5846b5",
                gitBlob(Files.readAllBytes(root.resolve("M3RecipeScopeRegistry.java"))));
    }

    @Test void scopePoliciesRemainCanonical() {
        var file = List.of("src/java.base/share/classes/java/lang/String.java");
        assertTrue(M3RecipeScopePolicy.inferred().fileLocalMechanical(file));
        assertEquals(M3EditScope.MODULE,
                M3RecipeScopePolicy.inferred().resolve(List.of(
                        "src/java.base/share/classes/java/lang/String.java",
                        "src/java.base/share/classes/java/util/Map.java")));
        assertFalse(M3RecipeScopePolicy.explicitContractChange().fileLocalMechanical(file));
        assertTrue(M3RecipeScopeRegistry.registered("org.openrewrite.java.RemoveUnusedImports"));
        assertThrows(IllegalArgumentException.class,
                () -> M3RecipeScopeRegistry.require("unregistered.m3.recipe"));
    }

    private static String gitBlob(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + bytes.length + "\0").getBytes(StandardCharsets.UTF_8));
        digest.update(bytes);
        return HexFormat.of().formatHex(digest.digest());
    }
}
