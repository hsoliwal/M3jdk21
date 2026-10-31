// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

/**
 * Pluggable accelerator backend for prepacked MIndex tensor kernels.
 *
 * <p>Backends must preserve Java reference semantics exactly. Hardware/runtime providers such as
 * LiteRT NPU integrations can be supplied with {@link java.util.ServiceLoader} without changing
 * the MIndex public contract.</p>
 */
public interface MIndexTensorBackend {
    /** Stable lowercase backend id, for example {@code java}, {@code jni}, or {@code litert-npu}. */
    String id();

    /** True when this backend can execute in the current process/environment. */
    boolean available();

    /** Executes signed-int8 matrix multiplication with signed-int32 accumulation. */
    int[] multiplyInt8(
            MIndexInt8MatmulPlan plan,
            byte[] left,
            int rows,
            int[] bias);
}
