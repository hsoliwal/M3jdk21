/* SPDX-License-Identifier: Apache-2.0 */
/*
 * Original production JNI C owner is compiled and exercised directly.
 * LeetCode #35 lower-bound, #88 ordered-query merge; no copied kernel.
 */
#include <assert.h>
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include "../../main/native/collections/m3_span_index_jni.c"

static unsigned long long assertions;

static jint query_value(int mode, jsize i, jsize count) {
    switch (mode) {
        case 0: return i / 4;
        case 1: return count - i;
        case 2: return i % M3_SPAN_QUERY_PAGE;
        case 3: return ((i / 260) & 1) ? 1000 - (i % 260) : (i % 260);
        case 4: return i < count / 2 ? i / 3 : INT_MAX;
        case 5: return (i % 41 == 0) ? INT_MIN
                : (i % 19 == 0) ? -1 : (i % 17 == 0) ? 0 : i % 300;
        default: return 400;
    }
}

static void verify_case(jsize span_count, jsize query_count, int mode) {
    /* calloc(1) on empty Java-like lanes keeps the native pointer non-NULL. */
    jint *starts = calloc((size_t) span_count + 1, sizeof(*starts));
    jlong *prefix = calloc((size_t) span_count + 1, sizeof(*prefix));
    jint *queries = calloc((size_t) query_count + 1, sizeof(*queries));
    jint *old = calloc((size_t) query_count + 1, sizeof(*old));
    jint *fast = calloc((size_t) query_count + 1, sizeof(*fast));
    if (starts == NULL || prefix == NULL || queries == NULL
            || old == NULL || fast == NULL) abort();

    for (jsize i = 0; i < span_count; i++) {
        starts[i] = i;
        prefix[i] = (jlong) i + 15;
    }
    for (jsize i = 0; i < query_count; i++)
        queries[i] = query_value(mode, i, query_count);

    jsize cursor = 0;
    jint previous_last = 0;
    jboolean previous_sweep = JNI_FALSE;
    for (jsize done = 0; done < query_count;) {
        jsize page = query_count - done;
        if (page > M3_SPAN_QUERY_PAGE) page = M3_SPAN_QUERY_PAGE;
        jsize reset_cursor = 0;
        /* Old behavior (page-local reset) is an independent reference. */
        (void) span_answer_page(queries + done, page, starts, prefix,
                span_count, old + done, &reset_cursor, JNI_FALSE, 0);
        jsize cursor_before = cursor;
        jboolean saved_sweep = previous_sweep;
        jint saved_last = previous_last;
        previous_sweep = span_answer_page(queries + done, page,
                starts, prefix, span_count, fast + done, &cursor,
                previous_sweep, previous_last);
        previous_last = queries[done + page - 1];
        if (saved_sweep == JNI_TRUE && previous_sweep == JNI_TRUE
                && queries[done] >= saved_last) {
            if (cursor < cursor_before) {
                fprintf(stderr, "Cross-page monotone cursor moved backward\n");
                abort();
            }
            assertions++;
        }
        for (jsize i = 0; i < page; i++) {
            const jint wanted = earliest_covering(queries[done + i],
                    starts, prefix, span_count);
            if (old[done + i] != wanted || fast[done + i] != wanted) {
                fprintf(stderr,
                        "Mismatch mode=%d spans=%d query=%d got=%d reset=%d expected=%d\n",
                        mode, (int)span_count, (int)(done+i),
                        fast[done+i], old[done+i], wanted);
                abort();
            }
            assertions++;
        }
        done += page;
    }
    free(starts);
    free(prefix);
    free(queries);
    free(old);
    free(fast);
}

int main(void) {
    const jsize spans[] = {0, 1, 2, 33, 256, 1024, 8192};
    const jsize queries[] = {0, 1, 2, 255, 256, 257, 511, 512, 513, 5000};
    for (size_t n = 0; n < sizeof(spans)/sizeof(spans[0]); n++) {
        for (size_t q = 0; q < sizeof(queries)/sizeof(queries[0]); q++) {
            for (int mode = 0; mode <= 6; mode++)
                verify_case(spans[n], queries[q], mode);
        }
    }
    printf("PASS M3_SPAN_PAGE_CURSOR assertions=%llu\n", assertions);
    return 0;
}
