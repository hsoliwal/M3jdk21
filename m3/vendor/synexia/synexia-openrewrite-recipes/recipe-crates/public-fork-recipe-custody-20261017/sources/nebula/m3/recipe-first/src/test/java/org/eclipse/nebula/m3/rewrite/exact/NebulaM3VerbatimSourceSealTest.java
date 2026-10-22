// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.exact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class NebulaM3VerbatimSourceSealTest {
    @TempDir Path root;

    @Test
    void exactBytesProduceAuthorityFreeReceipt() throws Exception {
        byte[] source = "class A {}\n".getBytes(StandardCharsets.UTF_8);
        Path file = root.resolve("src/A.java");
        Files.createDirectories(file.getParent());
        Files.write(file, source);

        String hash = NebulaM3VerbatimSourceSeal.sha256(source);
        var receipt = NebulaM3VerbatimSourceSeal.verify(root, "src/A.java", hash);

        assertEquals("src/A.java", receipt.logicalPath());
        assertEquals(hash, receipt.sha256());
        assertEquals(source.length, receipt.bytes());
        assertTrue(receipt.root().matches("[0-9a-f]{64}"));
        assertFalse(receipt.sourceMutationAuthority());
        assertFalse(receipt.replacementAuthority());
        assertFalse(receipt.promotionAuthority());
    }

    @Test
    void trailingRawBytesFailEvenWhenStructuredParserCouldIgnoreThem() throws Exception {
        byte[] reviewed = "class A {}\n".getBytes(StandardCharsets.UTF_8);
        Path file = root.resolve("src/A.java");
        Files.createDirectories(file.getParent());
        Files.write(file, reviewed);
        String hash = NebulaM3VerbatimSourceSeal.sha256(reviewed);

        Files.writeString(
                file,
                "class A {}\n// drift\n",
                StandardCharsets.UTF_8);

        IllegalStateException failure =
                assertThrows(
                        IllegalStateException.class,
                        () -> NebulaM3VerbatimSourceSeal.verify(root, "src/A.java", hash));
        assertTrue(failure.getMessage().contains("preimage drift"));
    }

    @Test
    void traversalAndAuthorityEscalationFailClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3VerbatimSourceSeal.verify(
                        root, "../escape.java", "0".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3VerbatimSourceSeal.verify(
                        root, "/absolute.java", "0".repeat(64)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new NebulaM3VerbatimSourceSeal.Receipt(
                                "src/A.java",
                                "0".repeat(64),
                                1L,
                                true,
                                false,
                                false,
                                ""));
    }

    @Test
    void symlinkedParentIsRejected(@TempDir Path outside) throws Exception {
        Path outsideFile = outside.resolve("A.java");
        Files.writeString(outsideFile, "class A {}\n", StandardCharsets.UTF_8);
        try {
            Files.createSymbolicLink(root.resolve("src"), outside);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unsupported) {
            return;
        }

        String hash = NebulaM3VerbatimSourceSeal.sha256(Files.readAllBytes(outsideFile));
        assertThrows(
                IllegalArgumentException.class,
                () -> NebulaM3VerbatimSourceSeal.verify(root, "src/A.java", hash));
    }
}
