/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include "m3_ncm_codebook.h"

#include <float.h>
#include <math.h>

/* MinGW's isfinite macro dispatches on sizeof through a float branch that trips
 * -Wfloat-conversion under the owner's -Werror; the GCC/Clang builtin has the
 * same C99 classification semantics. */
#if defined(__GNUC__) || defined(__clang__)
#define M3_NCM_ISFINITE(x) __builtin_isfinite(x)
#else
#define M3_NCM_ISFINITE(x) isfinite(x)
#endif

#ifdef __FAST_MATH__
#error "NCM codebook parity requires strict floating-point compilation"
#endif

_Static_assert(FLT_RADIX == 2 && DBL_MANT_DIG == 53 && DBL_MAX_EXP == 1024
    && DBL_MIN_EXP == -1021 && sizeof(double) == 8,
    "NCM codebook requires IEEE-754 binary64 double");

/* The strict build disables contraction and reassociation. With no excess
 * evaluation precision, binary64 intermediates can remain in registers. Other
 * evaluation methods retain the original volatile store-rounding path. */
#if defined(FLT_EVAL_METHOD) && FLT_EVAL_METHOD == 0
typedef double m3_ncm_intermediate;
#else
typedef volatile double m3_ncm_intermediate;
#endif

static int range_fits(size_t length, int32_t offset, int32_t rows, int32_t dimensions) {
  if (offset < 0 || (size_t) offset > length) return 0;
  return (size_t) rows <= (length - (size_t) offset) / (size_t) dimensions;
}

m3_ncm_status m3_ncm_codebook_validate_layout(
    size_t vector_length, int32_t vector_offset, int32_t rows,
    size_t codebook_length, int32_t codebook_offset, int32_t code_count,
    int32_t dimensions, size_t output_length) {
  if (rows < 0 || code_count < 0 || dimensions <= 0
      || (rows > 0 && code_count == 0)) return M3_NCM_INVALID_ARGUMENT;
  if (!range_fits(vector_length, vector_offset, rows, dimensions)
      || !range_fits(codebook_length, codebook_offset, code_count, dimensions)) {
    return M3_NCM_OUT_OF_RANGE;
  }
  if (output_length != (size_t) rows) return M3_NCM_INVALID_ARGUMENT;
  return M3_NCM_OK;
}

m3_ncm_status m3_ncm_codebook_nearest(
    const double *vectors, size_t vector_length, int32_t vector_offset, int32_t rows,
    const double *codebook, size_t codebook_length, int32_t codebook_offset,
    int32_t code_count, int32_t dimensions, int32_t *output, size_t output_length) {
  m3_ncm_status status = m3_ncm_codebook_validate_layout(vector_length, vector_offset, rows,
      codebook_length, codebook_offset, code_count, dimensions, output_length);
  if (status != M3_NCM_OK) return status;
  if ((rows > 0 && (vectors == NULL || output == NULL))
      || (code_count > 0 && codebook == NULL)) return M3_NCM_INVALID_ARGUMENT;

  const size_t dimension_count = (size_t) dimensions;
  const size_t vector_span = (size_t) rows * dimension_count;
  const size_t codebook_span = (size_t) code_count * dimension_count;
  for (size_t i = 0; i < vector_span; i++) {
    if (!M3_NCM_ISFINITE(vectors[(size_t) vector_offset + i])) return M3_NCM_NONFINITE_INPUT;
  }
  for (size_t i = 0; i < codebook_span; i++) {
    if (!M3_NCM_ISFINITE(codebook[(size_t) codebook_offset + i])) return M3_NCM_NONFINITE_INPUT;
  }

  size_t vector_base = (size_t) vector_offset;
  for (int32_t row = 0; row < rows; row++, vector_base += dimension_count) {
    int32_t best_code = 0;
    double best_distance = INFINITY;
    size_t code_base = (size_t) codebook_offset;
    for (int32_t code = 0; code < code_count; code++, code_base += dimension_count) {
      m3_ncm_intermediate distance = 0.0;
      for (size_t dimension = 0; dimension < dimension_count; dimension++) {
        m3_ncm_intermediate difference = vectors[vector_base + dimension] - codebook[code_base + dimension];
        m3_ncm_intermediate square = difference * difference;
        distance = distance + square;
      }
      if (!M3_NCM_ISFINITE(distance)) return M3_NCM_DISTANCE_OVERFLOW;
      if (distance < best_distance) {
        best_distance = distance;
        best_code = code;
      }
    }
    output[row] = best_code;
  }
  return M3_NCM_OK;
}
