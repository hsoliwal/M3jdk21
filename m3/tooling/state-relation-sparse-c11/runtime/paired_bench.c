/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Bounded measurement glue over existing unchanged/recipe-emitted C kernels.
 * Benchmark protocol adapts warmup/repetition/interleaving concepts only; no donor code copied.
 */
#define _POSIX_C_SOURCE 200809L
#include "m3_state_relation.h"
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define SAMPLE_COUNT 5U
#define ITERATION_CAP UINT64_C(1048576)
#define TARGET_NS UINT64_C(20000000)
#define MAX_CASES 200U

typedef int (*compose_fn)(const uint64_t *, const uint64_t *, uint64_t *, size_t);
typedef int (*repeat_fn)(const uint64_t *, uint64_t *, size_t, uint64_t);

#define DECLARE_VARIANT(name) \
  extern int m3_bench_##name##_compose(const uint64_t *, const uint64_t *, uint64_t *, size_t); \
  extern int m3_bench_##name##_repeat(const uint64_t *, uint64_t *, size_t, uint64_t)
DECLARE_VARIANT(baseline);
DECLARE_VARIANT(sparse);
DECLARE_VARIANT(portable);

struct variant { const char *name; compose_fn compose; repeat_fn repeat; };
static const struct variant variants[3] = {
    {"baseline", m3_bench_baseline_compose, m3_bench_baseline_repeat},
    {"sparse", m3_bench_sparse_compose, m3_bench_sparse_repeat},
    {"portable", m3_bench_portable_compose, m3_bench_portable_repeat}
};
static volatile uint64_t consumed;
static const char *families[10] = {
    "zero", "sparse", "dense", "highest-bit", "ceiling-early",
    "repeated-frontier", "alternating", "random", "zero-left-only", "zero-right-only"
};

static void fail(const char *message) {
    (void) fprintf(stderr, "M3_BENCH_FAIL:%s\n", message);
    exit(1);
}

static uint64_t timespec_ns(struct timespec value) {
    if (value.tv_sec < 0 || value.tv_nsec < 0 || value.tv_nsec >= 1000000000L)
        fail("invalid clock result");
    const uint64_t seconds = (uint64_t) value.tv_sec;
    const uint64_t nanos = (uint64_t) value.tv_nsec;
    if (seconds > (UINT64_MAX - nanos) / UINT64_C(1000000000))
        fail("clock overflow");
    return seconds * UINT64_C(1000000000) + nanos;
}

static uint64_t now_ns(void) {
    struct timespec value;
    if (clock_gettime(CLOCK_MONOTONIC, &value) != 0) fail("clock_gettime");
    return timespec_ns(value);
}

/* Provisioned libc supplies the generator. It runs only while constructing printed inputs. */
static uint64_t random_bits(void) {
    uint64_t value = 0;
    for (unsigned bit = 0; bit < 64; ++bit)
        if (rand() > RAND_MAX / 2) value |= UINT64_C(1) << bit;
    return value;
}

static void input_rows(size_t n, unsigned family, uint64_t *left, uint64_t *right) {
    const uint64_t mask = n == 64 ? UINT64_MAX : (UINT64_C(1) << n) - UINT64_C(1);
    srand(0x4d335245U ^ (unsigned) n ^ (family << 16));
    for (size_t row = 0; row < n; ++row) {
        const uint64_t diagonal = UINT64_C(1) << row;
        switch (family) {
            case 0: left[row] = 0; right[row] = 0; break;
            case 1:
                left[row] = UINT64_C(1) << ((row * 13 + 3) % n);
                right[row] = UINT64_C(1) << ((row * 7 + 1) % n);
                break;
            case 2: left[row] = mask; right[row] = diagonal; break;
            case 3:
                left[row] = (UINT64_C(1) << (n - 1)) | diagonal;
                right[row] = diagonal;
                break;
            case 4:
                left[row] = UINT64_C(1) << ((n - 1 - row) % n);
                right[row] = mask;
                break;
            case 5:
                left[row] = UINT64_C(0xaa55aa55aa55aa55) & mask;
                right[row] = diagonal;
                break;
            case 6:
                left[row] = (row % 2 == 0 ? UINT64_C(0xaaaaaaaaaaaaaaaa)
                                          : UINT64_C(0x5555555555555555)) & mask;
                right[row] = diagonal;
                break;
            case 7:
                left[row] = random_bits() & mask;
                right[row] = random_bits() & mask;
                break;
            case 8: left[row] = 0; right[row] = diagonal; break;
            case 9: left[row] = diagonal; right[row] = 0; break;
            default: fail("unknown family");
        }
    }
}

static int invoke(unsigned variant, unsigned operation, const uint64_t *left,
                  const uint64_t *right, uint64_t *out, size_t n) {
    return operation == 0 ? variants[variant].compose(left, right, out, n)
                          : variants[variant].repeat(left, out, n, UINT64_C(7));
}

static uint64_t exercise(unsigned variant, unsigned operation, const uint64_t *left,
                         const uint64_t *right, uint64_t *out, size_t n, uint64_t iterations) {
    uint64_t checksum = UINT64_C(0xcbf29ce484222325);
    for (uint64_t i = 0; i < iterations; ++i) {
        if (invoke(variant, operation, left, right, out, n) != 0) fail("kernel rejected valid rows");
        checksum = (checksum * UINT64_C(0x100000001b3)) ^ out[(size_t) (i % (uint64_t) n)] ^ i;
    }
    consumed = consumed ^ checksum;
    return checksum;
}

static uint64_t timed(unsigned variant, unsigned operation, const uint64_t *left,
                      const uint64_t *right, uint64_t *out, size_t n,
                      uint64_t iterations, uint64_t *checksum) {
    const uint64_t before = now_ns();
    *checksum = exercise(variant, operation, left, right, out, n, iterations);
    const uint64_t after = now_ns();
    if (after <= before) fail("nonpositive measured interval");
    return after - before;
}

static void json_rows(const uint64_t *rows, size_t n) {
    (void) putchar('[');
    for (size_t i = 0; i < n; ++i)
        (void) printf("%s\"%016" PRIx64 "\"", i == 0 ? "" : ",", rows[i]);
    (void) putchar(']');
}

int main(void) {
    const size_t dimensions[10] = {1, 2, 31, 32, 63, 64, 3, 4, 8, 16};
    struct timespec resolution;
    if (clock_getres(CLOCK_MONOTONIC, &resolution) != 0) fail("clock_getres");
    const uint64_t resolution_ns = timespec_ns(resolution);
    if (resolution_ns == 0) fail("zero clock resolution");
    (void) printf("{\"kind\":\"environment\",\"schema\":\"M3_RELATION_NATIVE_PAIRED_TIMING_V2\","
                 "\"cases\":200,\"samples\":5,\"variants\":3,\"targetNs\":%" PRIu64 ","
                 "\"iterationCap\":%" PRIu64 ",\"clockResolutionNs\":%" PRIu64 ","
                 "\"randomInputApi\":\"libc-rand-srand\",\"randMax\":%d}\n",
                 TARGET_NS, ITERATION_CAP, resolution_ns, RAND_MAX);
    unsigned case_id = 0, measurements = 0;
    /* Original families first: cases 0..95 retain their original inputs and order. */
    for (unsigned cohort = 0; cohort < 2; ++cohort) {
        const unsigned first_family = cohort == 0 ? 0U : 8U;
        const unsigned family_limit = cohort == 0 ? 8U : 10U;
        for (unsigned d = 0; d < 10; ++d) {
            const size_t n = dimensions[d];
            for (unsigned family = first_family; family < family_limit; ++family) {
                uint64_t left[64] = {0}, right[64] = {0};
                input_rows(n, family, left, right);
                for (unsigned operation = 0; operation < 2; ++operation) {
                    uint64_t expected[64] = {0}, out[64] = {0};
                    if (invoke(0, operation, left, right, expected, n) != 0) fail("baseline result");
                    for (unsigned variant = 0; variant < 3; ++variant) {
                        if (invoke(variant, operation, left, right, out, n) != 0
                                || memcmp(expected, out, n * sizeof(uint64_t)) != 0)
                            fail("complete output disagreement before timing");
                        (void) exercise(variant, operation, left, right, out, n, UINT64_C(256));
                    }
                    uint64_t iterations = UINT64_C(64), pilot_ns = 0, pilot_checksum = 0;
                    for (;;) {
                        pilot_ns = timed(0, operation, left, right, out, n, iterations, &pilot_checksum);
                        if (pilot_ns >= TARGET_NS || iterations == ITERATION_CAP) break;
                        iterations *= UINT64_C(2);
                    }
                    (void) printf("{\"kind\":\"case\",\"case\":%u,\"states\":%zu,\"family\":\"%s\","
                                 "\"operation\":\"%s\",\"iterations\":%" PRIu64 ","
                                 "\"pilotNs\":%" PRIu64 ",\"pilotChecksum\":\"%016" PRIx64 "\","
                                 "\"targetReached\":%s,\"left\":",
                                 case_id, n, families[family], operation == 0 ? "compose" : "repeat-7",
                                 iterations, pilot_ns, pilot_checksum, pilot_ns >= TARGET_NS ? "true" : "false");
                    json_rows(left, n); (void) printf(",\"right\":"); json_rows(right, n);
                    (void) printf(",\"expected\":"); json_rows(expected, n); (void) printf("}\n");
                    for (unsigned sample = 0; sample < SAMPLE_COUNT; ++sample) {
                        uint64_t reference_checksum = 0;
                        for (unsigned ordinal = 0; ordinal < 3; ++ordinal) {
                            const unsigned variant = (sample + case_id + ordinal) % 3;
                            uint64_t checksum = 0;
                            const uint64_t elapsed = timed(variant, operation, left, right, out, n,
                                                          iterations, &checksum);
                            if (memcmp(expected, out, n * sizeof(uint64_t)) != 0)
                                fail("complete output disagreement after timing");
                            if (ordinal == 0) reference_checksum = checksum;
                            else if (checksum != reference_checksum) fail("paired checksum disagreement");
                            (void) printf("{\"kind\":\"sample\",\"case\":%u,\"sample\":%u,\"ordinal\":%u,"
                                         "\"variant\":\"%s\",\"iterations\":%" PRIu64 ","
                                         "\"elapsedNs\":%" PRIu64 ",\"checksum\":\"%016" PRIx64 "\"}\n",
                                         case_id, sample, ordinal, variants[variant].name,
                                         iterations, elapsed, checksum);
                            ++measurements;
                        }
                    }
                    ++case_id;
                }
            }
        }
    }
    if (case_id != MAX_CASES || measurements != 3000) fail("measurement census");
    (void) printf("{\"kind\":\"summary\",\"cases\":%u,\"measurements\":%u,"
                 "\"consumedChecksum\":\"%016" PRIx64 "\",\"state\":\"PASS\"}\n",
                 case_id, measurements, consumed);
    if (fflush(stdout) != 0 || ferror(stdout)) fail("stdout");
    return 0;
}
