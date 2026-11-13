/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * Threshold-band port of the existing EditDistance recurrence, raw UTF-16. */
#ifndef M3_EDIT_DISTANCE_H
#define M3_EDIT_DISTANCE_H
#include "m3_exact_summary.h"

typedef struct { jint start, count; jchar units[1024]; } M3EditPage;
static int m3_edit_unit(JNIEnv *env, jstring source, jint length, jint at,
                        M3EditPage *page, jchar *unit) {
    if (at < page->start || at >= page->start + page->count) {
        page->start = at;
        page->count = length - at;
        if (page->count > 1024) page->count = 1024;
        (*env)->GetStringRegion(env, source, page->start, page->count, page->units);
        if ((*env)->ExceptionCheck(env)) return 0;
    }
    *unit = page->units[at - page->start];
    return 1;
}
static jint m3_edit_min(jint a, jint b) { return a < b ? a : b; }
static jint m3_edit_value(const jint *row, jint low, jint high, jint at, jint infinity) {
    return at < low || at > high ? infinity : row[at - low];
}
JNIEXPORT jint JNICALL Java_jdk_m3_tools_text_similarity_EditDistance_withinNative0
  (JNIEnv *env, jclass owner, jstring left, jstring right, jint bound, jlong max_cells) {
    (void)owner;
    if (left == NULL || right == NULL) {
        m3_summary_throw(env, "java/lang/NullPointerException", "null distance text"); return -1;
    }
    jint a = (*env)->GetStringLength(env, left), b = (*env)->GetStringLength(env, right);
    if (a > (1 << 20) || b > (1 << 20) || bound < 0 || bound > 1000000
            || max_cells < 0 || max_cells > ((jlong)64 << 20)) {
        m3_summary_throw(env, "java/lang/IllegalArgumentException", "distance geometry/work budget"); return -1;
    }
    if (a < b) { jint size = a; a = b; b = size; jstring text = left; left = right; right = text; }
    jint infinity = bound + 1;
    if (a - b > bound) return infinity;
    if (b == 0) return m3_edit_min(a, infinity);
    jlong x = a > bound ? a - bound : 0, y = b > bound ? b - bound : 0;
    jlong cells = ((jlong)a + 1) * ((jlong)b + 1) - x * (x + 1) / 2 - y * (y + 1) / 2;
    if (cells > max_cells) {
        m3_summary_throw(env, "java/lang/IllegalArgumentException", "distance band work budget"); return -1;
    }
    jint width = m3_edit_min(b + 1, 2 * bound + 1);
    jint *storage = (jint *)m3_summary_alloc((size_t)2 * (size_t)width * sizeof(jint));
    if (storage == NULL) {
        m3_summary_throw(env, "java/lang/OutOfMemoryError", "distance band scratch"); return -1;
    }
    jint *previous = storage, *current = storage + width;
    jint previous_low = 0, previous_high = m3_edit_min(b, bound), result = infinity;
    for (jint j = 0; j <= previous_high; j++) previous[j] = j;
    M3EditPage left_page = {.start = -1, .count = 0};
    M3EditPage right_page = {.start = -1, .count = 0};
    for (jint i = 1; i <= a; i++) {
        jint low = i > bound ? i - bound : 0, high = m3_edit_min(b, i + bound);
        jint minimum = infinity;
        jchar unit;
        if (!m3_edit_unit(env, left, a, i - 1, &left_page, &unit)) goto done;
        for (jint j = low; j <= high; j++) {
            jint value;
            if (j == 0) value = i;
            else {
                jchar other;
                if (!m3_edit_unit(env, right, b, j - 1, &right_page, &other)) goto done;
                jint up = m3_edit_value(previous, previous_low, previous_high, j, infinity);
                jint diagonal = m3_edit_value(previous, previous_low, previous_high, j - 1, infinity);
                jint before = j == low ? infinity : current[j - low - 1];
                jint substitution = diagonal + (unit == other ? 0 : 1);
                value = m3_edit_min(infinity, m3_edit_min(substitution, m3_edit_min(up + 1, before + 1)));
            }
            current[j - low] = value;
            minimum = m3_edit_min(minimum, value);
        }
        if (minimum > bound) goto done;
        jint *swap = previous; previous = current; current = swap;
        previous_low = low; previous_high = high;
    }
    result = previous[b - previous_low];
done:
    free(storage);
    return result;
}
#endif
