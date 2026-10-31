// SPDX-License-Identifier: Apache-2.0
/*
 * Source-byte Aho-Corasick containment prefilter. The Java owner compiles the
 * same sparse primitive automaton; no needle spelling or JVM address is retained.
 * JNI snapshots the bounded metadata arrays, then reads a caller-owned direct
 * byte buffer synchronously. The existing v1 LF/KMP JNI symbols are unchanged.
 */
#include <jni.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>

static void invalid_set(JNIEnv *env, const char *message) {
    jclass klass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (klass != NULL) (void)(*env)->ThrowNew(env, klass, message);
}

static jint find_sparse_edge(const jint *starts, const jbyte *symbols,
        const jint *targets, jint state, uint8_t unit) {
    jint low = starts[state], high = starts[state + 1] - 1;
    while (low <= high) {
        jint mid = low + (high - low) / 2;
        uint8_t label = (uint8_t) symbols[mid];
        if (label == unit) return targets[mid];
        if (label < unit) low = mid + 1;
        else high = mid - 1;
    }
    return -1;
}

JNIEXPORT jlong JNICALL
Java_com_m3_runtime_capability_BytePatternSet_scanSet0(
        JNIEnv *env, jclass owner, jobject buffer, jint length,
        jintArray starts_array, jbyteArray symbols_array,
        jintArray targets_array, jintArray failure_array, jlongArray output_array) {
    (void) owner;
    if (buffer == NULL || length < 0 || length > 16777216
            || starts_array == NULL || symbols_array == NULL
            || targets_array == NULL || failure_array == NULL || output_array == NULL) {
        invalid_set(env, "invalid byte-pattern-set JNI input");
        return 0;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    if (capacity < length || capacity < 0) {
        invalid_set(env, "byte-pattern-set direct capacity");
        return 0;
    }
    const uint8_t *data = (const uint8_t *)(*env)->GetDirectBufferAddress(env, buffer);
    if (data == NULL && length != 0) {
        invalid_set(env, "missing pattern-set direct input");
        return 0;
    }
    jsize states = (*env)->GetArrayLength(env, failure_array);
    jsize edges = (*env)->GetArrayLength(env, symbols_array);
    if (states < 1 || states > 16385 || edges < 0 || edges > 16384
            || (*env)->GetArrayLength(env, starts_array) != states + 1
            || (*env)->GetArrayLength(env, targets_array) != edges
            || (*env)->GetArrayLength(env, output_array) != states
            || 4LL * (states + 1LL) + 5LL * edges + 12LL * states > 1048576LL) {
        invalid_set(env, "byte-pattern-set metadata geometry");
        return 0;
    }
    jint *starts = malloc(((size_t) states + 1U) * sizeof(*starts));
    jbyte *symbols = malloc(((size_t) edges + 1U) * sizeof(*symbols));
    jint *targets = malloc(((size_t) edges + 1U) * sizeof(*targets));
    jint *failures = malloc((size_t) states * sizeof(*failures));
    jlong *outputs = malloc((size_t) states * sizeof(*outputs));
    if (starts == NULL || symbols == NULL || targets == NULL
            || failures == NULL || outputs == NULL) {
        jclass oom = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (oom != NULL) (void)(*env)->ThrowNew(env, oom, "pattern-set JNI metadata");
        goto release;
    }
    (*env)->GetIntArrayRegion(env, starts_array, 0, states + 1, starts);
    if ((*env)->ExceptionCheck(env)) goto release;
    (*env)->GetByteArrayRegion(env, symbols_array, 0, edges, symbols);
    if ((*env)->ExceptionCheck(env)) goto release;
    (*env)->GetIntArrayRegion(env, targets_array, 0, edges, targets);
    if ((*env)->ExceptionCheck(env)) goto release;
    (*env)->GetIntArrayRegion(env, failure_array, 0, states, failures);
    if ((*env)->ExceptionCheck(env)) goto release;
    (*env)->GetLongArrayRegion(env, output_array, 0, states, outputs);
    if ((*env)->ExceptionCheck(env)) goto release;

    if (starts[0] != 0 || starts[states] != edges || failures[0] != 0) {
        invalid_set(env, "invalid pattern-set start/end metadata");
        goto release;
    }
    for (jint s = 0; s < states; s++) {
        if (starts[s] < 0 || starts[s] > starts[s + 1]
                || starts[s + 1] > edges || failures[s] < 0 || failures[s] >= states) {
            invalid_set(env, "invalid pattern-set edge/failure range");
            goto release;
        }
        jint previous = -1;
        for (jint e = starts[s]; e < starts[s + 1]; e++) {
            jint symbol = (uint8_t) symbols[e];
            if (symbol <= previous || targets[e] <= 0 || targets[e] >= states) {
                invalid_set(env, "invalid pattern-set sorted edge");
                goto release;
            }
            previous = symbol;
        }
    }
    jint state = 0;
    uint64_t mask = 0;
    for (jint i = 0; i < length; i++) {
        uint8_t value = data[i];
        jint next = find_sparse_edge(starts, symbols, targets, state, value);
        jint steps = 0;
        while (next < 0 && state != 0) {
            if (++steps > states) {
                invalid_set(env, "pattern-set failure cycle");
                goto release;
            }
            state = failures[state];
            next = find_sparse_edge(starts, symbols, targets, state, value);
        }
        state = next < 0 ? 0 : next;
        mask |= (uint64_t) outputs[state];
    }
    free(outputs); free(failures); free(targets); free(symbols); free(starts);
    return (jlong) mask;
release:
    free(outputs); free(failures); free(targets); free(symbols); free(starts);
    return 0;
}
