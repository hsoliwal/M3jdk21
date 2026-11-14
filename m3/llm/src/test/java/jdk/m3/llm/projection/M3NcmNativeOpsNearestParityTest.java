// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.projection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Pins nearest-code parity after the NCM JNI moved into the single jdk.m3.llm
 * native owner (m3/llm/src/main/native, library m3_ncm), whose nearest entry
 * delegates to the shared m3_ncm_nearest kernel.
 *
 * <p>The golden digest and row count were produced by the received standalone
 * kernel (m3/llm/native/ncm_jni.c at eb33bbe48566, library m3_llm_ncm, FP32
 * expanded-norm distances) on this fixed corpus, before that copy was removed.
 * The corpus mixes seeded Gaussian floats with exactly representable 1/128 grid
 * values. Near-tie inputs outside this corpus can rank differently, because the
 * shared kernel accumulates binary64 ordered differences.
 */
final class M3NcmNativeOpsNearestParityTest {
    private static final long SEED = 0x4e434d50L;
    private static final int CASES = 384;
    private static final int GOLDEN_ROWS = 2307;
    private static final String GOLDEN_SHA256 =
            "2a0b9febe55e87580627af942afa897215f4cee3284ebac21f26ea6638040f28";

    @Test void routedNearestMatchesTheReceivedKernelOnTheFixedCorpus() throws NoSuchAlgorithmException {
        if (Boolean.getBoolean("ncm.test.native.required")) assertTrue(M3NcmNativeOps.available());
        var mode = M3NcmNativeOps.available()
                ? M3NcmNativeOps.Mode.NATIVE_REQUIRED : M3NcmNativeOps.Mode.AUTO;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(SEED);
        int rows = 0;
        for (int sample = 0; sample < CASES; sample++) {
            int channels = 1 + random.nextInt(24), vocabulary = 1 + random.nextInt(96);
            int count = 1 + random.nextInt(12);
            boolean gaussian = (sample & 1) == 0;
            float[] vectors = values(random, count * channels, gaussian);
            float[] codebook = values(random, vocabulary * channels, gaussian);
            int[] selected = M3NcmNativeOps.nearest(vectors, channels, codebook, mode);
            assertArrayEquals(M3NcmOps.nearest(vectors, channels, codebook), selected);
            ByteBuffer bytes = ByteBuffer.allocate(Integer.BYTES * selected.length);
            for (int id : selected) bytes.putInt(id);
            digest.update(bytes.array());
            rows += selected.length;
        }
        assertEquals(GOLDEN_ROWS, rows);
        assertEquals(GOLDEN_SHA256, HexFormat.of().formatHex(digest.digest()));
    }

    private static float[] values(Random random, int count, boolean gaussian) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = gaussian ? (float) random.nextGaussian() : (random.nextInt(2001) - 1000) / 128f;
        }
        return result;
    }
}
