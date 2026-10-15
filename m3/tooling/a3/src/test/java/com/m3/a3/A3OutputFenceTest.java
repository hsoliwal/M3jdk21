// SPDX-License-Identifier: Apache-2.0
package com.m3.a3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class A3OutputFenceTest {
    @TempDir Path root;

    @Test
    void ordinaryEvidenceIsWrittenAndReplayedDeterministically() throws Exception {
        Path target = Path.of("m3/build/a3/completion/receipt.tsv");

        A3Fs.write(root, target, "first\n");
        assertEquals("first\n", Files.readString(root.resolve(target)));

        A3Fs.write(root, target, "second\n");
        assertEquals("second\n", Files.readString(root.resolve(target)));
        assertEquals(1L, Files.list(root.resolve("m3/build/a3/completion")).count());
    }

    @Test
    void rejectsBuildRootSymlinkWithoutTouchingProduct() throws Exception {
        Path productDirectory = Files.createDirectories(root.resolve("src"));
        Path product = productDirectory.resolve("Important.java");
        Files.writeString(product, "sealed-java-product\n");
        Files.createDirectories(root.resolve("m3"));

        createLinkOrSkip(root.resolve("m3/build"), productDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Fs.write(root, Path.of("m3/build/Important.java"), "intruder\n"));
        assertEquals("sealed-java-product\n", Files.readString(product));
    }

    @Test
    void rejectsNestedParentSymlinkWithoutTouchingProduct() throws Exception {
        Path productDirectory = Files.createDirectories(root.resolve("src"));
        Path product = productDirectory.resolve("Important.java");
        Files.writeString(product, "sealed-java-product\n");
        Files.createDirectories(root.resolve("m3/build"));

        createLinkOrSkip(root.resolve("m3/build/a3"), productDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Fs.write(root, Path.of("m3/build/a3/Important.java"), "intruder\n"));
        assertEquals("sealed-java-product\n", Files.readString(product));
    }

    @Test
    void rejectsTargetSymlinkWithoutTouchingProduct() throws Exception {
        Path productDirectory = Files.createDirectories(root.resolve("src"));
        Path product = productDirectory.resolve("Important.java");
        Files.writeString(product, "sealed-java-product\n");
        Path outputDirectory = Files.createDirectories(root.resolve("m3/build/a3"));

        createLinkOrSkip(outputDirectory.resolve("receipt.tsv"), product);

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Fs.write(root, Path.of("m3/build/a3/receipt.tsv"), "intruder\n"));
        assertEquals("sealed-java-product\n", Files.readString(product));
    }

    @Test
    void hardLinkedOutputDoesNotTruncateProductInode() throws Exception {
        Path productDirectory = Files.createDirectories(root.resolve("src"));
        Path product = productDirectory.resolve("Important.java");
        Files.writeString(product, "sealed-java-product\n");
        Path outputDirectory = Files.createDirectories(root.resolve("m3/build/a3"));
        Path output = outputDirectory.resolve("receipt.tsv");
        try {
            Files.createLink(output, product);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            assumeTrue(false, "filesystem does not support hard links: " + unavailable);
        }

        A3Fs.write(root, Path.of("m3/build/a3/receipt.tsv"), "verified-a3-evidence\n");

        assertEquals("sealed-java-product\n", Files.readString(product));
        assertEquals("verified-a3-evidence\n", Files.readString(output));
        assertFalse(Files.isSameFile(product, output));
    }

    @Test
    void rejectsDirectoryTargetAndLexicalProductEscape() throws Exception {
        Path output = Files.createDirectories(root.resolve("m3/build/a3"));
        assertThrows(
                IllegalArgumentException.class,
                () -> A3Fs.write(root, Path.of("m3/build/a3"), "bad"));
        assertTrue(Files.isDirectory(output));

        assertThrows(
                IllegalArgumentException.class,
                () -> A3Fs.write(root, Path.of("m3/build/../../src/Important.java"), "bad"));
        assertFalse(Files.exists(root.resolve("src/Important.java")));
    }

    private static void createLinkOrSkip(Path link, Path target) throws Exception {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            assumeTrue(false, "filesystem does not support symlinks: " + unavailable);
        }
    }
}
