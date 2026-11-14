/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary The installed jdk.m3.io image loads all three PMem JNI libraries
 * @requires (os.family == "linux") & (os.arch == "amd64" | os.arch == "x86_64")
 * @modules jdk.m3.io/jdk.m3.optane jdk.m3.io/jdk.internal.m3.optane
 * @run main/othervm -Xcheck:jni PmemNativePackaging
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import jdk.internal.m3.optane.PmemAccelerator;
import jdk.m3.optane.MappedReadAdvice;
import jdk.m3.optane.PmemAccess;
import jdk.m3.optane.PmemBackend;
import jdk.m3.optane.PmemRegion;
import jdk.m3.optane.PmemSdk;

/** Installed-image smoke; no Optane, DAX or power-loss claim. */
public final class PmemNativePackaging {
    private PmemNativePackaging() { }

    public static void main(String[] args) throws Exception {
        if (!MappedReadAdvice.nativeStatus().equals("available; ABI=1")) {
            throw new AssertionError("m3optane not installed: " + MappedReadAdvice.nativeStatus());
        }
        if (!PmemSdk.capabilities().nativeAvailable()) {
            throw new AssertionError("m3pmemsdk not installed");
        }

        Path file = Files.createTempFile("m3-pmem-native-image-", ".bin");
        byte[] weights = {1, -2, 3, 4, 5, -6};
        try {
            Files.write(file, new byte[weights.length]);
            try (PmemRegion region = PmemSdk.open(file, 0, weights.length,
                    PmemAccess.READ_WRITE, PmemBackend.POSIX)) {
                byte[] actual = new byte[weights.length];
                region.read(0, actual, 0, actual.length);
                if (!Arrays.equals(new byte[weights.length], actual)) {
                    throw new AssertionError("POSIX mapped read");
                }
                region.write(0, weights, 0, weights.length);
                region.persist(0, weights.length);
            }
            try (PmemRegion region = PmemSdk.open(file, 0, weights.length,
                    PmemAccess.READ_ONLY, PmemBackend.FILE)) {
                byte[] actual = new byte[weights.length];
                region.read(0, actual, 0, actual.length);
                if (!Arrays.equals(weights, actual)) throw new AssertionError("FILE reopen");
            }
            try (PmemAccelerator.Mapping map = PmemAccelerator.open(file, false)) {
                float[] actual = map.matVecQ8(0, 2, 3, 0.5f, new float[]{2f, 3f, 4f});
                // Hand-derived: (2 - 6 + 12)/2 = 4; (8 + 15 - 24)/2 = -0.5.
                if (actual.length != 2
                        || Float.floatToRawIntBits(actual[0]) != Float.floatToRawIntBits(4f)
                        || Float.floatToRawIntBits(actual[1]) != Float.floatToRawIntBits(-0.5f)) {
                    throw new AssertionError("installed Q8 kernel: " + Arrays.toString(actual));
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("PMEM_NATIVE_IMAGE_PASS libraries=3 backend=POSIX q8_rows=2");
    }
}
