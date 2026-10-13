/* SPDX-License-Identifier: Apache-2.0 */

/**
 * M3 parallel API: launch one-dimensional, embarrassingly parallel kernels over off-heap {@code int} buffers.
 *
 * <p>Typical use:</p>
 * <pre>{@code
 * Backend backend = Backend.select(Backend.Policy.BEST);
 * try (Plan plan = Plan.builder(backend)
 *         .kernel(kernelId)
 *         .grid(new Grid1D(n, 64))
 *         .bind(resident, Transfer.ONCE)
 *         .bind(config, Transfer.EACH_LAUNCH)
 *         .bind(output, Transfer.ON_DEMAND)
 *         .build()) {
 *     plan.execute().download(output);
 * }
 * }</pre>
 *
 * <p>Kernels are named by {@link com.m3.parallel.KernelId}. The pure-Java CPU reference backend executes the
 * {@link com.m3.parallel.JavaKernel} registered in {@link com.m3.parallel.JavaKernels} under that id; native
 * backends execute their own precompiled kernel for the same id. {@link com.m3.parallel.Verified} runs a plan on its
 * backend and on the CPU reference and admits the result only on full-buffer equality.</p>
 */
package com.m3.parallel;
