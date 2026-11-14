/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
#include "m3_ncm_kernel.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdio>
#include <cstdlib>
#include <limits>
#include <vector>

namespace {
size_t assertions = 0;

void require(bool condition, const char *message) {
    ++assertions;
    if (!condition) {
        std::fprintf(stderr, "controlled NCM kernel: %s\n", message);
        std::exit(1);
    }
}

struct Probe { size_t calls; size_t cancel_at; };

int checkpoint(void *context) {
    auto *probe = static_cast<Probe *>(context);
    ++probe->calls;
    return probe->calls == probe->cancel_at ? 1 : 0;
}

void cancel_every_boundary(const std::vector<float>& vectors, size_t rows, size_t channels,
                           const std::vector<float>& books, size_t vocabulary,
                           const std::vector<int>& expected, size_t expected_calls) {
    const auto original_vectors = vectors;
    const auto original_books = books;
    std::vector<float> norms(vocabulary, -999.0F);
    std::vector<int> output(rows, -999);
    Probe completed{0, 0};
    require(m3_ncm_nearest_expanded_f32_controlled(vectors.data(), vectors.size(), rows, channels,
                    books.data(), books.size(), vocabulary, norms.data(), norms.size(),
                    output.data(), output.size(), checkpoint, &completed) == 0,
            "non-cancelling callback succeeds");
    require(output == expected, "independent bounded fixture selection");
    require(completed.calls == expected_calls, "finite/norm/row checkpoint schedule");
    for (size_t cancel_at = 1; cancel_at <= expected_calls; ++cancel_at) {
        std::fill(output.begin(), output.end(), -999);
        std::fill(norms.begin(), norms.end(), -999.0F);
        Probe stopped{0, cancel_at};
        require(m3_ncm_nearest_expanded_f32_controlled(vectors.data(), vectors.size(), rows, channels,
                        books.data(), books.size(), vocabulary, norms.data(), norms.size(),
                        output.data(), output.size(), checkpoint, &stopped) == 4,
                "every requested boundary cancels with status four");
        require(stopped.calls == cancel_at, "no callbacks after cancellation");
        require(vectors == original_vectors && books == original_books, "input arrays remain unchanged");
        if (cancel_at == 1) {
            require(std::all_of(output.begin(), output.end(), [](int id) { return id == -999; }),
                    "entry cancellation does not publish output");
        }
        Probe recovery{0, 0};
        require(m3_ncm_nearest_expanded_f32_controlled(vectors.data(), vectors.size(), rows, channels,
                        books.data(), books.size(), vocabulary, norms.data(), norms.size(),
                        output.data(), output.size(), checkpoint, &recovery) == 0,
                "recovery after every cancellation");
        require(output == expected && recovery.calls == expected_calls, "recovery has no retained state");
    }
    std::fill(output.begin(), output.end(), -999);
    require(m3_ncm_nearest_expanded_f32(vectors.data(), vectors.size(), rows, channels,
                    books.data(), books.size(), vocabulary, norms.data(), norms.size(),
                    output.data(), output.size()) == 0 && output == expected,
            "original ABI delegates to the identical null-control numerical path");
}
}

int main() {
    const std::vector<float> vectors{0, 0, 1, 0, 3, 2};
    const std::vector<float> books{1, 0, -1, 0, 3, 2};
    cancel_every_boundary(vectors, 3, 2, books, 3, {0, 0, 2}, 8);

    // Two inputs cross a 65,536-element boundary without increasing norm/row counts.
    // Exact zero/one vectors make the expected answer independent of reduction details.
    constexpr size_t channels = 65536;
    std::vector<float> wide_vectors(channels, 0.0F);
    std::vector<float> wide_books(2 * channels, 1.0F);
    std::fill_n(wide_books.begin(), channels, 0.0F);
    cancel_every_boundary(wide_vectors, 1, channels, wide_books, 2, {0}, 8);

    // Keep the two mathematical profiles distinct on an expanded-norm rounding tie.
    const float row[] = {8192.0F};
    const float near[] = {8193.0F, 8192.0F};
    float norms[2] = {};
    int result = -1;
    require(m3_ncm_nearest(row, 1, 1, 1, near, 2, 2, &result, 1) == 0 && result == 1,
            "existing F64 direct-distance profile is unchanged");
    require(m3_ncm_nearest_expanded_f32_controlled(row, 1, 1, 1, near, 2, 2,
                    norms, 2, &result, 1, nullptr, nullptr) == 0 && result == 0,
            "FP32 expanded profile retains lowest-ID rounded tie");

    Probe invalid{0, 0};
    result = -99;
    require(m3_ncm_nearest_expanded_f32_controlled(row, 1, 1, 0, near, 2, 2,
                    norms, 2, &result, 1, checkpoint, &invalid) == 1,
            "invalid shape keeps original status one");
    require(invalid.calls == 0 && result == -99, "layout rejects before callbacks or writes");
    require(m3_ncm_nearest_expanded_f32_controlled(row, 1, 1, 1, near, 2, 2,
                    norms, 1, &result, 1, checkpoint, &invalid) == 1 && invalid.calls == 0,
            "invalid scratch retains original contract");
    const float nan[] = {std::numeric_limits<float>::quiet_NaN()};
    require(m3_ncm_nearest_expanded_f32_controlled(nan, 1, 1, 1, near, 2, 2,
                    norms, 2, &result, 1, nullptr, nullptr) == 2 && result == -99,
            "nonfinite input keeps original status two and unpublished output");
    const float huge[] = {std::numeric_limits<float>::max()};
    require(m3_ncm_nearest_expanded_f32_controlled(huge, 1, 1, 1, huge, 1, 1,
                    norms, 1, &result, 1, nullptr, nullptr) == 3 && result == -99,
            "expanded norm overflow keeps original status three");
    Probe zero_rows{0, 0};
    require(m3_ncm_nearest_expanded_f32_controlled(nullptr, 0, 0, 1, near, 2, 2,
                    norms, 2, nullptr, 0, checkpoint, &zero_rows) == 0 && zero_rows.calls == 4,
            "zero-row C ABI still validates and prepares the codebook");

    std::printf("NCM_CONTROLLED_KERNEL_PASS assertions=%zu\n", assertions);
    return 0;
}
