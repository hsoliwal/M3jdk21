/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#ifndef M3_UNIX_UTF8_KMP_H
#define M3_UNIX_UTF8_KMP_H

#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* Private invocation-scoped plan, not a new public native or JNI API. Bytes are
 * immutable inputs owned by the caller; only the failure lane is allocated.
 */
typedef struct {
    const unsigned char *needle;
    size_t length;
    uint32_t *failure;
} M3Utf8Kmp;

/* Allocation is optional: memory pressure falls back to the old bytewise oracle
 * instead of altering successful invocation semantics. No source is retained.
 */
static M3Utf8Kmp m3_kmp_prepare(const unsigned char *needle, size_t length) {
    M3Utf8Kmp result = {needle, length, NULL};
    /* JNI jsize counts at most INT32_MAX bytes. Keep table entries 32-bit
     * even on LP64, and fail to the pre-existing bytewise path when an
     * arbitrary standalone caller exceeds representable/allocatable size.
     */
    if (length <= 2 || length > UINT32_MAX
            || length > SIZE_MAX / sizeof(uint32_t)) return result;
    result.failure = (uint32_t *)malloc(length * sizeof(uint32_t));
    if (result.failure == NULL) return result;
    result.failure[0] = 0;
    for (size_t i = 1, prefix = 0; i < length; i++) {
        while (prefix != 0 && needle[i] != needle[prefix]) {
            prefix = result.failure[prefix - 1];
        }
        if (needle[i] == needle[prefix]) prefix++;
        result.failure[i] = (uint32_t)prefix;
    }
    return result;
}

/* Exact bytewise substring predicate, with row boundaries enforced by caller.
 * With a prepared failure lane, work is O(row_length + pattern_length) across
 * preparation + selected rows; under OOM fallback the old worst case applies.
 */
static int m3_kmp_contains(const M3Utf8Kmp *plan,
                           const unsigned char *source, size_t sourceLength) {
    size_t size = plan->length;
    if (size == 0) return 1;
    if (size > sourceLength) return 0;
    if (size == 1) return memchr(source, plan->needle[0], sourceLength) != NULL;
    if (plan->failure == NULL) {
        for (size_t i = 0; i <= sourceLength - size; i++) {
            if (source[i] == plan->needle[0]
                    && memcmp(source + i, plan->needle, size) == 0) return 1;
        }
        return 0;
    }
    for (size_t i = 0, matched = 0; i < sourceLength; i++) {
        while (matched != 0 && source[i] != plan->needle[matched]) {
            matched = plan->failure[matched - 1];
        }
        if (source[i] == plan->needle[matched] && ++matched == size) {
            return 1;
        }
    }
    return 0;
}

static void m3_kmp_release(M3Utf8Kmp *plan) {
    free(plan->failure);
    plan->failure = NULL;
}

#endif /* M3_UNIX_UTF8_KMP_H */
