/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright 2026 Hitesh Soliwal and contributors */
/* Test-only independent UTF-16 exact lexeme transition oracle; never loaded by product. */
#include <jni.h>

JNIEXPORT jint JNICALL
Java_jdk_m3_regexcursor_M3StaticGrammarLexiconNativeProof_exactNative(
        JNIEnv *env, jclass unused, jobject lexicon, jstring input, jint from, jint to) {
    (void)unused;
    if (lexicon == NULL || input == NULL) return -1;
    jsize n = (*env)->GetStringLength(env, input);
    if (from < 0 || to < from || to > n) return -1;
    jclass cls = (*env)->GetObjectClass(env, lexicon);
    if (cls == NULL) return -1;
    jfieldID fs = (*env)->GetFieldID(env, cls, "edgeStart", "[I");
    jfieldID fu = (*env)->GetFieldID(env, cls, "edgeUnit", "[C");
    jfieldID ft = (*env)->GetFieldID(env, cls, "edgeTarget", "[I");
    jfieldID fi = (*env)->GetFieldID(env, cls, "terminalId", "[I");
    if (fs == NULL || fu == NULL || ft == NULL || fi == NULL) return -1;
    jintArray startsObj = (jintArray)(*env)->GetObjectField(env, lexicon, fs);
    jcharArray unitsObj = (jcharArray)(*env)->GetObjectField(env, lexicon, fu);
    jintArray targetsObj = (jintArray)(*env)->GetObjectField(env, lexicon, ft);
    jintArray terminalObj = (jintArray)(*env)->GetObjectField(env, lexicon, fi);
    if (startsObj == NULL || unitsObj == NULL || targetsObj == NULL || terminalObj == NULL) return -1;
    const jchar *source = (*env)->GetStringChars(env, input, NULL);
    jint *starts = (*env)->GetIntArrayElements(env, startsObj, NULL);
    jchar *units = (*env)->GetCharArrayElements(env, unitsObj, NULL);
    jint *targets = (*env)->GetIntArrayElements(env, targetsObj, NULL);
    jint *terminals = (*env)->GetIntArrayElements(env, terminalObj, NULL);
    jint result = -1;
    if (source != NULL && starts != NULL && units != NULL && targets != NULL && terminals != NULL) {
        jint state = 0;
        for (jint i = from; i < to; ++i) {
            jint lo = starts[state];
            jint hi = starts[state + 1];
            jint target = -1;
            while (lo < hi) {
                jint mid = lo + ((hi - lo) / 2);
                if (units[mid] < source[i]) lo = mid + 1;
                else if (units[mid] > source[i]) hi = mid;
                else { target = targets[mid]; break; }
            }
            if (target < 0) { state = -1; break; }
            state = target;
        }
        if (state >= 0) result = terminals[state];
    }
    if (terminals != NULL) (*env)->ReleaseIntArrayElements(env, terminalObj, terminals, JNI_ABORT);
    if (targets != NULL) (*env)->ReleaseIntArrayElements(env, targetsObj, targets, JNI_ABORT);
    if (units != NULL) (*env)->ReleaseCharArrayElements(env, unitsObj, units, JNI_ABORT);
    if (starts != NULL) (*env)->ReleaseIntArrayElements(env, startsObj, starts, JNI_ABORT);
    if (source != NULL) (*env)->ReleaseStringChars(env, input, source);
    return result;
}
