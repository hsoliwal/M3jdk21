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

extern "C" int m3_ncm_nearest(
        const float *vectors, size_t vector_count, size_t rows, size_t channels,
        const float *codebook, size_t codebook_count, size_t codebook_size,
        int *output, size_t output_count) {
    if (channels == 0 || codebook_size == 0 || codebook_size > static_cast<size_t>(INT_MAX)
            || rows > std::numeric_limits<size_t>::max() / channels
            || codebook_size > std::numeric_limits<size_t>::max() / channels
            || vector_count != rows * channels || codebook_count != codebook_size * channels
            || output_count != rows || codebook == nullptr
            || (rows != 0 && (vectors == nullptr || output == nullptr))) {
        return 1;
    }
    for (size_t i = 0; i < vector_count; ++i) {
        if (!std::isfinite(vectors[i])) {
            return 2;
        }
    }
    for (size_t i = 0; i < codebook_count; ++i) {
        if (!std::isfinite(codebook[i])) {
            return 2;
        }
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
