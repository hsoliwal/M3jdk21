// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors

#include <jni.h>
#include <limits.h>
#include <stdint.h>

static jlong cluster_score_q20(jint first, jint last, jint significant) {
    jint span = last - first + 1;
    uint64_t sig = (uint64_t)(uint32_t)significant;
    uint64_t numerator = sig * sig;
    if (numerator > ((uint64_t)INT64_MAX >> 20)) {
        return (jlong)(INT64_MAX / span);
    }
    return (jlong)((numerator << 20) / (uint64_t)(uint32_t)span);
}

JNIEXPORT jint JNICALL
Java_com_m3_lexicon_M3LanguageNative_scoreLuhnClusters0(
        JNIEnv *env,
        jclass ignored,
        jintArray word_ids,
        jintArray sentence_ends,
        jlongArray frequencies,
        jint word_count,
        jlong minimum_frequency,
        jlong maximum_frequency,
        jint maximum_gap,
        jlongArray out_score_q20,
        jintArray out_significant,
        jintArray out_span) {
    (void)ignored;

    if (word_ids == NULL || sentence_ends == NULL || frequencies == NULL
            || out_score_q20 == NULL || out_significant == NULL || out_span == NULL) {
        return -1;
    }
    if (word_count < 0 || minimum_frequency < 0
            || maximum_frequency < minimum_frequency || maximum_gap < 0) {
        return -2;
    }

    jsize word_length = (*env)->GetArrayLength(env, word_ids);
    jsize sentence_count = (*env)->GetArrayLength(env, sentence_ends);
    jsize frequency_length = (*env)->GetArrayLength(env, frequencies);
    if (frequency_length < (jsize)(word_count + 1)
            || (*env)->GetArrayLength(env, out_score_q20) != sentence_count
            || (*env)->GetArrayLength(env, out_significant) != sentence_count
            || (*env)->GetArrayLength(env, out_span) != sentence_count) {
        return -3;
    }

    jboolean words_copy = JNI_FALSE;
    jboolean ends_copy = JNI_FALSE;
    jboolean frequencies_copy = JNI_FALSE;
    jboolean score_copy = JNI_FALSE;
    jboolean significant_copy = JNI_FALSE;
    jboolean span_copy = JNI_FALSE;

    jint *words = (*env)->GetIntArrayElements(env, word_ids, &words_copy);
    jint *ends = (*env)->GetIntArrayElements(env, sentence_ends, &ends_copy);
    jlong *frequency = (*env)->GetLongArrayElements(env, frequencies, &frequencies_copy);
    jlong *scores = (*env)->GetLongArrayElements(env, out_score_q20, &score_copy);
    jint *significant_out = (*env)->GetIntArrayElements(env, out_significant, &significant_copy);
    jint *span_out = (*env)->GetIntArrayElements(env, out_span, &span_copy);

    if (words == NULL || ends == NULL || frequency == NULL || scores == NULL
            || significant_out == NULL || span_out == NULL) {
        if (span_out != NULL) (*env)->ReleaseIntArrayElements(env, out_span, span_out, 0);
        if (significant_out != NULL) {
            (*env)->ReleaseIntArrayElements(env, out_significant, significant_out, 0);
        }
        if (scores != NULL) (*env)->ReleaseLongArrayElements(env, out_score_q20, scores, 0);
        if (frequency != NULL) (*env)->ReleaseLongArrayElements(env, frequencies, frequency, JNI_ABORT);
        if (ends != NULL) (*env)->ReleaseIntArrayElements(env, sentence_ends, ends, JNI_ABORT);
        if (words != NULL) (*env)->ReleaseIntArrayElements(env, word_ids, words, JNI_ABORT);
        return -4;
    }

    jint status = 0;
    jint start = 0;
    for (jsize sentence = 0; sentence < sentence_count; sentence++) {
        jint end = ends[sentence];
        if (end <= start || end > word_length) {
            status = -5;
            break;
        }

        jlong best = 0;
        jint strongest_span = 0;
        jint significant_total = 0;
        jint cluster_first = -1;
        jint cluster_last = -1;
        jint cluster_significant = 0;

        for (jint position = start; position < end; position++) {
            jint cwid = words[position];
            if (cwid <= 0 || cwid > word_count) {
                status = -6;
                break;
            }
            jlong value = frequency[cwid];
            if (value < minimum_frequency || value > maximum_frequency) {
                continue;
            }

            significant_total++;
            if (cluster_first < 0) {
                cluster_first = position;
                cluster_last = position;
                cluster_significant = 1;
                continue;
            }

            if (position - cluster_last - 1 > maximum_gap) {
                jlong score = cluster_score_q20(
                        cluster_first, cluster_last, cluster_significant);
                jint span = cluster_last - cluster_first + 1;
                if (score > best || (score == best
                        && (strongest_span == 0 || span < strongest_span))) {
                    best = score;
                    strongest_span = span;
                }
                cluster_first = position;
                cluster_significant = 1;
            } else {
                cluster_significant++;
            }
            cluster_last = position;
        }
        if (status != 0) break;

        if (cluster_first >= 0) {
            jlong score = cluster_score_q20(
                    cluster_first, cluster_last, cluster_significant);
            jint span = cluster_last - cluster_first + 1;
            if (score > best || (score == best
                    && (strongest_span == 0 || span < strongest_span))) {
                best = score;
                strongest_span = span;
            }
        }

        scores[sentence] = best;
        significant_out[sentence] = significant_total;
        span_out[sentence] = strongest_span;
        start = end;
    }

    if (status == 0 && start != word_length) {
        status = -7;
    }

    (*env)->ReleaseIntArrayElements(env, out_span, span_out, status == 0 ? 0 : JNI_ABORT);
    (*env)->ReleaseIntArrayElements(
            env, out_significant, significant_out, status == 0 ? 0 : JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, out_score_q20, scores, status == 0 ? 0 : JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, frequencies, frequency, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, sentence_ends, ends, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, word_ids, words, JNI_ABORT);
    return status;
}
