/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stddef.h>

/*
 * Signed, unique, ascending primitive lanes only: validated by M3SortedIntSet.
 * Keep the Java JNI name, ownership and two-pass exact-sized allocation unchanged.
 * One private atom picks merge for balanced lanes and monotone exponential search
 * when the smaller side is more than 16x shorter, matching the Java owner.
 */
#ifdef M3_SORTED_SET_PROBES
static unsigned long long m3_sorted_set_comparisons;
#define M3_LESS(a, b) (++m3_sorted_set_comparisons, (a) < (b))
#define M3_EQUAL(a, b) (++m3_sorted_set_comparisons, (a) == (b))
#else
#define M3_LESS(a, b) ((a) < (b))
#define M3_EQUAL(a, b) ((a) == (b))
#endif

static jsize m3_lower_bound(const jint *lane, jsize first, jsize last, jint target) {
    while (first < last) {
        const jsize mid = first + (last - first) / 2;
        if (M3_LESS(lane[mid], target)) first = mid + 1;
        else last = mid;
    }
    return first;
}

/* Every probe advances from the last resolved candidate; do not rescan a prefix. */
static jsize m3_gallop_lower_bound(const jint *lane, jsize cursor, jsize length, jint target) {
    if (cursor == length || !M3_LESS(lane[cursor], target)) return cursor;
    const size_t remaining = (size_t)(length - cursor);
    size_t span = 1;
    while (span < remaining && M3_LESS(lane[(size_t)cursor + span], target)) {
        span <<= 1;
    }
    const size_t lower = (size_t)cursor + (span >> 1) + 1;
    size_t upper = (size_t)cursor + span + 1;
    if (upper > (size_t)length) upper = (size_t)length;
    return m3_lower_bound(lane, (jsize)lower, (jsize)upper, target);
}

/* out=NULL counts the exact intersection; out!=NULL writes that many ints. */
static jsize m3_intersect_sorted(const jint *left, jsize na,
                                  const jint *right, jsize nb, jint *out) {
    const jint *small = left;
    const jint *large = right;
    jsize ns = na;
    jsize nl = nb;
    if (ns > nl) {
        small = right;
        large = left;
        ns = nb;
        nl = na;
    }
    jsize used = 0;
    if ((jlong)ns * 16 < (jlong)nl) {
        jsize cursor = 0;
        for (jsize i = 0; i < ns; i++) {
            cursor = m3_gallop_lower_bound(large, cursor, nl, small[i]);
            if (cursor == nl) break;
            if (M3_EQUAL(large[cursor], small[i])) {
                if (out != NULL) out[used] = small[i];
                used++;
                cursor++;
            }
        }
    } else {
        jsize i = 0;
        jsize j = 0;
        while (i < ns && j < nl) {
            if (M3_LESS(small[i], large[j])) i++;
            else if (M3_LESS(large[j], small[i])) j++;
            else {
                if (out != NULL) out[used] = small[i];
                used++;
                i++;
                j++;
            }
        }
    }
    return used;
}

/* The sole exported JNI symbol remains source/binary compatible with Wave 10. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_tools_cpu_M3SortedIntSetNative_intersection0(
        JNIEnv *env, jclass owner, jintArray left, jintArray right) {
    (void)owner;
    if (left == NULL || right == NULL) return NULL;
    const jsize na = (*env)->GetArrayLength(env, left);
    const jsize nb = (*env)->GetArrayLength(env, right);
    if (na == 0 || nb == 0) return (*env)->NewIntArray(env, 0);

    jint *a = (*env)->GetIntArrayElements(env, left, NULL);
    if (a == NULL) return NULL;
    jint *b = (*env)->GetIntArrayElements(env, right, NULL);
    if (b == NULL) {
        (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
        return NULL;
    }
    const jsize used = m3_intersect_sorted(a, na, b, nb, NULL);
    jintArray result = (*env)->NewIntArray(env, used);
    if (result != NULL && used != 0) {
        jint *out = (*env)->GetIntArrayElements(env, result, NULL);
        if (out != NULL) {
            const jsize written = m3_intersect_sorted(a, na, b, nb, out);
            (*env)->ReleaseIntArrayElements(env, result, out, 0);
            if (written != used) { /* Fail closed on any internal count/emit divergence. */
                result = NULL;
                jclass error = (*env)->FindClass(env, "java/lang/IllegalStateException");
                if (error != NULL) (*env)->ThrowNew(env, error, "intersection count mismatch");
            }
        } else result = NULL; /* Preserve pending JVM allocation exception. */
    }
    (*env)->ReleaseIntArrayElements(env, right, b, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, left, a, JNI_ABORT);
    return result;
}
