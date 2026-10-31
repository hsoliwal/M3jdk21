/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel.testing;

import com.m3.parallel.IntBuffer;
import com.m3.parallel.JavaKernel;
import com.m3.parallel.JavaKernels;
import com.m3.parallel.KernelId;

/** Small kernels used by the m3.parallel tests. Each guards its own logical bound. */
public final class TestKernels {
    /** {@code out[i] = a[i] + b[i]} for {@code i < config[0]}; buffers: a, b, out, config. */
    public static final KernelId ADD = JavaKernels.register(KernelId.of("test.add"), TestKernels::add);
    /** {@code state[i] += config[1]} for {@code i < config[0]}; buffers: state, config. */
    public static final KernelId ACCUMULATE =
            JavaKernels.register(KernelId.of("test.accumulate"), TestKernels::accumulate);
    /** {@code out[i] = i + 1} for every launched work-item, padded ones included; buffers: out. */
    public static final KernelId TOUCH = JavaKernels.register(KernelId.of("test.touch"), TestKernels::touch);
    /** Throws at work-item 7; buffers: any. */
    public static final KernelId FAIL = JavaKernels.register(KernelId.of("test.fail"), TestKernels::fail);
    /** A kernel id with no Java body. */
    public static final KernelId MISSING = KernelId.of("test.missing");
    /** The {@link #ADD} body, for registration identity tests. */
    public static final JavaKernel ADD_BODY = JavaKernels.lookup(ADD).orElseThrow();

    private TestKernels() {
    }

    private static void add(int index, IntBuffer... buffers) {
        if (index >= buffers[3].get(0)) {
            return;
        }
        buffers[2].set(index, buffers[0].get(index) + buffers[1].get(index));
    }

    private static void accumulate(int index, IntBuffer... buffers) {
        if (index >= buffers[1].get(0)) {
            return;
        }
        buffers[0].set(index, buffers[0].get(index) + buffers[1].get(1));
    }

    private static void touch(int index, IntBuffer... buffers) {
        buffers[0].set(index, index + 1);
    }

    private static void fail(int index, IntBuffer... buffers) {
        if (index == 7) {
            throw new IllegalStateException("work-item 7 failed");
        }
    }
}
