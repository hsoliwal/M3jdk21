/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.compiletime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Real javac.classfile -> existing ASM scan -> GVAS literal image, without bytecode mutation. */
final class M3GvasLiteralHarvesterTest {
    @TempDir Path temp;

    @Test
    void scansConstantPoolThenEmitsPinnedLiteralImage() throws Exception {
        Path source = temp.resolve("CompileSource.java");
        Files.writeString(source, """
                public final class CompileSource {
                    private static final String CONSTANT = "M3compile" + "time";
                    public static boolean run(String s) {
                        return s.startsWith(CONSTANT) && s.contains("M3squeeze");
                    }
                }
                """);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "21",
                "-d", temp.toString(), source.toString());
        assertEquals(0, result);
        Path jar = temp.resolve("fixture.jar");
        try (var stream = new JarOutputStream(Files.newOutputStream(jar))) {
            stream.putNextEntry(new JarEntry("CompileSource.class"));
            stream.write(Files.readAllBytes(temp.resolve("CompileSource.class")));
            stream.closeEntry();
        }
        Path listing = temp.resolve("jars.txt");
        Files.writeString(listing, jar + "\n");
        Path out = temp.resolve("prepared");
        M3GvasLiteralHarvester.main(new String[] {"--out", out.toString(),
                "--jars", listing.toString(), "--max-bytes", "1048576"});
        byte[] image = Files.readAllBytes(out.resolve("literals.m3gvas"));
        assertTrue(image.length > 64);
        assertEquals("M3GVAS01", new String(image, 0, 8, java.nio.charset.StandardCharsets.US_ASCII));
        String receipt = Files.readString(out.resolve("PRECOMPUTE_RECEIPT.tsv"));
        assertTrue(receipt.contains("image_sha256\t" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(image))));
        String offsets = Files.readString(out.resolve("COMPILETIME_LITERAL_OFFSETS.tsv"));
        assertTrue(offsets.split("\n").length >= 3);
        assertTrue(Files.readString(out.resolve("INPUT_SET_MANIFEST.tsv")).contains("fixture.jar"));
        assertThrows(java.io.IOException.class, () -> M3GvasLiteralHarvester.main(
                new String[] {"--out", out.toString(), "--jars", listing.toString()}));
    }

    @Test
    void failsClosedForUnparseableClass() throws Exception {
        Path jar = temp.resolve("broken.jar");
        try (var stream = new JarOutputStream(Files.newOutputStream(jar))) {
            stream.putNextEntry(new JarEntry("Broken.class"));
            stream.write(new byte[] {0, 0, 0, 0});
            stream.closeEntry();
        }
        Path listing = temp.resolve("invalid-jars.txt");
        Files.writeString(listing, jar + "\n");
        Path out = temp.resolve("bad-output");
        assertThrows(java.io.IOException.class, () -> M3GvasLiteralHarvester.main(
                new String[] {"--out", out.toString(), "--jars", listing.toString()}));
        assertFalse(Files.exists(out));
    }
}
