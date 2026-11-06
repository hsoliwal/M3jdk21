// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/CountOnlyNativeBinarySealTest.java (blob 84ae2dc72352) by M3_SEARCH_RELOCATE v1.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Standalone Java21 assertions for the real-JNI-only binary provenance guard. */
final class CountOnlyNativeBinarySealTest {
    private static final String ABC_SHA256 =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test void canonicalBinaryAndExactShaAreRequired() throws IOException {
        Path file = Files.createTempFile("m3-jni-identity", ".bin");
        try {
            Files.writeString(file, "abc", StandardCharsets.US_ASCII);
            assertEquals(file.toAbsolutePath().normalize(),
                    CountOnlyNativeBinarySeal.requireExact(file, ABC_SHA256));
            assertThrows(IllegalArgumentException.class,
                    () -> CountOnlyNativeBinarySeal.requireExact(file, null));
            assertThrows(IllegalArgumentException.class,
                    () -> CountOnlyNativeBinarySeal.requireExact(file, ABC_SHA256.toUpperCase(java.util.Locale.ROOT)));
            assertThrows(IllegalArgumentException.class,
                    () -> CountOnlyNativeBinarySeal.requireExact(file, "0".repeat(63)));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test void alteredOrMissingLibraryFailsClosedBeforeJniLoading() throws IOException {
        Path file = Files.createTempFile("m3-jni-drift", ".bin");
        try {
            Files.writeString(file, "abc", StandardCharsets.US_ASCII);
            CountOnlyNativeBinarySeal.requireExact(file, ABC_SHA256);
            Files.writeString(file, "abd", StandardCharsets.US_ASCII);
            assertThrows(IllegalStateException.class,
                    () -> CountOnlyNativeBinarySeal.requireExact(file, ABC_SHA256));
        } finally {
            Files.deleteIfExists(file);
        }
        assertThrows(IllegalStateException.class,
                () -> CountOnlyNativeBinarySeal.requireExact(file, ABC_SHA256));
    }

    @Test void linkedLibraryIsNotTrustedAsAnExactNativeArtifact() throws IOException {
        Path folder = Files.createTempDirectory("m3-jni-linked");
        Path target = folder.resolve("built.so");
        Path linked = folder.resolve("alias.so");
        try {
            Files.writeString(target, "abc", StandardCharsets.US_ASCII);
            try {
                Files.createSymbolicLink(linked, target.getFileName());
            } catch (UnsupportedOperationException | SecurityException | IOException unavailable) {
                // The missing-file negative gate is portable; symlink refusal needs OS support.
                assertThrows(IllegalStateException.class,
                        () -> CountOnlyNativeBinarySeal.requireExact(linked, ABC_SHA256));
                return;
            }
            assertTrue(Files.isSymbolicLink(linked));
            assertThrows(IllegalStateException.class,
                    () -> CountOnlyNativeBinarySeal.requireExact(linked, ABC_SHA256));
            assertEquals(target.toAbsolutePath().normalize(),
                    CountOnlyNativeBinarySeal.requireExact(target, ABC_SHA256));
        } finally {
            Files.deleteIfExists(linked);
            Files.deleteIfExists(target);
            Files.deleteIfExists(folder);
        }
    }
}
