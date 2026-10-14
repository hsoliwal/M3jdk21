/* Copyright (c) 2026, Hitesh Soliwal. GPLv2 with the Classpath exception. */
#include <jni.h>

JNIEXPORT jint JNICALL Java_M3StringModifiedUtf8DpTest_nativeModifiedLength(
        JNIEnv *env, jclass unused, jstring text) {
    (void) unused;
    if (text == NULL) return -1;
    return (*env)->GetStringUTFLength(env, text);
}

JNIEXPORT jbyteArray JNICALL Java_M3StringModifiedUtf8DpTest_nativeModifiedBytes(
        JNIEnv *env, jclass unused, jstring text) {
    (void) unused;
    if (text == NULL) return NULL;
    jsize bytes = (*env)->GetStringUTFLength(env, text);
    if ((*env)->ExceptionCheck(env) || bytes < 0) return NULL;
    const char *utf = (*env)->GetStringUTFChars(env, text, NULL);
    if (utf == NULL) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, bytes);
    if (result != NULL && bytes != 0) {
        (*env)->SetByteArrayRegion(env, result, 0, bytes, (const jbyte *) utf);
    }
    (*env)->ReleaseStringUTFChars(env, text, utf);
    return result;
}
