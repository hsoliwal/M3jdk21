// SPDX-License-Identifier: Apache-2.0
// C17 JNI test oracle only: independent scalar case rules and Liang pattern scoring.
// NOT a production JNI symbol set or a port of external hyphenation pattern data.
#include <jni.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

static void reject(JNIEnv *env, const char *message) {
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_m3_lexicon_M3CaseLiangJniParityProof_nativeBoundary(JNIEnv *env, jclass clazz,
        jint kind, jint before, jint at, jint after) {
    (void) env;
    (void) clazz;
    if (kind == 0) {
        return (jboolean) (at == 1 && (before == 2 || before == 9));
    }
    bool changed = at != before && !(at == 2 && before == 1);
    bool acronym = after == 2 && at == 1 && before == 1;
    return (jboolean) (changed || acronym);
}

static jchar lower_ascii(jchar c) {
    return (jchar) (c >= (jchar) 'A' && c <= (jchar) 'Z' ? c + 32u : c);
}

static jchar augmented(const jchar *word, jsize n, jsize offset) {
    if (offset == 0 || offset == n + 1) return (jchar) '.';
    return lower_ascii(word[offset - 1]);
}

JNIEXPORT jintArray JNICALL
Java_com_m3_lexicon_M3CaseLiangJniParityProof_nativeCuts(JNIEnv *env, jclass clazz,
        jstring word, jobjectArray patterns, jint left, jint right) {
    (void) clazz;
    if (word == NULL || patterns == NULL || left < 1 || right < 1) {
        reject(env, "invalid JNI hyphenation arguments");
        return NULL;
    }
    jsize n = (*env)->GetStringLength(env, word);
    if (n < 0 || n > 4096) {
        reject(env, "unsupported word length");
        return NULL;
    }
    const jchar *word_chars = (*env)->GetStringChars(env, word, NULL);
    if (word_chars == NULL) return NULL;
    for (jsize i = 0; i < n; i++) {
        jchar c = lower_ascii(word_chars[i]);
        if (c < (jchar) 'a' || c > (jchar) 'z') {
            (*env)->ReleaseStringChars(env, word, word_chars);
            reject(env, "non-ASCII JNI word refused");
            return NULL;
        }
    }
    uint8_t *scores = (uint8_t *) calloc((size_t) n + 3u, 1u);
    if (scores == NULL) {
        (*env)->ReleaseStringChars(env, word, word_chars);
        return NULL;
    }
    jsize count = (*env)->GetArrayLength(env, patterns);
    bool good = true;
    for (jsize p = 0; p < count && good; p++) {
        jstring raw = (jstring) (*env)->GetObjectArrayElement(env, patterns, p);
        if (raw == NULL) { good = false; break; }
        jsize plen = (*env)->GetStringLength(env, raw);
        if (plen <= 0 || plen > 64) {
            (*env)->DeleteLocalRef(env, raw);
            good = false;
            break;
        }
        const jchar *pattern = (*env)->GetStringChars(env, raw, NULL);
        if (pattern == NULL) {
            (*env)->DeleteLocalRef(env, raw);
            good = false;
            break;
        }
        jchar glyphs[65] = {0};
        uint8_t weights[66] = {0};
        jsize len = 0;
        bool last_digit = false;
        for (jsize j = 0; j < plen; j++) {
            jchar c = pattern[j];
            if (c >= (jchar) '0' && c <= (jchar) '9' && !last_digit) {
                weights[len] = (uint8_t) (c - (jchar) '0');
                last_digit = true;
            } else if ((c >= (jchar) 'a' && c <= (jchar) 'z')
                    || (c == (jchar) '.' && (j == 0 || j == plen - 1))) {
                glyphs[len++] = c;
                last_digit = false;
            } else { good = false; break; }
        }
        if (len == 0) good = false;
        if (good) {
            for (jsize start = 0; start + len <= n + 2; start++) {
                bool match = true;
                for (jsize k = 0; k < len; k++) {
                    if (glyphs[k] != augmented(word_chars, n, start + k)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    for (jsize k = 0; k <= len; k++) {
                        uint8_t *value = &scores[start + k];
                        if (*value < weights[k]) *value = weights[k];
                    }
                }
            }
        }
        (*env)->ReleaseStringChars(env, raw, pattern);
        (*env)->DeleteLocalRef(env, raw);
    }
    if (!good) {
        free(scores);
        (*env)->ReleaseStringChars(env, word, word_chars);
        if (!(*env)->ExceptionCheck(env)) reject(env, "invalid JNI Liang pattern");
        return NULL;
    }
    jintArray result = (*env)->NewIntArray(env, n + 1);
    if (result != NULL) {
        jint *cuts = (jint *) calloc((size_t) n + 1u, sizeof(jint));
        if (cuts == NULL) {
            (*env)->DeleteLocalRef(env, result);
            result = NULL;
        } else {
            for (jint k = left; k <= n - right; k++) {
                cuts[k] = (jint) (scores[k + 1] & 1u);
            }
            (*env)->SetIntArrayRegion(env, result, 0, n + 1, cuts);
            free(cuts);
        }
    }
    free(scores);
    (*env)->ReleaseStringChars(env, word, word_chars);
    return result;
}
