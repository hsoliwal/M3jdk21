// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSourceImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Real PMEMSDK FILE/JNI acceptance; not a simulated source owner. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemSourceImageReusableBufferJUnitTest.java blob fb9f45e18533; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class PmemSourceImageReusableBufferJUnitTest {
    @TempDir Path temporary;
    private static final PmemSourceImage.Limits SMALL =
            new PmemSourceImage.Limits(3, 200_000, 65_536, 8192);

    private static byte[] utf8(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private Path snapshot() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("repository"));
        Files.write(root.resolve("A.java"), utf8("a🦊bc\n"));
        Files.write(root.resolve("B.java"), new byte[]{0, (byte) 255, 0, 1});
        Files.write(root.resolve("C.java"), new byte[0]);
        Path snapshot = temporary.resolve("snapshot");
        PmemSourceImage.pack(root, List.of("C.java", "B.java", "A.java"), snapshot, SMALL);
        return snapshot;
    }

    @Test void reusableDestinationKeepsExactBytesAndNeverOverwritesGuards() throws Exception {
        try (PmemSourceImage image = PmemSourceImage.open(snapshot(), PmemBackend.FILE, SMALL)) {
            byte[] scratch = new byte[128];
            Arrays.fill(scratch, (byte) 0x5a);
            int total = 0;
            for (int i = 0; i < image.fileCount(); i++) {
                int written = image.readSealed(i, scratch, 3);
                byte[] original = image.readSealed(i);
                assertEquals(original.length, written);
                assertArrayEquals(original, Arrays.copyOfRange(scratch, 3, 3 + written));
                assertEquals((byte) 0x5a, scratch[0]);
                assertEquals((byte) 0x5a, scratch[1]);
                assertEquals((byte) 0x5a, scratch[2]);
                assertEquals((byte) 0x5a, scratch[3 + written]);
                total += written;
                Arrays.fill(scratch, (byte) 0x5a);
            }
            assertEquals(utf8("a🦊bc\n").length + 4, total);
        }
    }

    @Test void malformedExtentAndClosedEmptySourceRefuse() throws Exception {
        PmemSourceImage image = PmemSourceImage.open(snapshot(), PmemBackend.FILE, SMALL);
        try {
            int len = image.lengthAt(0);
            assertThrows(IndexOutOfBoundsException.class,
                    () -> image.readSealed(0, new byte[len - 1], 0));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> image.readSealed(0, new byte[len + 1], -1));
            assertThrows(NullPointerException.class,
                    () -> image.readSealed(0, null, 0));
            assertThrows(IndexOutOfBoundsException.class,
                    () -> image.readSealed(-1, new byte[128], 0));
            assertEquals(0, image.readSealed(2, new byte[0], 0));
        } finally {
            image.close();
        }
        assertThrows(IllegalStateException.class,
                () -> image.readSealed(2, new byte[0], 0));
    }

    @Test void sameLengthExternalMutationFailsExactShaForBothReadPaths() throws Exception {
        Path packed = snapshot();
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL)) {
            // Modify one byte without changing source.bin size or its manifest.
            Path backing = packed.resolve("source.bin");
            byte[] first = image.readSealed(0);
            first[0] ^= 1;
            Files.write(backing, first, StandardOpenOption.WRITE);
            assertThrows(SecurityException.class, () -> image.readSealed(0));
            assertThrows(SecurityException.class,
                    () -> image.readSealed(0, new byte[128], 0));
        }
    }

    @Test void nativePosixAndFileVerifiedBytesMatchWhenExplicitlyRequested() throws Exception {
        if (!Boolean.getBoolean("pmem.source.reuse.native")) return;
        assertTrue(PmemSdk.capabilities().nativeAvailable(), "explicitly required JNI unavailable");
        Path packed = snapshot();
        try (PmemSourceImage file = PmemSourceImage.open(packed, PmemBackend.FILE, SMALL);
             PmemSourceImage nativeImage = PmemSourceImage.open(packed, PmemBackend.POSIX, SMALL)) {
            byte[] x = new byte[128], y = new byte[128];
            for (int i = 0; i < file.fileCount(); i++) {
                assertEquals(file.readSealed(i, x, 4), nativeImage.readSealed(i, y, 4));
                assertArrayEquals(x, y);
            }
        }
    }

    @Test @EnabledIfSystemProperty(named = "pmem.source.reuse.scale", matches = "true")
    void twentyThousandDistinctFiftyKilobyteFilesWithOneBuffer() throws Exception {
        final int count = 20_000, length = 50_000;
        Path root = Files.createDirectory(temporary.resolve("repository"));
        List<String> paths = new ArrayList<>(count);
        byte[] bytes = new byte[length];
        for (int i = 0; i < count; i++) {
            Arrays.fill(bytes, (byte) (i & 255));
            byte[] salt = utf8("M3-" + i);
            System.arraycopy(salt, 0, bytes, 0, salt.length);
            String name = String.format(Locale.ROOT, "File%05d.java", i);
            Files.write(root.resolve(name), bytes);
            paths.add(name);
        }
        var limits = new PmemSourceImage.Limits(count, 1_000_000_000L, length, 8192);
        Path packed = temporary.resolve("snapshot");
        PmemSourceImage.pack(root, paths, packed, limits);
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, limits)) {
            byte[] scratch = new byte[length];
            long verified = 0;
            for (int i = 0; i < image.fileCount(); i++)
                verified += image.readSealed(i, scratch, 0);
            assertEquals(1_000_000_000L, verified);
            assertEquals(count, image.fileCount());
        }
    }
}
