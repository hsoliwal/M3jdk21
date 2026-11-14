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
 * native owner (m3/llm/src/main/native, library m3_ncm). Native nearest uses the
 * owner kernel's FP32 expanded-norm entry, so every case must equal Mode.JAVA.
 *
 * <p>The golden digests and row counts were produced by the received standalone
 * kernel (m3/llm/native/ncm_jni.c at eb33bbe48566, library m3_llm_ncm) on these
 * fixed corpora, before that copy was removed. The general corpus mixes seeded
 * Gaussian floats with exactly representable 1/128 grid values; the near-tie
 * corpus places every code within about 1e-3 of an input row.
 */
final class NcmNativeOpsNearestParityTest {
    private static final int CASES = 384;
    private static final long GENERAL_SEED = 0x4e434d50L;
    private static final int GENERAL_ROWS = 2307;
    private static final String GENERAL_SHA256 =
            "2a0b9febe55e87580627af942afa897215f4cee3284ebac21f26ea6638040f28";
    private static final long NEAR_TIE_SEED = 0x4e434d54L;
    private static final int NEAR_TIE_ROWS = 2655;
    private static final String NEAR_TIE_SHA256 =
            "2cd85b2cdf11f1bd26bf34c91dcb9d62f0457b6b3433b49593219e58c9d963f3";

    @Test void generalCorpusMatchesJavaAndTheReceivedKernel() throws NoSuchAlgorithmException {
        assertCorpus(GENERAL_SEED, false, GENERAL_ROWS, GENERAL_SHA256);
    }

    @Test void nearTieCorpusMatchesJavaAndTheReceivedKernel() throws NoSuchAlgorithmException {
        assertCorpus(NEAR_TIE_SEED, true, NEAR_TIE_ROWS, NEAR_TIE_SHA256);
    }

    private static void assertCorpus(long seed, boolean nearTie, int goldenRows, String goldenSha256)
            throws NoSuchAlgorithmException {
        if (Boolean.getBoolean("ncm.test.native.required")) assertTrue(M3NcmNativeOps.available());
        var mode = M3NcmNativeOps.available()
                ? M3NcmNativeOps.Mode.NATIVE_REQUIRED : M3NcmNativeOps.Mode.AUTO;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Random random = new Random(seed);
        int rows = 0;
        for (int sample = 0; sample < CASES; sample++) {
            int channels = 1 + random.nextInt(24), vocabulary = 1 + random.nextInt(96);
            int count = 1 + random.nextInt(12);
            boolean gaussian = nearTie || (sample & 1) == 0;
            float[] vectors = values(random, count * channels, gaussian);
            float[] codebook = nearTie ? nearTies(random, vectors, count, channels, vocabulary)
                    : values(random, vocabulary * channels, gaussian);
            int[] selected = M3NcmNativeOps.nearest(vectors, channels, codebook, mode);
            assertArrayEquals(M3NcmOps.nearest(vectors, channels, codebook), selected);
            ByteBuffer bytes = ByteBuffer.allocate(Integer.BYTES * selected.length);
            for (int id : selected) bytes.putInt(id);
            digest.update(bytes.array());
            rows += selected.length;
        }
        assertEquals(goldenRows, rows);
        assertEquals(goldenSha256, HexFormat.of().formatHex(digest.digest()));
    }

    private static float[] values(Random random, int count, boolean gaussian) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = gaussian ? (float) random.nextGaussian() : (random.nextInt(2001) - 1000) / 128f;
        }
        return result;
    }

    private static float[] nearTies(Random random, float[] vectors, int count, int channels, int vocabulary) {
        float[] result = new float[vocabulary * channels];
        for (int code = 0; code < vocabulary; code++) {
            int row = random.nextInt(count) * channels;
            for (int c = 0; c < channels; c++) {
                result[code * channels + c] = vectors[row + c] + (float) (random.nextGaussian() * 1e-3);
            }
        }
        return result;
    }
}
