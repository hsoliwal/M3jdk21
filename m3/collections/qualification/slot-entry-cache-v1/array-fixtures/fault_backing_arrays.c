/* SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * TEST ONLY: exercises the Java boundary, never substitutes the production C owner.
 */
#include <jni.h>
#ifndef M3_PENDING
#error M3_PENDING must explicitly select the test-only failure mode
#endif
#if M3_PENDING != 0 && M3_PENDING != 1
#error invalid M3_PENDING test mode
#endif

static void m3_test_fault(JNIEnv *env) {
#if M3_PENDING
  if ((*env)->ExceptionCheck(env)) return;
  jclass type = (*env)->FindClass(env, "com/synexia/proof/M3BackingArrayContractV2");
  if (type == NULL || (*env)->ExceptionCheck(env)) {
    if (type != NULL) (*env)->DeleteLocalRef(env, type);
    return;
  }
  jfieldID field = (*env)->GetStaticFieldID(
      env, type, "MARKER", "Ljava/lang/IllegalStateException;");
  if (field == NULL || (*env)->ExceptionCheck(env)) {
    (*env)->DeleteLocalRef(env, type);
    return;
  }
  jobject marker = (*env)->GetStaticObjectField(env, type, field);
  if (!(*env)->ExceptionCheck(env) && marker != NULL) {
    (void)(*env)->Throw(env, (jthrowable)marker);
  }
  if (marker != NULL) (*env)->DeleteLocalRef(env, marker);
  (*env)->DeleteLocalRef(env, type);
#else
  (void)env;
#endif
}

JNIEXPORT jobjectArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateObjects(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner; (void)length;
  m3_test_fault(env);
  return NULL;
}
JNIEXPORT jbyteArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateBytes(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner; (void)length;
  m3_test_fault(env);
  return NULL;
}
JNIEXPORT jintArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateInts(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner; (void)length;
  m3_test_fault(env);
  return NULL;
}
JNIEXPORT jlongArray JNICALL
Java_com_m3_arrays_M3ArrayNative_nativeAllocateLongs(
    JNIEnv *env, jclass owner, jint length) {
  (void)owner; (void)length;
  m3_test_fault(env);
  return NULL;
}
