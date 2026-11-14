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

/* FP32 expanded-norm selection, the AREA_LINEAR_RVQ_V1 profile of
 * jdk.m3.llm.projection.M3NcmOps.nearest: every code norm first, then per row
 * distance = (rowNorm + codeNorm) - 2 * dot, all binary32 with channel-ascending
 * sums; the lowest index wins exact ties. Same layout contract and statuses 1/2 as
 * m3_ncm_nearest, plus 3: a nonfinite norm or distance (output must be discarded).
 * code_norms is caller scratch of exactly codebook_size floats.
 */
int m3_ncm_nearest_expanded_f32(const float *vectors, size_t vector_count,
                      size_t rows, size_t channels,
                      const float *codebook, size_t codebook_count,
                      size_t codebook_size, float *code_norms, size_t code_norm_count,
                      int *output, size_t output_count);

#ifdef __cplusplus
}
#endif
#endif
