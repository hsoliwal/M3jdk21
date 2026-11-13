/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "m3_utf8_kmp.h"

/* Primitive-only line selection. All source pointers belong to the active JNI
 * invocation; no native code retains a Java-heap or direct-buffer pointer.
 */
enum { M3_GREP = 1, M3_UNIQ = 2, M3_HEAD = 3, M3_TAIL = 4 };

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass error = (*env)->FindClass(env, type);
    if (error != NULL) {
        (*env)->ThrowNew(env, error, message);
        (*env)->DeleteLocalRef(env, error);
    }
}

static int m3_selected(const jlong *mask, jsize index) {
    return (((uint64_t) mask[index >> 6] >> (index & 63)) & UINT64_C(1)) != 0;
}

static void m3_mark(jlong *out, jsize index) {
    out[index >> 6] = (jlong)((uint64_t) out[index >> 6]
                              | (UINT64_C(1) << (index & 63)));
}

/* Pure-bitset traversal of selected row positions, bounded to native word width.
 * This helper never dereferences text, stores JNI pointers, or changes the ABI.
 */
static int m3_lowbit_index(uint64_t nonzero) {
#if defined(__GNUC__) || defined(__clang__)
    return __builtin_ctzll((unsigned long long)nonzero);
#else
    int index = 0;
    while ((nonzero & UINT64_C(1)) == 0) { nonzero >>= 1; index++; }
    return index;
#endif
}

/* Source bytes are guaranteed live for this call by the immediate JNI wrapper. */
static jlongArray m3_select(JNIEnv *env, const unsigned char *source, jsize sourceLength,
                            jintArray offsetArray, jlongArray selectedArray,
                            jbyteArray needleArray, jint op, jint take) {
    jbyte *needle = NULL;
    jint *offsets = NULL;
    jlong *selected = NULL;
    jlong *output = NULL;
    jlongArray result = NULL;
    M3Utf8Kmp kmp = {NULL, 0, NULL};
    jsize offsetsLength, lineCount, maskWords, needleLength;

    if (offsetArray == NULL || selectedArray == NULL || needleArray == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "native line arrays");
        return NULL;
    }
    offsetsLength = (*env)->GetArrayLength(env, offsetArray);
    needleLength = (*env)->GetArrayLength(env, needleArray);
    if (offsetsLength < 1 || (op != M3_GREP && op != M3_UNIQ && op != M3_HEAD && op != M3_TAIL)
            || take < 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "native line request");
        return NULL;
    }
    lineCount = offsetsLength - 1;
    maskWords = (jsize) ((((uint64_t) lineCount) + 63) >> 6);
    if ((*env)->GetArrayLength(env, selectedArray) != maskWords) {
        m3_throw(env, "java/lang/IllegalArgumentException", "selection mask geometry");
        return NULL;
    }

    offsets = (*env)->GetIntArrayElements(env, offsetArray, NULL);
    if (offsets == NULL) goto done;
    selected = (*env)->GetLongArrayElements(env, selectedArray, NULL);
    if (selected == NULL) goto done;
    needle = (*env)->GetByteArrayElements(env, needleArray, NULL);
    if (needle == NULL) goto done;
    if (op == M3_GREP) {
        kmp = m3_kmp_prepare((const unsigned char *)needle, (size_t)needleLength);
    }

    if (offsets[0] != 0 || offsets[lineCount] != sourceLength) {
        m3_throw(env, "java/lang/IllegalArgumentException", "line offset boundaries");
        goto done;
    }
    for (jsize i = 1; i <= lineCount; i++) {
        if (offsets[i] < offsets[i - 1] || offsets[i] > sourceLength) {
            m3_throw(env, "java/lang/IllegalArgumentException", "nonmonotone line offsets");
            goto done;
        }
    }
    if (lineCount % 64 != 0 && maskWords != 0) {
        uint64_t valid = (UINT64_C(1) << (lineCount & 63)) - UINT64_C(1);
        if (((uint64_t) selected[maskWords - 1] & ~valid) != 0) {
            m3_throw(env, "java/lang/IllegalArgumentException", "out-of-range selection bits");
            goto done;
        }
    }

    output = (jlong *)calloc((size_t)(maskWords == 0 ? 1 : maskWords), sizeof(jlong));
    if (output == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native result mask allocation");
        goto done;
    }
    /* Validate all offsets and mask geometry above before any sparse traversal.
     * Each set bit is visited exactly once, preserving original row order.
     */
    jlong skip = 0;
    if (op == M3_TAIL) {
        jlong eligible = 0;
        for (jsize word = 0; word < maskWords; word++) {
            uint64_t bits = (uint64_t)selected[word];
            while (bits != 0) { eligible++; bits &= bits - UINT64_C(1); }
        }
        skip = eligible > take ? eligible - take : 0;
    }
    jsize previous = -1;
    jlong kept = 0;
    for (jsize word = 0; word < maskWords; word++) {
        uint64_t bits = (uint64_t)selected[word];
        while (bits != 0) {
            jsize i = (jsize)((uint64_t)word * UINT64_C(64)
                    + (uint64_t)m3_lowbit_index(bits));
            bits &= bits - UINT64_C(1);
            if (op == M3_TAIL) {
                if (skip > 0) { skip--; continue; }
                m3_mark(output, i);
                continue;
            }
            if (op == M3_HEAD && kept >= take) break;
            int accept;
            jsize length = offsets[i + 1] - offsets[i];
            if (op == M3_GREP) {
                accept = m3_kmp_contains(&kmp, source + offsets[i], (size_t)length);
            } else if (op == M3_UNIQ) {
                accept = previous < 0
                        || length != offsets[previous + 1] - offsets[previous]
                        || memcmp(source + offsets[i], source + offsets[previous], (size_t)length) != 0;
            } else {
                accept = 1;
            }
            if (accept) {
                m3_mark(output, i);
                previous = i;
                kept++;
            }
        }
        if (op == M3_HEAD && kept >= take) break;
    }
    result = (*env)->NewLongArray(env, maskWords);
    if (result != NULL && maskWords != 0) {
        (*env)->SetLongArrayRegion(env, result, 0, maskWords, output);
    }
done:
    m3_kmp_release(&kmp);
    free(output);
    if (needle != NULL) (*env)->ReleaseByteArrayElements(env, needleArray, needle, JNI_ABORT);
    if (selected != NULL) (*env)->ReleaseLongArrayElements(env, selectedArray, selected, JNI_ABORT);
    if (offsets != NULL) (*env)->ReleaseIntArrayElements(env, offsetArray, offsets, JNI_ABORT);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jint JNICALL
Java_jdk_m3_text_NativeLineBatch_abi0(JNIEnv *env, jclass type) {
    (void)env;
    (void)type;
    return 0x00010002;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_text_NativeLineBatch_select0(
        JNIEnv *env, jclass type, jbyteArray sourceArray, jintArray offsets,
        jlongArray selected, jbyteArray needle, jint op, jint take) {
    jbyte *source;
    jlongArray result;
    (void)type;
    if (sourceArray == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "packed source");
        return NULL;
    }
    jsize length = (*env)->GetArrayLength(env, sourceArray);
    source = (*env)->GetByteArrayElements(env, sourceArray, NULL);
    if (source == NULL) return NULL;
    result = m3_select(env, (const unsigned char *)source, length,
                       offsets, selected, needle, op, take);
    (*env)->ReleaseByteArrayElements(env, sourceArray, source, JNI_ABORT);
    return result;
}

JNIEXPORT jlongArray JNICALL
Java_jdk_m3_text_NativeLineBatch_selectDirect0(
        JNIEnv *env, jclass type, jobject direct, jintArray offsets,
        jlongArray selected, jbyteArray needle, jint op, jint take) {
    (void)type;
    if (direct == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "direct source");
        return NULL;
    }
    void *source = (*env)->GetDirectBufferAddress(env, direct);
    jlong length = (*env)->GetDirectBufferCapacity(env, direct);
    if (source == NULL || length < 0 || length > INT32_MAX) {
        m3_throw(env, "java/lang/IllegalArgumentException", "invalid M3 direct buffer");
        return NULL;
    }
    return m3_select(env, (const unsigned char *)source, (jsize)length,
                     offsets, selected, needle, op, take);
}

/* Stable unsigned-byte row ordering. This additive kernel keeps selection and
 * payload immutable and returns only original row indices. Its comparison order
 * is intentionally distinct from Java String's UTF-16 order. */
typedef struct {
    JNIEnv *env;
    jobject monitor;
    jmethodID checkCanceled;
    uint64_t budget;
    uint64_t used;
    uint64_t ticks;
} M3OrderControl;

static int m3_order_poll(M3OrderControl *control) {
    if (control->monitor != NULL) {
        (*control->env)->CallVoidMethod(control->env, control->monitor, control->checkCanceled);
        if ((*control->env)->ExceptionCheck(control->env)) return 0;
    }
    return 1;
}

static int m3_order_tick(M3OrderControl *control) {
    control->ticks++;
    return (control->ticks & UINT64_C(4095)) != 0 || m3_order_poll(control);
}

static int m3_order_charge(M3OrderControl *control) {
    if (control->used == control->budget) {
        m3_throw(control->env, "java/lang/IllegalArgumentException", "native sort comparison budget exceeded");
        return 0;
    }
    control->used++;
    return m3_order_tick(control);
}

static int m3_order_compare(M3OrderControl *control, const unsigned char *source,
                            const jint *offsets, jint a, jint b, int reverse, int *comparison) {
    if (!m3_order_charge(control)) return 0;
    jint ai = offsets[a], ae = offsets[a + 1], bi = offsets[b], be = offsets[b + 1];
    int value = 0;
    while (ai < ae && bi < be) {
        if (!m3_order_charge(control)) return 0;
        unsigned char x = source[ai++], y = source[bi++];
        if (x != y) { value = x < y ? -1 : 1; break; }
    }
    if (value == 0) value = ai == ae ? (bi == be ? 0 : -1) : 1;
    *comparison = reverse ? -value : value;
    return 1;
}

JNIEXPORT jintArray JNICALL
Java_jdk_m3_text_NativeLineBatch_order0(
        JNIEnv *env, jclass type, jbyteArray packedArray, jobject direct,
        jintArray offsetArray, jlongArray selectedArray, jboolean reverse,
        jboolean distinct, jlong comparisonBudget, jobject monitor) {
    (void)type;
    jintArray result = NULL;
    jint *offsets = NULL, *order = NULL, *scratch = NULL;
    jlong *selected = NULL;
    jbyte *packed = NULL;
    const unsigned char *source = NULL;
    jsize sourceLength = 0, lineCount = 0, count = 0;
    M3OrderControl control = {env, monitor, NULL, 0, 0, 0};
    if ((packedArray == NULL) == (direct == NULL) || offsetArray == NULL
            || selectedArray == NULL || comparisonBudget < 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "native sort request");
        return NULL;
    }
    control.budget = (uint64_t)comparisonBudget;
    if (monitor != NULL) {
        jclass monitorClass = (*env)->GetObjectClass(env, monitor);
        if (monitorClass == NULL) return NULL;
        control.checkCanceled = (*env)->GetMethodID(env, monitorClass, "checkCancelled", "()V");
        (*env)->DeleteLocalRef(env, monitorClass);
        if (control.checkCanceled == NULL || !m3_order_poll(&control)) return NULL;
    }
    if (packedArray != NULL) {
        sourceLength = (*env)->GetArrayLength(env, packedArray);
        packed = (*env)->GetByteArrayElements(env, packedArray, NULL);
        if (packed == NULL) return NULL;
        source = (const unsigned char *)packed;
    } else {
        source = (const unsigned char *)(*env)->GetDirectBufferAddress(env, direct);
        jlong capacity = (*env)->GetDirectBufferCapacity(env, direct);
        if (capacity < 0 || capacity > INT32_MAX || (source == NULL && capacity != 0)) {
            m3_throw(env, "java/lang/IllegalArgumentException", "invalid M3 direct sort buffer");
            goto done;
        }
        sourceLength = (jsize)capacity;
    }
    jsize offsetsLength = (*env)->GetArrayLength(env, offsetArray);
    if (offsetsLength == 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "empty sort offset geometry");
        goto done;
    }
    lineCount = offsetsLength - 1;
    jsize maskWords = (jsize)((((uint64_t)lineCount) + 63) >> 6);
    if ((*env)->GetArrayLength(env, selectedArray) != maskWords) {
        m3_throw(env, "java/lang/IllegalArgumentException", "sort selection geometry");
        goto done;
    }
    offsets = (*env)->GetIntArrayElements(env, offsetArray, NULL);
    if (offsets == NULL) goto done;
    selected = (*env)->GetLongArrayElements(env, selectedArray, NULL);
    if (selected == NULL) goto done;
    if (offsets[0] != 0 || offsets[lineCount] != sourceLength) {
        m3_throw(env, "java/lang/IllegalArgumentException", "sort offset boundaries");
        goto done;
    }
    for (jsize i = 1; i <= lineCount; i++) {
        if (!m3_order_tick(&control)) goto done;
        if (offsets[i] < offsets[i - 1] || offsets[i] > sourceLength) {
            m3_throw(env, "java/lang/IllegalArgumentException", "sort nonmonotone offsets");
            goto done;
        }
    }
    if ((lineCount & 63) != 0 && maskWords != 0) {
        uint64_t valid = (UINT64_C(1) << (lineCount & 63)) - 1;
        if (((uint64_t)selected[maskWords - 1] & ~valid) != 0) {
            m3_throw(env, "java/lang/IllegalArgumentException", "sort out-of-range selection");
            goto done;
        }
    }
    for (jsize i = 0; i < lineCount; i++) {
        if (!m3_order_tick(&control)) goto done;
        if (m3_selected(selected, i)) count++;
    }
    if ((uint64_t)count * sizeof(jint) > SIZE_MAX) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native sort index size overflow");
        goto done;
    }
    size_t size = (size_t)(count == 0 ? 1 : count) * sizeof(jint);
    order = (jint *)malloc(size);
    scratch = (jint *)malloc(size);
    if (order == NULL || scratch == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "native sort index allocation");
        goto done;
    }
    jsize at = 0;
    for (jsize i = 0; i < lineCount; i++) {
        if (!m3_order_tick(&control)) goto done;
        if (m3_selected(selected, i)) order[at++] = i;
    }
    for (uint64_t width = 1; width < (uint64_t)count; width *= 2) {
        if (!m3_order_poll(&control)) goto done;
        for (uint64_t lo = 0; lo < (uint64_t)count; lo += 2 * width) {
            uint64_t mid = lo + width < (uint64_t)count ? lo + width : (uint64_t)count;
            uint64_t hi = mid + width < (uint64_t)count ? mid + width : (uint64_t)count;
            uint64_t a = lo, b = mid, out = lo;
            while (a < mid && b < hi) {
                int comparison;
                if (!m3_order_compare(&control, source, offsets, order[a], order[b],
                                      reverse != JNI_FALSE, &comparison)) goto done;
                /* Choosing left on equality preserves original selected-row order. */
                scratch[out++] = comparison <= 0 ? order[a++] : order[b++];
            }
            while (a < mid) {
                if (!m3_order_tick(&control)) goto done;
                scratch[out++] = order[a++];
            }
            while (b < hi) {
                if (!m3_order_tick(&control)) goto done;
                scratch[out++] = order[b++];
            }
        }
        jint *swap = order; order = scratch; scratch = swap;
    }
    jsize kept = count;
    if (distinct != JNI_FALSE && count != 0) {
        kept = 1;
        for (jsize i = 1; i < count; i++) {
            int comparison;
            if (!m3_order_compare(&control, source, offsets, order[kept - 1], order[i],
                                  0, &comparison)) goto done;
            if (comparison != 0) order[kept++] = order[i];
        }
    }
    if (!m3_order_poll(&control)) goto done;
    result = (*env)->NewIntArray(env, kept);
    if (result != NULL && kept != 0) (*env)->SetIntArrayRegion(env, result, 0, kept, order);
done:
    free(scratch);
    free(order);
    if (selected != NULL) (*env)->ReleaseLongArrayElements(env, selectedArray, selected, JNI_ABORT);
    if (offsets != NULL) (*env)->ReleaseIntArrayElements(env, offsetArray, offsets, JNI_ABORT);
    if (packed != NULL) (*env)->ReleaseByteArrayElements(env, packedArray, packed, JNI_ABORT);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
