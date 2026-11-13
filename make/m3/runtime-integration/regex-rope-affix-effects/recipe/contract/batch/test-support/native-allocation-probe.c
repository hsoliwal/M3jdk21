// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/* Qualification-only allocation probe. No hooks are added to the product owner. */
#include <stdint.h>
#include <stdlib.h>
#include <stdatomic.h>
#include <jni.h>

static atomic_long allocation_allowance = -1;
static atomic_long live_allocations = 0;

static int allow_allocation(void) {
    long remaining = atomic_load(&allocation_allowance);
    if (remaining < 0) return 1;
    while (remaining > 0) {
        if (atomic_compare_exchange_weak(&allocation_allowance, &remaining, remaining - 1)) return 1;
    }
    return 0;
}

static void *probe_malloc(size_t bytes) {
    if (!allow_allocation()) return NULL;
    void *result = malloc(bytes);
    if (result != NULL) atomic_fetch_add(&live_allocations, 1);
    return result;
}

static void *probe_calloc(size_t count, size_t bytes) {
    if (!allow_allocation()) return NULL;
    void *result = calloc(count, bytes);
    if (result != NULL) atomic_fetch_add(&live_allocations, 1);
    return result;
}

static void probe_free(void *pointer) {
    if (pointer != NULL) atomic_fetch_sub(&live_allocations, 1);
    free(pointer);
}

#define malloc probe_malloc
#define calloc probe_calloc
#define free probe_free
#ifndef M3_REGEX_BATCH_OWNER_C
#define M3_REGEX_BATCH_OWNER_C "../templates/src/java.base/share/native/libjava/M3RegexDpPlan.c"
#endif
#include M3_REGEX_BATCH_OWNER_C
#undef malloc
#undef calloc
#undef free

JNIEXPORT void JNICALL
Java_jdk_internal_mindex_M3RegexPreparedBatchTestSupport_failAllocationAfter(
        JNIEnv *env, jclass cls, jlong allowance) {
    (void)env;
    (void)cls;
    atomic_store(&allocation_allowance, (long)allowance);
}

JNIEXPORT jlong JNICALL
Java_jdk_internal_mindex_M3RegexPreparedBatchTestSupport_liveAllocations(JNIEnv *env, jclass cls) {
    (void)env;
    (void)cls;
    return (jlong)atomic_load(&live_allocations);
}
