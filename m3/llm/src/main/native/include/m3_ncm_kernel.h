/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#ifndef M3_NCM_KERNEL_H
#define M3_NCM_KERNEL_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* 0: success; 1: invalid shape/range/pointer; 2: nonfinite input.
 * No output is written on validation failure. Zero rows permit null vectors/output.
 * Arrays must have the supplied sizes. Inputs may alias; output must not overlap them.
 * Compile without fast-math and with FP contraction disabled for Java-oracle parity.
 */
int m3_ncm_nearest(const float *vectors, size_t vector_count,
                      size_t rows, size_t channels,
                      const float *codebook, size_t codebook_count,
                      size_t codebook_size, int *output, size_t output_count);

#ifdef __cplusplus
}
#endif
#endif
