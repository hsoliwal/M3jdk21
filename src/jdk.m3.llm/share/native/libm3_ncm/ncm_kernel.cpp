/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_ncm_kernel.h"

#include <climits>
#include <cmath>
#include <limits>

static_assert(sizeof(float) == 4 && std::numeric_limits<float>::is_iec559);
static_assert(sizeof(double) == 8 && std::numeric_limits<double>::is_iec559);
static_assert(sizeof(int) == 4);

namespace {
// Shared layout contract of both nearest-code entry points.
bool layout_valid(const float *vectors, size_t vector_count, size_t rows, size_t channels,
                  const float *codebook, size_t codebook_count, size_t codebook_size,
                  const int *output, size_t output_count) {
    return !(channels == 0 || codebook_size == 0 || codebook_size > static_cast<size_t>(INT_MAX)
            || rows > std::numeric_limits<size_t>::max() / channels
            || codebook_size > std::numeric_limits<size_t>::max() / channels
            || vector_count != rows * channels || codebook_count != codebook_size * channels
            || output_count != rows || codebook == nullptr
            || (rows != 0 && (vectors == nullptr || output == nullptr)));
}

bool all_finite(const float *values, size_t count) {
    for (size_t i = 0; i < count; ++i) {
        if (!std::isfinite(values[i])) {
            return false;
        }
    }
    return true;
}

int controlled_finite(const float *values, size_t count,
                      m3_ncm_checkpoint checkpoint, void *context) {
    if (checkpoint == nullptr) {
        return all_finite(values, count) ? 0 : 2;
    }
    if (checkpoint(context) != 0) {
        return 4;
    }
    for (size_t i = 0; i < count; ++i) {
        if ((i & static_cast<size_t>(65535)) == static_cast<size_t>(65535)
                && checkpoint(context) != 0) {
            return 4;
        }
        if (!std::isfinite(values[i])) {
            return 2;
        }
    }
    return 0;
}
}

extern "C" int m3_ncm_nearest(
        const float *vectors, size_t vector_count, size_t rows, size_t channels,
        const float *codebook, size_t codebook_count, size_t codebook_size,
        int *output, size_t output_count) {
    if (!layout_valid(vectors, vector_count, rows, channels, codebook, codebook_count,
                      codebook_size, output, output_count)) {
        return 1;
    }
    if (!all_finite(vectors, vector_count) || !all_finite(codebook, codebook_count)) {
        return 2;
    }
    for (size_t row = 0; row < rows; ++row) {
        double best_distance = std::numeric_limits<double>::infinity();
        size_t best = 0;
        const size_t input_base = row * channels;
        for (size_t code = 0; code < codebook_size; ++code) {
            const size_t code_base = code * channels;
            double distance = 0.0;
            for (size_t channel = 0; channel < channels; ++channel) {
                const double difference = static_cast<double>(vectors[input_base + channel])
                        - static_cast<double>(codebook[code_base + channel]);
                const double square = difference * difference;
                distance = distance + square;
                // Finite squared terms are nonnegative; later codes lose ties.
                // Validation above still covers every component of every code.
                if (distance >= best_distance) {
                    break;
                }
            }
            if (distance < best_distance) {
                best_distance = distance;
                best = code;
                if (best_distance == 0.0) {
                    break;
                }
            }
        }
        output[row] = static_cast<int>(best);
    }
    return 0;
}

extern "C" int m3_ncm_nearest_expanded_f32(
        const float *vectors, size_t vector_count, size_t rows, size_t channels,
        const float *codebook, size_t codebook_count, size_t codebook_size,
        float *code_norms, size_t code_norm_count, int *output, size_t output_count) {
    return m3_ncm_nearest_expanded_f32_controlled(vectors, vector_count, rows, channels,
            codebook, codebook_count, codebook_size, code_norms, code_norm_count,
            output, output_count, nullptr, nullptr);
}

extern "C" int m3_ncm_nearest_expanded_f32_controlled(
        const float *vectors, size_t vector_count, size_t rows, size_t channels,
        const float *codebook, size_t codebook_count, size_t codebook_size,
        float *code_norms, size_t code_norm_count, int *output, size_t output_count,
        m3_ncm_checkpoint checkpoint, void *context) {
    if (!layout_valid(vectors, vector_count, rows, channels, codebook, codebook_count,
                      codebook_size, output, output_count)
            || code_norms == nullptr || code_norm_count != codebook_size) {
        return 1;
    }
    int status = controlled_finite(vectors, vector_count, checkpoint, context);
    if (status != 0) return status;
    status = controlled_finite(codebook, codebook_count, checkpoint, context);
    if (status != 0) return status;
    // Same operation order as M3NcmOps.nearest: every code norm first, then per row
    // its norm and, per code, (rowNorm + codeNorm) - 2 * dot, all in binary32.
    for (size_t code = 0; code < codebook_size; ++code) {
        if (checkpoint != nullptr && checkpoint(context) != 0) return 4;
        float sum = 0.0f;
        for (size_t channel = 0; channel < channels; ++channel) {
            const float value = codebook[code * channels + channel];
            sum = sum + value * value;
        }
        if (!std::isfinite(sum)) {
            return 3;
        }
        code_norms[code] = sum;
    }
    for (size_t row = 0; row < rows; ++row) {
        if (checkpoint != nullptr && checkpoint(context) != 0) return 4;
        const size_t input_base = row * channels;
        float row_norm = 0.0f;
        for (size_t channel = 0; channel < channels; ++channel) {
            row_norm = row_norm + vectors[input_base + channel] * vectors[input_base + channel];
        }
        if (!std::isfinite(row_norm)) {
            return 3;
        }
        float best_distance = std::numeric_limits<float>::infinity();
        size_t best = 0;
        for (size_t code = 0; code < codebook_size; ++code) {
            const size_t code_base = code * channels;
            float dot = 0.0f;
            for (size_t channel = 0; channel < channels; ++channel) {
                dot = dot + vectors[input_base + channel] * codebook[code_base + channel];
            }
            const float distance = (row_norm + code_norms[code]) - 2.0f * dot;
            if (!std::isfinite(distance)) {
                return 3;
            }
            if (distance < best_distance) {
                best_distance = distance;
                best = code;
            }
        }
        output[row] = static_cast<int>(best);
    }
    return 0;
}
