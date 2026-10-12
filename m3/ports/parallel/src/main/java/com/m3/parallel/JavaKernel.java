/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

/**
 * Pure-Java kernel body executed by the CPU reference backend, one call per work-item.
 *
 * <p>Implementations are static, stateless functions: they read and write only the buffers passed in, write only
 * elements owned by {@code index}, and guard their own logical bound because padded work-items also run (see
 * {@link Grid1D}). The {@code buffers} array is shared by every call of one launch and must not be modified.</p>
 */
@FunctionalInterface
public interface JavaKernel {
    /**
     * Executes one work-item.
     *
     * @param index   global work-item index in {@code [0, Grid1D.launchSize())}
     * @param buffers device buffers in binding order
     */
    void run(int index, IntBuffer... buffers);
}
