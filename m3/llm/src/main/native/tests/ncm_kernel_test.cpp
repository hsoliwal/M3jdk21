/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_ncm_kernel.h"

#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <limits>
#include <vector>

namespace {
void require(bool condition, const char *message) {
    if (!condition) {
        std::fprintf(stderr, "FAIL: %s\n", message);
        std::exit(1);
    }
}

std::uint32_t next(std::uint32_t &state) {
    state ^= state << 13;
    state ^= state >> 17;
    state ^= state << 5;
    return state;
}
}

int main() {
    const float books[] = {-1.0F, 1.0F};
    const float query[] = {0.0F};
    int out[] = {-77};
    require(m3_ncm_nearest(query, 1, 1, 1, books, 2, 2, out, 1) == 0
            && out[0] == 0, "equal distances select first code");
    const float zeros[] = {-0.0F, 0.0F};
    require(m3_ncm_nearest(query, 1, 1, 1, zeros, 2, 2, out, 1) == 0
            && out[0] == 0, "signed-zero tie");
    require(m3_ncm_nearest(nullptr, 0, 0, 1, books, 2, 2, nullptr, 0) == 0,
            "empty batch");
    out[0] = -77;
    require(m3_ncm_nearest(nullptr, 1, 1, 1, books, 2, 2, out, 1) == 1,
            "null nonempty input refused");
    require(m3_ncm_nearest(query, 1, 1, 0, books, 2, 2, out, 1) == 1,
            "zero channels refused");
    require(m3_ncm_nearest(query, 1, std::numeric_limits<size_t>::max(),
            2, books, 2, 1, out, 1) == 1, "dimension overflow refused");
    require(m3_ncm_nearest(query, 1, 1, 1, books, 1, 2, out, 1) == 1,
            "short codebook refused");
    require(m3_ncm_nearest(query, 1, 1, 1, books, 2, 2, out, 0) == 1,
            "short output refused");
    const float nan_query[] = {std::numeric_limits<float>::quiet_NaN()};
    require(m3_ncm_nearest(nan_query, 1, 1, 1, books, 2, 2, out, 1) == 2,
            "NaN refused");
    const float infinite_book[] = {std::numeric_limits<float>::infinity()};
    require(m3_ncm_nearest(query, 1, 1, 1, infinite_book, 1, 1, out, 1) == 2,
            "infinity refused");
    require(out[0] == -77, "validation never mutates output");
    const float zero_vector[] = {0, 0, 0};
    const float late_nan[] = {0, 0, 0, 100, 0, std::numeric_limits<float>::quiet_NaN()};
    require(m3_ncm_nearest(zero_vector, 3, 1, 3, late_nan, 6, 2, out, 1) == 2
            && out[0] == -77, "zero-distance pruning cannot bypass codebook validation");
    const float partial_tie[] = {1, 0, 0, 0, 1, 0, 0, 0, 0.5F};
    require(m3_ncm_nearest(zero_vector, 3, 1, 3, partial_tie, 9, 3, out, 1) == 0
            && out[0] == 2, "a pruned equal partial sum cannot hide a later better code");
    const float extremes[] = {-std::numeric_limits<float>::max(), std::numeric_limits<float>::max()};
    require(m3_ncm_nearest(extremes + 1, 1, 1, 1, extremes, 2, 2, out, 1) == 0
            && out[0] == 1, "finite extreme values avoid float distance overflow");

    std::uint32_t state = 0x5a71c39bU;
    for (int trial = 0; trial < 1200; ++trial) {
        const size_t rows = next(state) % 13U;
        const size_t channels = 1U + next(state) % 19U;
        const size_t count = 1U + next(state) % 31U;
        std::vector<float> inputs(rows * channels);
        std::vector<float> codebook(count * channels);
        for (float &value : inputs) {
            value = static_cast<float>(static_cast<int>(next(state) % 255U) - 127) / 8.0F;
        }
        for (float &value : codebook) {
            value = static_cast<float>(static_cast<int>(next(state) % 255U) - 127) / 8.0F;
        }
        std::vector<int> selected(rows, -1);
        require(m3_ncm_nearest(inputs.data(), inputs.size(), rows, channels,
                codebook.data(), codebook.size(), count, selected.data(), selected.size()) == 0,
                "randomized invocation");
        for (size_t row = 0; row < rows; ++row) {
            long double minimum = std::numeric_limits<long double>::infinity();
            int expected = -1;
            for (size_t code = 0; code < count; ++code) {
                long double distance = 0;
                for (size_t channel = 0; channel < channels; ++channel) {
                    const long double delta = static_cast<long double>(inputs[row * channels + channel])
                            - static_cast<long double>(codebook[code * channels + channel]);
                    distance += delta * delta;
                }
                if (distance < minimum) {
                    minimum = distance;
                    expected = static_cast<int>(code);
                }
            }
            require(selected[row] == expected, "independent extended-precision bounded oracle");
        }
    }
    std::puts("NCM C ABI oracle: 1200 randomized cases and boundary cases passed");
    return 0;
}
