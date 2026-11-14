/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Controlled FP32 NCM hierarchy retains exact semantics in its product module
 * @modules jdk.m3.llm
 * @run main/othervm -Xcheck:jni jdk.m3.llm.test.NcmHierarchyImageTest --require-native
 */
package jdk.m3.llm.test;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.llm.projection.M3NcmHierarchy;
import jdk.m3.llm.projection.M3NcmNativeOps;
import jdk.m3.llm.projection.M3NcmOps;
import jdk.m3.llm.projection.M3SemanticIdentity;

/** Standalone target test: no test-framework or external runtime dependency. */
public final class NcmHierarchyImageTest {
    private NcmHierarchyImageTest() { }

    public static void main(String[] args) {
        if (args.length > 1 || args.length == 1 && !args[0].equals("--require-native"))
            throw new IllegalArgumentException("optional --require-native only");
        boolean required = args.length == 1;
        Module module = M3NcmHierarchy.class.getModule();
        require(module.isNamed() && module.getName().equals("jdk.m3.llm"), "existing product module");
        require(module == M3SemanticIdentity.class.getModule(), "single derived-state owner");
        require(module.isExported("jdk.m3.llm.projection"), "preserved exported package");

        String source = "Exact UTF-16: \u0000 \ud800 \ud83d\ude00";
        String root = M3SemanticIdentity.hasher("image-exact-text").text(source).finish();
        require(!root.equals(M3SemanticIdentity.hasher("image-exact-text").text(source + "x").finish()),
                "derived identity detects changed exact text");
        M3NcmHierarchy hierarchy = new M3NcmHierarchy("image-fp32-v1", 1,
                new int[]{1, 2}, new float[][]{{0, 2}, {0, 1}});
        float[] latent = {2, 3};
        int[] expected = {1, 0, 1};
        require(Arrays.equals(expected, hierarchy.encode(latent, 1)), "independent code fixture");
        bits(latent, hierarchy.decode(expected, 1));
        bits(new float[]{2, 2}, hierarchy.decodePrefix(new int[]{1}, 1, 1));
        bits(new float[]{2, 2}, hierarchy.nextCondition(new int[]{1}, 1, 1));

        require(Arrays.equals(new int[]{0}, M3NcmOps.nearest(
                new float[]{8192}, 1, new float[]{8193, 8192})), "expanded FP32 profile is preserved");
        if (required) {
            require(M3NcmNativeOps.available(), "ABI1 present in installed module image");
            require(M3NcmNativeOps.controlledAvailable(), "controlled capability present");
        }
        boolean executed = M3NcmNativeOps.controlledAvailable();
        if (executed) {
            var mode = M3NcmNativeOps.Mode.NATIVE_REQUIRED;
            AtomicInteger calls = new AtomicInteger();
            M3NcmOps.Cancellation active = calls::incrementAndGet;
            require(Arrays.equals(expected, hierarchy.encode(latent, 1,
                    M3NcmOps.DEFAULT_BUDGET, active, mode)), "native hierarchy code parity");
            bits(latent, hierarchy.decode(expected, 1, M3NcmOps.DEFAULT_BUDGET, active, mode));
            bits(new float[]{2, 2}, hierarchy.decodePrefix(new int[]{1}, 1, 1,
                    M3NcmOps.DEFAULT_BUDGET, active, mode));
            bits(new float[]{2, 2}, hierarchy.nextCondition(new int[]{1}, 1, 1,
                    M3NcmOps.DEFAULT_BUDGET, active, mode));
            require(calls.get() > 0, "active cancellation checkpoints");
            var marker = new CancellationException("image cancellation");
            try {
                hierarchy.encode(latent, 1, M3NcmOps.DEFAULT_BUDGET, () -> { throw marker; }, mode);
                throw new AssertionError("cancelled hierarchy returned normally");
            } catch (CancellationException actual) {
                require(actual == marker, "original cancellation identity");
            }
            require(Arrays.equals(expected, hierarchy.encode(latent, 1, mode)), "recovery after cancellation");
        }
        bits(new float[]{2, 3}, latent);
        System.out.println("NCM_HIERARCHY_IMAGE_PASS module=jdk.m3.llm controlledNative=" + executed
                + " profile=AREA_LINEAR_RVQ_V1 exactTextRetained=true");
    }

    private static void bits(float[] expected, float[] actual) {
        require(expected.length == actual.length, "result dimensions");
        for (int i = 0; i < expected.length; i++)
            require(Float.floatToRawIntBits(expected[i]) == Float.floatToRawIntBits(actual[i]),
                    "raw FP32 result at " + i);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
