/* Native port of the adapted Z-window in ExactSearch.prefixProfile.
 * Copyright 2026 Hitesh Soliwal and contributors (modifications).
 * SPDX-License-Identifier: MIT
 * MIT License
 *
 * Copyright (c) 2021 The Algorithms
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
#ifndef M3_PREFIX_PROFILE_H
#define M3_PREFIX_PROFILE_H
#include "m3_exact_summary.h"

/* Two independent ABI-copy windows: prefix cursor and suffix cursor. */
typedef struct {
    jint start;
    jint count;
    jchar units[1024];
} M3PrefixPage;

static int m3_prefix_unit(JNIEnv *env, jstring source, jint length, jint index,
                          M3PrefixPage *page, jchar *unit) {
    if (page->start < 0 || index < page->start || index - page->start >= page->count) {
        page->start = (index / 1024) * 1024;
        page->count = length - page->start;
        if (page->count > 1024) page->count = 1024;
        (*env)->GetStringRegion(env, source, page->start, page->count, page->units);
        if ((*env)->ExceptionCheck(env)) return 0;
    }
    *unit = page->units[index - page->start];
    return 1;
}

JNIEXPORT jintArray JNICALL Java_jdk_m3_tools_text_search_ExactSearch_prefixProfileNative0
  (JNIEnv *env, jclass owner, jstring source) {
    (void)owner;
    if (source == NULL) {
        m3_summary_throw(env, "java/lang/NullPointerException", "null prefix source");
        return NULL;
    }
    jsize length = (*env)->GetStringLength(env, source);
    if (length > (1 << 20)) {
        m3_summary_throw(env, "java/lang/IllegalArgumentException", "prefix UTF-16 budget");
        return NULL;
    }
    if (length == 0) return (*env)->NewIntArray(env, 0);
    jint *z = (jint *)m3_summary_alloc((size_t)length * sizeof(jint));
    if (z == NULL) {
        m3_summary_throw(env, "java/lang/OutOfMemoryError", "prefix metadata scratch");
        return NULL;
    }
    z[0] = length;
    M3PrefixPage prefix_page = {.start = -1, .count = 0};
    M3PrefixPage suffix_page = {.start = -1, .count = 0};
    jint left = 0, right = 0;
    for (jint i = 1; i < length; i++) {
        z[i] = 0;
        if (i < right) z[i] = z[i - left] < right - i ? z[i - left] : right - i;
        while (i + z[i] < length) {
            jchar a, b;
            if (!m3_prefix_unit(env, source, length, z[i], &prefix_page, &a)
                    || !m3_prefix_unit(env, source, length, i + z[i], &suffix_page, &b)) {
                free(z);
                return NULL;
            }
            if (a != b) break;
            z[i]++;
        }
        if (i + z[i] > right) { left = i; right = i + z[i]; }
    }
    jintArray result = (*env)->NewIntArray(env, length);
    if (result != NULL) {
        for (jint at = 0; at < length;) {
            jint count = length - at;
            if (count > 1024) count = 1024;
            (*env)->SetIntArrayRegion(env, result, at, count, z + at);
            if ((*env)->ExceptionCheck(env)) { result = NULL; break; }
            at += count;
        }
    }
    free(z);
    return result;
}
#endif
