// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemSdk;
import jdk.m3.optane.PmemSourceImage;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Exact original-source verification with a single reusable scratch buffer. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/campaign/src/test/java/com/synexia/m3/pmem/campaign/PmemStreamingSealJUnitTest.java blob e36991b90bd6; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class PmemStreamingSealJUnitTest {
    @TempDir Path temporary;
    private static final PmemSourceImage.Limits LIMITS =
            new PmemSourceImage.Limits(3, 100_000L, 60_000, 4096);

    private Path snapshot() throws Exception {
        Path root = Files.createDirectory(temporary.resolve("source"));
        byte[] large = new byte[50_000];
        Arrays.fill(large, (byte) 'a');
        Files.write(root.resolve("A.java"), large);
        Files.write(root.resolve("B.java"), new byte[0]);
        Files.write(root.resolve("C.java"), new byte[]{0, 1, (byte) 255, 0, 1});
        Path packed = temporary.resolve("snapshot");
        PmemSourceImage.pack(root, List.of("C.java", "A.java", "B.java"), packed, LIMITS);
        return packed;
    }

    @Test void reusableWindowAndEmptySourceMatchOriginalSealedReads() throws Exception {
        Path packed = snapshot();
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, LIMITS)) {
            byte[] scratch = new byte[1024];
            assertEquals(50_000, image.verifySealed(0, scratch));
            assertEquals(0, image.verifySealed(1, scratch));
            assertEquals(5, image.verifySealed(2, scratch));
            assertEquals(50_000, image.readSealed(0).length, "original API must be unchanged");
            assertEquals(0, image.readSealed(1).length);
            assertThrows(IllegalArgumentException.class, () -> image.verifySealed(0, new byte[0]));
            assertThrows(IllegalArgumentException.class,
                    () -> image.verifySealed(0, new byte[16 * 1024 * 1024 + 1]));
            assertThrows(IndexOutOfBoundsException.class, () -> image.verifySealed(-1, scratch));
            assertThrows(IndexOutOfBoundsException.class, () -> image.verifySealed(3, scratch));
        }
    }

    @Test void sameSizeExternalMutationRefusesOriginalSourceIdentity() throws Exception {
        Path packed = snapshot();
        try (PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, LIMITS)) {
            byte[] scratch = new byte[7];
            assertEquals(50_000, image.verifySealed(0, scratch));
            try (FileChannel channel = FileChannel.open(
                    packed.resolve("source.bin"), StandardOpenOption.WRITE)) {
                assertEquals(1, channel.write(ByteBuffer.wrap(new byte[]{'b'}), 4096));
                channel.force(true);
            }
            assertThrows(SecurityException.class, () -> image.verifySealed(0, scratch));
            assertEquals(0, image.verifySealed(1, scratch));
            assertEquals(5, image.verifySealed(2, scratch));
        }
    }

    @Test void optionalNativePosixAndFileDigestReceiptsAreIdentical() throws Exception {
        if (!Boolean.getBoolean("pmem.streaming.native")) return;
        assertTrue(PmemSdk.capabilities().nativeAvailable(), "explicit native qualification requires JNI");
        Path packed = snapshot();
        try (PmemSourceImage file = PmemSourceImage.open(packed, PmemBackend.FILE, LIMITS);
             PmemSourceImage nativeImage = PmemSourceImage.open(packed, PmemBackend.POSIX, LIMITS)) {
            byte[] scratch = new byte[8192];
            for (int i = 0; i < 3; i++) {
                assertEquals(file.verifySealed(i, scratch), nativeImage.verifySealed(i, scratch));
                assertEquals(file.sha256At(i), nativeImage.sha256At(i));
            }
        }
    }

    @Test void closedImageRefusesEvenEmptySourceVerification() throws Exception {
        Path packed = snapshot();
        PmemSourceImage image = PmemSourceImage.open(packed, PmemBackend.FILE, LIMITS);
        image.close();
        assertThrows(IllegalStateException.class, () -> image.verifySealed(1, new byte[8192]));
    }
}
