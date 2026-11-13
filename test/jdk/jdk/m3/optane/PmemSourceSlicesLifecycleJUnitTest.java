// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.optane;

import jdk.m3.optane.*;
import java.nio.file.*;
import java.security.MessageDigest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/src/junit/java/test/PmemSourceSlicesLifecycleJUnitTest.java blob 6e1f6c5f9d4f; recipe make/m3/optane/recipe/pmem-source-image-20261106/relocate.py v3
final class PmemSourceSlicesLifecycleJUnitTest {
    @TempDir Path temporary;
    @Test void emptyReadStillRequiresAnOpenRegionLease() throws Exception {
        Path file = temporary.resolve("empty-image");
        Files.write(file, new byte[] {0});
        PmemRegion region = PmemSdk.open(file, 0, 1, PmemAccess.READ_ONLY, PmemBackend.POSIX);
        try {
            PmemSourceSlices sources = new PmemSourceSlices(region, new long[] {0}, new int[] {0},
                    MessageDigest.getInstance("SHA-256").digest(new byte[0]));
            assertArrayEquals(new byte[0], sources.readSealed(0));
            region.close();
            assertThrows(IllegalStateException.class, () -> sources.readSealed(0));
        } finally { region.close(); }
    }
}
