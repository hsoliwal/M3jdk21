/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
#ifndef M3_REGEX_EFFECTS_CUDA_H
#define M3_REGEX_EFFECTS_CUDA_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

enum {
    M3_REGEX_CUDA_OK = 0,
    M3_REGEX_CUDA_UNAVAILABLE = -1,
    M3_REGEX_CUDA_NOMEM = -2,
    M3_REGEX_CUDA_GEOMETRY = -3,
    M3_REGEX_CUDA_RUNTIME = -4
};

/* The owning JNI layer independently validates every lane and CSR coordinate.
 * Preparation copies only immutable numeric effects and coordinates to the device.
 * The open prefix seed and two suffix acceptance masks are derived once by that owner.
 * Neither the host caller's buffers nor any canonical character payload is retained. */
int m3_regex_cuda_prepare(const uint64_t *lanes, size_t lane_count,
        const uint64_t *dot_rows, size_t states, uint64_t seed, int32_t seed_high,
        uint64_t suffix_without_high, uint64_t suffix_with_high, uint64_t affix_length,
        const int32_t *tuples, size_t tuple_count, const int32_t *offsets, size_t offset_count,
        void **prepared);

/* One bounded call executes at most 16384 rows. The provider serializes reuse of its
 * resident workspace and synchronizes only its private stream before returning truth. */
int m3_regex_cuda_evaluate(void *prepared, int32_t start, int32_t end, int8_t *answers);
void m3_regex_cuda_close(void *prepared);

#ifdef __cplusplus
}
#endif
#endif
