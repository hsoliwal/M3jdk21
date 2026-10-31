/* SPDX-License-Identifier: Apache-2.0 */

/**
 * RV32I 2.1 + M 2.0 virtual cores (plus FENCE/FENCE.I no-ops, ECALL/EBREAK traps and the custom {@code XM3_HALT}
 * semihosting stop) executed one core per work-item through the M3 parallel API.
 *
 * <p>{@link com.m3.vcpu.CpuRv32iEngine} is the sequential oracle. {@link com.m3.vcpu.Rv32iKernel} is the
 * {@code m3.rv32im.slice} kernel; {@link com.m3.vcpu.Rv32iSession} keeps state resident across slices; and
 * {@link com.m3.vcpu.Rv32iBulkExecutor} issues content-addressed {@link com.m3.vcpu.Rv32iBulkReceipt}s, admitting a
 * backend run only after full parity with the oracle.</p>
 */
package com.m3.vcpu;
