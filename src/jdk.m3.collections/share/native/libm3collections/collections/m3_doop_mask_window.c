/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Lineage: com.synexia 537b10a9de1f synexia-primitives/src/main/native/m3_doop_mask_window.c blob 59637067aa64 (com.synexia.primitives.M3DoopMaskedWindowScan -> jdk.internal.m3.collections.DoopMaskedWindowScan, 1 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
/* Optional DOOP JNI peer: bounded stack-copy windows and no retained JNI references. */
#include <jni.h>
#include <stdint.h>

#define M3_MASK_CHUNK 128
#define M3_MAX_WINDOW 4096

static jlong m3_pack(jint next, jint written) {
    return ((jlong) next << 32) | ((jlong) written & INT64_C(0xffffffff));
}

static jlong m3_argument_error(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
    return (jlong) -1;
}

static int m3_selected(jshort flags, jshort required, jshort excluded) {
    uint16_t bits = (uint16_t) flags;
    uint16_t yes = (uint16_t) required;
    uint16_t no = (uint16_t) excluded;
    return (bits & yes) == yes && (bits & no) == 0;
}

JNIEXPORT jlong JNICALL Java_jdk_internal_m3_collections_DoopMaskedWindowScan_scanNative(
        JNIEnv *env, jclass type, jlongArray idsArray, jshortArray flagsArray,
        jint count, jint first, jshort required, jshort excluded,
        jlongArray outputArray, jint offset, jint maximum) {
    (void) type;
    if (idsArray == NULL || flagsArray == NULL || outputArray == NULL
            || count < 0 || first < 0 || first > count
            || maximum < 0 || maximum > M3_MAX_WINDOW || offset < 0) {
        return m3_argument_error(env, "invalid native masked viewport arguments");
    }
    if ((*env)->IsSameObject(env, idsArray, outputArray) == JNI_TRUE) {
        return m3_argument_error(env, "source/output alias");
    }
    jsize outputLength = (*env)->GetArrayLength(env, outputArray);
    if ((*env)->GetArrayLength(env, idsArray) < count
            || (*env)->GetArrayLength(env, flagsArray) < count
            || offset > outputLength || maximum > outputLength - offset) {
        return m3_argument_error(env, "invalid native masked viewport capacity");
    }
    if (maximum == 0 || first == count) return m3_pack(first, 0);

    jlong ids[M3_MASK_CHUNK];
    jshort flags[M3_MASK_CHUNK];
    /* The caller already bounds output to 4096 primitive slots. This stack-only
     * staging lane never aliases a Java array and retains no JNI pointer. */
    jlong selected[M3_MAX_WINDOW];
    jint next = first;
    jint matches = 0;
    /* Validate every visited ID before publishing any caller-visible output.
     * Populate the selected primitive IDs during this already required scan;
     * no second Get*ArrayRegion replay is necessary. */
    while (next < count && matches < maximum) {
        jint take = count - next;
        if (take > M3_MASK_CHUNK) take = M3_MASK_CHUNK;
        (*env)->GetLongArrayRegion(env, idsArray, next, take, ids);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong) -1;
        (*env)->GetShortArrayRegion(env, flagsArray, next, take, flags);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong) -1;
        for (jint i = 0; i < take && matches < maximum; i++) {
            if (ids[i] <= 0) return m3_argument_error(env, "invalid native logical ID");
            if (m3_selected(flags[i], required, excluded)) selected[matches++] = ids[i];
            next++;
        }
    }

    /* Keep the original 128-element output Region boundaries and their error
     * checks. The first write still happens only after complete validation. */
    for (jint copied = 0; copied < matches;) {
        jint take = matches - copied;
        if (take > M3_MASK_CHUNK) take = M3_MASK_CHUNK;
        (*env)->SetLongArrayRegion(env, outputArray, offset + copied, take,
                selected + copied);
        if ((*env)->ExceptionCheck(env) == JNI_TRUE) return (jlong) -1;
        copied += take;
    }
    return m3_pack(next, matches);
}
