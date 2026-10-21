/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 * Test-only native caller. It exercises the real Java transfer owner, not an alternate engine.
 */
#include <jni.h>
#include "com_m3_lexicon_M3LexiconTransferCompositionProof.h"

JNIEXPORT jlong JNICALL
Java_com_m3_lexicon_M3LexiconTransferCompositionProof_inspectNative(
        JNIEnv *env, jclass proof_class, jobject table, jintArray ids,
        jint offset, jint length, jint state) {
    (void)proof_class;
    jclass table_class = NULL;
    jclass summary_class = NULL;
    jobject summary = NULL;
    jlong result = 0;
    if (table == NULL) {
        jclass error = (*env)->FindClass(env, "java/lang/NullPointerException");
        if (error != NULL) {
            (*env)->ThrowNew(env, error, "table");
            (*env)->DeleteLocalRef(env, error);
        }
        return 0;
    }
    table_class = (*env)->GetObjectClass(env, table);
    if (table_class == NULL) goto cleanup;
    jmethodID summarize = (*env)->GetMethodID(env, table_class, "summarize",
            "([III)Lcom/m3/lexicon/M3RegexWordTransferRows$TransferSummary;");
    if (summarize == NULL) goto cleanup;
    summary = (*env)->CallObjectMethod(env, table, summarize, ids, offset, length);
    if ((*env)->ExceptionCheck(env) || summary == NULL) goto cleanup;
    summary_class = (*env)->GetObjectClass(env, summary);
    if (summary_class == NULL) goto cleanup;
    jmethodID step = (*env)->GetMethodID(env, summary_class, "step", "(I)I");
    if (step == NULL) goto cleanup;
    jmethodID seen = (*env)->GetMethodID(env, summary_class, "acceptsInside", "(I)Z");
    if (seen == NULL) goto cleanup;
    jint exit_state = (*env)->CallIntMethod(env, summary, step, state);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    jboolean accepting = (*env)->CallBooleanMethod(env, summary, seen, state);
    if ((*env)->ExceptionCheck(env)) goto cleanup;
    result = (jlong)exit_state | (accepting == JNI_TRUE ? ((jlong)1 << 32) : (jlong)0);
cleanup:
    if (summary_class != NULL) (*env)->DeleteLocalRef(env, summary_class);
    if (summary != NULL) (*env)->DeleteLocalRef(env, summary);
    if (table_class != NULL) (*env)->DeleteLocalRef(env, table_class);
    return result;
}
