/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
/* Bounded direct-ABI corpus for the existing compose contract; no product mutation.
 * Reuses the baseline C buffer contract and the straightforward Java/matrix proof.
 * Linux dlopen/dlsym diagnostics only. This does not qualify unrestricted pointers. */
#include "m3_state_relation.h"
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define BUFFER_WORDS 68U
#define INPUT_START 2U
#define VARIANT_COUNT 3U
#define EXPECTED_CASES 611U
#define EXPECTED_CHECKS 2444U

typedef int (*compose_function)(const uint64_t *, const uint64_t *, uint64_t *, size_t);
_Static_assert(sizeof(compose_function) == sizeof(void *),
               "direct proof requires equal Linux object/function pointer sizes");

static const char *variant_name = "driver";
static size_t current_states;
static unsigned current_family, current_layout, cases, checks;

static void fail(const char *reason) {
    (void) fprintf(stderr,
        "M3_RELATION_DIRECT_C_PROOF_FAIL variant=%s states=%zu family=%u layout=%u "
        "case=%u check=%u reason=%s\n",
        variant_name, current_states, current_family, current_layout, cases, checks, reason);
    exit(EXIT_FAILURE);
}

static void check(int condition, const char *reason) {
    ++checks;
    if (!condition) fail(reason);
}

static void fill_storage(uint64_t *storage, uint64_t tag) {
    for (size_t i = 0; i < BUFFER_WORDS; ++i)
        storage[i] = UINT64_C(0xfedcba9876543210) ^ ((uint64_t) i << 8) ^ tag;
}

static void input_rows(size_t states, unsigned family, uint64_t *left, uint64_t *right) {
    const uint64_t mask = states == 64 ? UINT64_MAX
        : (UINT64_C(1) << states) - UINT64_C(1);
    const uint64_t highest = UINT64_C(1) << (states - 1);
    for (size_t row = 0; row < states; ++row) {
        const uint64_t diagonal = UINT64_C(1) << row;
        switch (family) {
            case 0:
                left[row] = (UINT64_C(0xaa55aa55aa55aa55) ^ diagonal ^ highest) & mask;
                right[row] = 0;
                break;
            case 1:
                left[row] = (UINT64_C(1) << ((row + 1) % states))
                    | (UINT64_C(1) << ((row + 2) % states));
                right[row] = diagonal | (UINT64_C(1) << ((row * 7 + 1) % states));
                break;
            case 2:
                left[row] = highest | diagonal;
                right[row] = UINT64_C(1) << ((row + 1) % states);
                break;
            case 3:
                left[row] = mask;
                right[row] = diagonal;
                break;
            case 4:
                left[row] = 0;
                right[row] = diagonal;
                break;
            default: fail("unknown input family");
        }
    }
}

/* Independent complete Boolean-matrix oracle: no CTZ, ceiling, adjacency cache,
 * staging shortcut or invocation of any loaded kernel. Inputs are frozen snapshots. */
static void matrix_oracle(const uint64_t *left, const uint64_t *right,
                          uint64_t *expected, size_t states) {
    for (size_t row = 0; row < states; ++row) {
        expected[row] = 0;
        for (size_t column = 0; column < states; ++column)
            if ((left[row] & (UINT64_C(1) << column)) != 0)
                expected[row] |= right[column];
    }
}

/* Layout0: disjoint. Layout1/2: exact left/right alias.
 * Layout3/4: output starts one word after/before left.
 * Layout5/6: output starts one word after/before right.
 * For states1 these shifted ranges are adjacent; states>=2 actually overlap. */
static void matrix_case(compose_function compose, size_t states, unsigned family,
                        unsigned layout, unsigned invalid) {
    uint64_t left_storage[BUFFER_WORDS], right_storage[BUFFER_WORDS], out_storage[BUFFER_WORDS];
    uint64_t expected_left[BUFFER_WORDS], expected_right[BUFFER_WORDS], expected_out[BUFFER_WORDS];
    uint64_t left_snapshot[64], right_snapshot[64], expected_rows[64];
    uint64_t *output, *expected_output;
    fill_storage(left_storage, UINT64_C(0x100));
    fill_storage(right_storage, UINT64_C(0x200));
    fill_storage(out_storage, UINT64_C(0x300));
    input_rows(states, family, left_storage + INPUT_START, right_storage + INPUT_START);
    if (invalid != 0) {
        if (states >= 64) fail("foreign-bit case must have states below64");
        const uint64_t foreign = UINT64_C(1) << states;
        if (invalid == 1 || invalid == 2)
            left_storage[INPUT_START + states - 1] |= foreign;
        else if (invalid == 3 || invalid == 4)
            right_storage[INPUT_START + states - 1] |= foreign;
        else fail("unknown invalid-row family");
    }
    memcpy(left_snapshot, left_storage + INPUT_START, states * sizeof(uint64_t));
    memcpy(right_snapshot, right_storage + INPUT_START, states * sizeof(uint64_t));
    memcpy(expected_left, left_storage, sizeof(left_storage));
    memcpy(expected_right, right_storage, sizeof(right_storage));
    memcpy(expected_out, out_storage, sizeof(out_storage));
    switch (layout) {
        case 0: output = out_storage + INPUT_START; expected_output = expected_out + INPUT_START; break;
        case 1: output = left_storage + INPUT_START; expected_output = expected_left + INPUT_START; break;
        case 2: output = right_storage + INPUT_START; expected_output = expected_right + INPUT_START; break;
        case 3: output = left_storage + INPUT_START + 1; expected_output = expected_left + INPUT_START + 1; break;
        case 4: output = left_storage + INPUT_START - 1; expected_output = expected_left + INPUT_START - 1; break;
        case 5: output = right_storage + INPUT_START + 1; expected_output = expected_right + INPUT_START + 1; break;
        case 6: output = right_storage + INPUT_START - 1; expected_output = expected_right + INPUT_START - 1; break;
        default: fail("unknown buffer layout");
    }
    if (invalid == 0) {
        matrix_oracle(left_snapshot, right_snapshot, expected_rows, states);
        memcpy(expected_output, expected_rows, states * sizeof(uint64_t));
    }
    current_states = states;
    current_family = family + invalid * 10U;
    current_layout = layout;
    ++cases;
    const int result = compose(left_storage + INPUT_START, right_storage + INPUT_START, output, states);
    check(result == (invalid == 0 ? 0 : -1), "compose return code");
    check(memcmp(left_storage, expected_left, sizeof(left_storage)) == 0, "complete left storage/guards");
    check(memcmp(right_storage, expected_right, sizeof(right_storage)) == 0, "complete right storage/guards");
    check(memcmp(out_storage, expected_out, sizeof(out_storage)) == 0, "complete output storage/guards");
}

static void argument_case(compose_function compose, size_t states, unsigned null_mask) {
    uint64_t left[BUFFER_WORDS], right[BUFFER_WORDS], output[BUFFER_WORDS];
    uint64_t expected_left[BUFFER_WORDS], expected_right[BUFFER_WORDS], expected_output[BUFFER_WORDS];
    fill_storage(left, UINT64_C(0x400));
    fill_storage(right, UINT64_C(0x500));
    fill_storage(output, UINT64_C(0x600));
    memcpy(expected_left, left, sizeof(left));
    memcpy(expected_right, right, sizeof(right));
    memcpy(expected_output, output, sizeof(output));
    current_states = states;
    current_family = 100U + null_mask;
    current_layout = 0;
    ++cases;
    const int result = compose((null_mask & 1U) != 0 ? NULL : left + INPUT_START,
        (null_mask & 2U) != 0 ? NULL : right + INPUT_START,
        (null_mask & 4U) != 0 ? NULL : output + INPUT_START, states);
    check(result == -1, "invalid argument return code");
    check(memcmp(left, expected_left, sizeof(left)) == 0, "invalid argument left storage/guards");
    check(memcmp(right, expected_right, sizeof(right)) == 0, "invalid argument right storage/guards");
    check(memcmp(output, expected_output, sizeof(output)) == 0, "invalid argument output storage/guards");
}

static void run_corpus(compose_function compose) {
    const size_t dimensions[10] = {1, 2, 3, 4, 8, 16, 31, 32, 63, 64};
    for (size_t d = 0; d < 10; ++d) {
        const size_t states = dimensions[d];
        for (unsigned family = 0; family < 5U; ++family)
            for (unsigned layout = 0; layout < 7U; ++layout)
                matrix_case(compose, states, family, layout, 0);
        if (states < 64) {
            for (unsigned invalid = 1; invalid <= 4U; ++invalid) {
                /*1: zero-right+late-invalid-left;2: nonzero+invalid-left;
                 *3: nonzero+invalid-right;4: zero-left+late-invalid-right. */
                const unsigned family = invalid == 1 ? 0U : invalid == 4 ? 4U : 1U;
                for (unsigned layout = 0; layout < 7U; ++layout)
                    matrix_case(compose, states, family, layout, invalid);
            }
        }
    }
    for (unsigned null_mask = 1; null_mask < 8U; ++null_mask)
        argument_case(compose, 64, null_mask);
    argument_case(compose, 0, 0);
    argument_case(compose, 65, 0);
}

int main(int argc, char **argv) {
    static const char *const names[VARIANT_COUNT] = {"baseline", "sparse", "portable"};
    unsigned total_cases = 0, total_checks = 0;
    if (argc != 4) fail("expected exactly baseline/sparse/portable absolute .so paths");
    for (unsigned i = 0; i < VARIANT_COUNT; ++i) {
        const char *path = argv[i + 1U];
        const size_t length = strlen(path);
        if (path[0] != '/' || length < 4 || strcmp(path + length - 3, ".so") != 0)
            fail("expected absolute .so path");
        for (unsigned previous = 0; previous < i; ++previous)
            if (strcmp(path, argv[previous + 1U]) == 0) fail("variant paths must be distinct");
    }
    for (unsigned variant = 0; variant < VARIANT_COUNT; ++variant) {
        variant_name = names[variant];
        cases = 0;
        checks = 0;
        void *handle = dlopen(argv[variant + 1U], RTLD_NOW | RTLD_LOCAL);
        if (handle == NULL) fail("dlopen failed");
        (void) dlerror();
        void *symbol = dlsym(handle, "m3_state_relation_compose");
        const char *error = dlerror();
        if (error != NULL || symbol == NULL) fail("compose symbol lookup failed");
        compose_function compose = NULL;
        /* POSIX supports dlsym function symbols. This Linux-only equal-size
         * representation copy avoids ISO C object/function-pointer casts and
         * type-punning; unsupported pointer sizes fail at compile time. */
        memcpy(&compose, &symbol, sizeof(compose));
        if (compose == NULL) fail("null compose function representation");
        run_corpus(compose);
        if (cases != EXPECTED_CASES || checks != EXPECTED_CHECKS) fail("deterministic corpus census");
        total_cases += cases;
        total_checks += checks;
        if (dlclose(handle) != 0) fail("dlclose failed");
    }
    if (total_cases != 1833U || total_checks != 7332U) fail("three-variant census");
    if (printf("{\"schema\":\"M3_RELATION_DIRECT_C_PROOF_V1\",\"state\":\"PASS\","
        "\"variants\":3,\"casesPerVariant\":611,\"checksPerVariant\":2444,"
        "\"totalCases\":1833,\"totalChecks\":7332,"
        "\"zeroRightCasesExecuted\":true,\"partialOverlapCasesExecuted\":true,"
        "\"exactAliasCasesExecuted\":true,\"lateInvalidOutputCasesExecuted\":true,"
        "\"invalidDimensionCasesExecuted\":true,\"nullArgumentCasesExecuted\":true,"
        "\"state64HighestBitCasesExecuted\":true,\"independentSnapshotOracleExecuted\":true,"
        "\"outputGuardCasesExecuted\":true,\"unrestrictedPointerProofQualified\":false,"
        "\"performanceQualified\":false}\n") < 0 || fflush(stdout) != 0) fail("stdout");
    return EXIT_SUCCESS;
}
