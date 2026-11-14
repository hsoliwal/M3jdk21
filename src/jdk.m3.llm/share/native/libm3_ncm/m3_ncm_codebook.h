/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_NCM_CODEBOOK_H
#define M3_NCM_CODEBOOK_H

#include <stddef.h>
#include <stdint.h>

/* Binary64 offset-batch entry of the single jdk.m3.llm NCM native owner (library
 * m3_ncm), serving jdk.m3.precompute.NativePrecomputeOps. Moved here from the
 * m3_precompute library; symbols carry the m3_ncm_codebook_ prefix so they do not
 * collide with the float entries in m3_ncm_kernel.h. */

typedef enum {
  M3_NCM_OK = 0,
  M3_NCM_INVALID_ARGUMENT = 1,
  M3_NCM_OUT_OF_RANGE = 2,
  M3_NCM_NONFINITE_INPUT = 3,
  M3_NCM_DISTANCE_OVERFLOW = 4
} m3_ncm_status;

/* Check shape, offsets, lengths and multiplication bounds without reading payloads. */
m3_ncm_status m3_ncm_codebook_validate_layout(
    size_t vector_length, int32_t vector_offset, int32_t rows,
    size_t codebook_length, int32_t codebook_offset, int32_t code_count,
    int32_t dimensions, size_t output_length);

/*
 * Select the lowest local code index attaining the least squared distance.
 * Inputs are flat row-major arrays and are never modified or retained.
 * Only selected ranges must contain finite values. Empty selected ranges may
 * use NULL pointers; a zero-row result may use a NULL output pointer.
 * Output storage must not overlap either input and must have exactly rows slots.
 * Callers must not mutate input arrays during the call.
 *
 * Compile with strict binary64 semantics (no fast-math or contraction); execution
 * requires the default round-to-nearest and gradual-underflow environment used by
 * the JVM. When FLT_EVAL_METHOD is zero, strict separate binary64 operations use
 * ordinary double intermediates; other evaluation methods retain the original
 * volatile store-rounding path. No contraction or reassociation is permitted.
 * Invalid layout or nonfinite input is rejected before output is written.
 * On distance overflow, output may be partial and must be discarded; the JNI
 * bridge keeps its fresh result private until success and throws on failure.
 *
 * This is a checked numerical vector-quantization primitive, not a learned model
 * or an exact text-storage codec. Codebook/checkpoint/level identity is external.
 */
m3_ncm_status m3_ncm_codebook_nearest(
    const double *vectors, size_t vector_length, int32_t vector_offset, int32_t rows,
    const double *codebook, size_t codebook_length, int32_t codebook_offset,
    int32_t code_count, int32_t dimensions, int32_t *output, size_t output_length);

#endif
