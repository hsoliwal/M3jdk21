/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 */
// Relocated from com.synexia m3-fast-search/src/main/native/fixed_word_concat_jni.c (blob 93d84ee1e76e) by M3_SEARCH_RELOCATE v2.
#include <jni.h>
#include <stdint.h>
#include <string.h>

enum { M3_MAX_SOURCE = 8192, M3_MAX_WORDS = 32, M3_MAX_WIDTH = 16, M3_TABLE_SIZE = 128 };

/* Bounded invocation-local primitive lanes. No JNI resource can escape this call. */
typedef struct {
    jchar spellings[M3_MAX_WORDS][M3_MAX_WIDTH];
    uint64_t hashes[M3_MAX_WORDS];
    jint required[M3_MAX_WORDS];
    jint slots[M3_TABLE_SIZE];
    jint distinct;
    jint width;
} M3WordDict;

static void m3_throw(JNIEnv *env, const char *type, const char *message) {
    jclass klass = (*env)->FindClass(env, type);
    if (klass != NULL) {
        (*env)->ThrowNew(env, klass, message);
        (*env)->DeleteLocalRef(env, klass);
    }
}

static uint64_t m3_hash(const jchar *chars, jint width) {
    uint64_t value = UINT64_C(14695981039346656037);
    for (jint i = 0; i < width; i++) {
        value = (value ^ (uint64_t) chars[i]) * UINT64_C(1099511628211);
    }
    return value;
}

static jint m3_lookup(const M3WordDict *dict, const jchar *source) {
    uint64_t key = m3_hash(source, dict->width);
    uint32_t slot = (uint32_t) key & (uint32_t) (M3_TABLE_SIZE - 1);
    for (;;) {
        jint resident = dict->slots[slot] - 1;
        if (resident < 0) return -1;
        if (dict->hashes[resident] == key
                && memcmp(source, dict->spellings[resident],
                          (size_t) dict->width * sizeof(jchar)) == 0) {
            return resident;
        }
        slot = (slot + UINT32_C(1)) & (uint32_t)(M3_TABLE_SIZE - 1);
    }
}

/* Native export is PRIVATE to a package-local proof class; no existing JNI ABI changes. */
JNIEXPORT jintArray JNICALL
Java_jdk_m3_search_FixedWordConcatenation_findNative(
        JNIEnv *env, jclass unused, jcharArray sourceArray, jobjectArray wordsArray) {
    (void) unused;
    if (sourceArray == NULL || wordsArray == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "fixed-word source/words");
        return NULL;
    }
    jsize n = (*env)->GetArrayLength(env, sourceArray);
    jsize wordCount = (*env)->GetArrayLength(env, wordsArray);
    if (n > M3_MAX_SOURCE || wordCount == 0 || wordCount > M3_MAX_WORDS) {
        m3_throw(env, "java/lang/IllegalArgumentException", "fixed-word search budget");
        return NULL;
    }
    M3WordDict dict = {0};
    jint width = -1;
    for (jsize w = 0; w < wordCount; w++) {
        jcharArray word = (jcharArray)(*env)->GetObjectArrayElement(env, wordsArray, w);
        if ((*env)->ExceptionCheck(env)) return NULL;
        if (word == NULL) {
            m3_throw(env, "java/lang/NullPointerException", "null fixed word");
            return NULL;
        }
        jsize length = (*env)->GetArrayLength(env, word);
        if (width < 0) width = length;
        if (width == 0 || width > M3_MAX_WIDTH || length != width) {
            (*env)->DeleteLocalRef(env, word);
            m3_throw(env, "java/lang/IllegalArgumentException", "fixed-word width");
            return NULL;
        }
        jchar current[M3_MAX_WIDTH];
        (*env)->GetCharArrayRegion(env, word, 0, length, current);
        (*env)->DeleteLocalRef(env, word);
        if ((*env)->ExceptionCheck(env)) return NULL;

        uint64_t key = m3_hash(current, width);
        uint32_t slot = (uint32_t) key & (uint32_t) (M3_TABLE_SIZE - 1);
        for (;;) {
            jint resident = dict.slots[slot] - 1;
            if (resident < 0) {
                jint index = dict.distinct++;
                memcpy(dict.spellings[index], current, (size_t)width * sizeof(jchar));
                dict.hashes[index] = key;
                dict.required[index] = 1;
                dict.slots[slot] = index + 1;
                break;
            }
            if (dict.hashes[resident] == key
                    && memcmp(dict.spellings[resident], current,
                              (size_t)width * sizeof(jchar)) == 0) {
                dict.required[resident]++;
                break;
            }
            slot = (slot + UINT32_C(1)) & (uint32_t)(M3_TABLE_SIZE - 1);
        }
    }
    dict.width = width;
    if ((int64_t)wordCount * (int64_t)width > (int64_t)n) {
        return (*env)->NewIntArray(env, 0);
    }
    jchar *source = (*env)->GetCharArrayElements(env, sourceArray, NULL);
    if (source == NULL) return NULL;

    /* All mutable state is one-call stack storage; no per-token heap or JNI allocation. */
    jint seen[M3_MAX_WORDS] = {0};
    uint8_t hits[M3_MAX_SOURCE] = {0};
    jint out[M3_MAX_SOURCE];
    for (jint offset = 0; offset < width; offset++) {
        memset(seen, 0, sizeof(seen));
        jint left = offset;
        jint count = 0;
        for (jint right = offset; right + width <= n; right += width) {
            jint id = m3_lookup(&dict, source + right);
            if (id < 0) {
                memset(seen, 0, sizeof(seen));
                count = 0;
                left = right + width;
                continue;
            }
            seen[id]++;
            count++;
            while (seen[id] > dict.required[id]) {
                jint removed = m3_lookup(&dict, source + left);
                if (removed < 0) {
                    (*env)->ReleaseCharArrayElements(env, sourceArray, source, JNI_ABORT);
                    m3_throw(env, "java/lang/IllegalStateException", "invalid native active window");
                    return NULL;
                }
                seen[removed]--;
                count--;
                left += width;
            }
            if (count == wordCount) {
                hits[left] = UINT8_C(1);
                jint removed = m3_lookup(&dict, source + left);
                if (removed < 0) {
                    (*env)->ReleaseCharArrayElements(env, sourceArray, source, JNI_ABORT);
                    m3_throw(env, "java/lang/IllegalStateException", "invalid native matching window");
                    return NULL;
                }
                seen[removed]--;
                count--;
                left += width;
            }
        }
    }
    (*env)->ReleaseCharArrayElements(env, sourceArray, source, JNI_ABORT);
    jint matches = 0;
    for (jint i = 0; i < n; i++) {
        if (hits[i] != 0) out[matches++] = i;
    }
    jintArray result = (*env)->NewIntArray(env, matches);
    if (result != NULL && matches != 0) {
        (*env)->SetIntArrayRegion(env, result, 0, matches, out);
    }
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
