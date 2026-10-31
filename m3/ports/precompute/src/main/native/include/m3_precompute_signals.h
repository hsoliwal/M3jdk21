/* SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_PRECOMPUTE_SIGNALS_H
#define M3_PRECOMPUTE_SIGNALS_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Candidate/routing evidence only; never Java syntax or regex validity proof. */
uint64_t m3_precompute_code_text_signal(const uint16_t *units, size_t length);
int m3_precompute_similarity_score(
        int query_length, uint64_t query_hash, int candidate_length, uint64_t candidate_hash);

#ifdef __cplusplus
}
#endif

#endif
