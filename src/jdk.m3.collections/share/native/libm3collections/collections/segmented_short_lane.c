/* SPDX-License-Identifier: Apache-2.0
 * Modified 2026: all-admitting predicates use only range metadata and bounded output writes.
 * Optional read-only page kernel for the existing SegmentedShortLane28 owner.
 * Follows segmented_bit_lane.c's bounded JNI region-copy discipline.
 * No global references, retained pointers, critical array pins, or native payload store.
 */
/* Lineage: com.synexia 537b10a9de1f synexia-common/src/main/native/collections/segmented_short_lane.c blob 01ac1d765034 lines 1-148 (M3FlagPageQuery appendix not absorbed: no M3 owner) (com.synexia.common.collections.SegmentedShortLane28 -> jdk.internal.m3.collections.range.SegmentedShortLane28, 2 symbols). JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */
#include <jni.h>
#include <stdint.h>

#define SHORT_QUERY_CHUNK 256

static void invalid(JNIEnv *env, const char *message) {
    if ((*env)->ExceptionCheck(env)) return;
    jclass type = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
        (*env)->DeleteLocalRef(env, type);
    }
}

static int matches_word(jshort value, jint required, jint forbidden) {
    uint32_t word = (uint16_t)value;
    return (word & (uint32_t)required) == (uint32_t)required
            && (word & (uint32_t)forbidden) == 0U;
}

static jint count_matches(
        JNIEnv *env, jshortArray page, jint from, jint to, jint required, jint forbidden) {
    jshort words[SHORT_QUERY_CHUNK];
    jint total = 0;
    for (jint cursor = from; cursor < to; ) {
        jint remaining = to - cursor;
        jint chunk = remaining < SHORT_QUERY_CHUNK ? remaining : SHORT_QUERY_CHUNK;
        (*env)->GetShortArrayRegion(env, page, cursor, chunk, words);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jint i = 0; i < chunk; i++) {
            if (matches_word(words[i], required, forbidden)) total++;
        }
        cursor += chunk;
    }
    return total;
}

static jint next_match(
        JNIEnv *env, jshortArray page, jint from, jint to, jint base,
        jint required, jint forbidden) {
    jshort words[SHORT_QUERY_CHUNK];
    for (jint cursor = from; cursor < to; ) {
        jint remaining = to - cursor;
        jint chunk = remaining < SHORT_QUERY_CHUNK ? remaining : SHORT_QUERY_CHUNK;
        (*env)->GetShortArrayRegion(env, page, cursor, chunk, words);
        if ((*env)->ExceptionCheck(env)) return 0;
        for (jint i = 0; i < chunk; i++) {
            if (matches_word(words[i], required, forbidden)) return base + cursor + i;
        }
        cursor += chunk;
    }
    return -1;
}

static jint copy_matches(
        JNIEnv *env, jshortArray page, jint from, jint to, jint base,
        jint required, jint forbidden, jintArray output, jint offset, jint maximum) {
    jshort words[SHORT_QUERY_CHUNK];
    jint matches[SHORT_QUERY_CHUNK];
    jint total = 0;
    for (jint cursor = from; cursor < to; ) {
        jint remaining = to - cursor;
        jint chunk = remaining < SHORT_QUERY_CHUNK ? remaining : SHORT_QUERY_CHUNK;
        (*env)->GetShortArrayRegion(env, page, cursor, chunk, words);
        if ((*env)->ExceptionCheck(env)) return 0;

        jint emitted = 0;
        for (jint i = 0; i < chunk; i++) {
            if (!matches_word(words[i], required, forbidden)) continue;
            matches[emitted++] = base + cursor + i;
            total++;
            if (total == maximum) break;
        }

        if (emitted != 0) {
            (*env)->SetIntArrayRegion(env, output, offset + total - emitted, emitted, matches);
            if ((*env)->ExceptionCheck(env)) return 0;
        }
        if (total == maximum) return total;
        cursor += chunk;
    }
    return total;
}

/* Metadata-only range atom. Validation remains in queryPage0 before this is called. */
static jint all_matches(JNIEnv *env, jint from, jint to, jint base, jint mode,
        jintArray output, jint offset, jint maximum) {
    if (mode == 0) return to - from;
    if (mode == 1) return base + from;
    jint count = maximum < to - from ? maximum : to - from;
    jint indices[SHORT_QUERY_CHUNK];
    for (jint total = 0; total < count; ) {
        jint remaining = count - total;
        jint chunk = remaining < SHORT_QUERY_CHUNK ? remaining : SHORT_QUERY_CHUNK;
        for (jint i = 0; i < chunk; i++) indices[i] = base + from + total + i;
        (*env)->SetIntArrayRegion(env, output, offset + total, chunk, indices);
        if ((*env)->ExceptionCheck(env)) return 0;
        total += chunk;
    }
    return count;
}

JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_range_SegmentedShortLane28_queryAbi0(JNIEnv *env, jclass type) {
    (void)env; (void)type;
    return 1;
}

JNIEXPORT jint JNICALL
Java_jdk_internal_m3_collections_range_SegmentedShortLane28_queryPage0(
        JNIEnv *env, jclass type, jshortArray page, jint from, jint to, jint base,
        jint required, jint forbidden, jint mode, jintArray output, jint offset, jint maximum) {
    (void)type;
    if (page == NULL || from < 0 || to < from || to > 4096
            || base < 0 || base > ((1 << 28) - 4096) || (base & 4095) != 0
            || required < 0 || required > 65535 || forbidden < 0 || forbidden > 65535
            || mode < 0 || mode > 2) {
        invalid(env, "invalid short query page/range/mask/mode"); return 0;
    }
    if ((*env)->GetArrayLength(env, page) != 4096) {
        invalid(env, "invalid short query page length"); return 0;
    }
    if (mode == 2) {
        if (output == NULL || offset < 0 || maximum < 0) {
            invalid(env, "invalid short query output"); return 0;
        }
        jint capacity = (*env)->GetArrayLength(env, output);
        if (offset > capacity || maximum > capacity - offset) {
            invalid(env, "short query output range"); return 0;
        }
    }
    if (from == to || (required & forbidden) != 0 || (mode == 2 && maximum == 0)) {
        return mode == 1 ? -1 : 0;
    }

    if ((required | forbidden) == 0) {
        return all_matches(env, from, to, base, mode, output, offset, maximum);
    }
    if (mode == 0) return count_matches(env, page, from, to, required, forbidden);
    if (mode == 1) return next_match(env, page, from, to, base, required, forbidden);
    return copy_matches(env, page, from, to, base, required, forbidden, output, offset, maximum);
}
