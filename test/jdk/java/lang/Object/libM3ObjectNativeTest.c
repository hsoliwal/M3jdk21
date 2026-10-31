/*
 * Copyright (c) 2026, Hitesh Soliwal and Contributors to the Synexia Project.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <jni.h>

/* Exercise the actual product bridge, including with a patched stock JDK.
 * This does not qualify the installed exports of a rebuilt libjava. */
#include "../../../../../src/java.base/share/native/libjava/M3Object.c"

/* These fixed-field fixture writes are called by one serialized producer.
 * Separate Get/Set JNI calls are not advertised as atomic old/new capture. */
JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_writeReference(JNIEnv *env, jclass test_class,
                                      jobject source, jobject value,
                                      jclass declaring_type, jstring field_name,
                                      jclass field_type)
{
    jfieldID field;
    jobject previous;
    jboolean changed;

    (void)test_class;
    if (source == NULL) {
        return m3_object_throw(env, "java/lang/NullPointerException", "source");
    }
    field = (*env)->GetFieldID(env, declaring_type, "reference",
                              "Ljava/lang/Object;");
    if (field == NULL) {
        return JNI_FALSE;
    }
    previous = (*env)->GetObjectField(env, source, field);
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    changed = !(*env)->IsSameObject(env, previous, value);
    (*env)->DeleteLocalRef(env, previous);
    (*env)->SetObjectField(env, source, field, value);
    if ((*env)->ExceptionCheck(env) || !changed) {
        return JNI_FALSE;
    }
    return M3Object_NotifyFieldChanged(env, source, declaring_type, field_name,
                                      field_type, M3_OBJECT_REFERENCE);
}

JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_writeValue(JNIEnv *env, jclass test_class,
                                  jobject source, jint value,
                                  jclass declaring_type, jstring field_name,
                                  jclass field_type)
{
    jfieldID field;
    jint previous;

    (void)test_class;
    if (source == NULL) {
        return m3_object_throw(env, "java/lang/NullPointerException", "source");
    }
    field = (*env)->GetFieldID(env, declaring_type, "value", "I");
    if (field == NULL) {
        return JNI_FALSE;
    }
    previous = (*env)->GetIntField(env, source, field);
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    (*env)->SetIntField(env, source, field, value);
    if ((*env)->ExceptionCheck(env) || previous == value) {
        return JNI_FALSE;
    }
    return M3Object_NotifyFieldChanged(env, source, declaring_type, field_name,
                                      field_type, M3_OBJECT_VALUE);
}

/* Deterministic fixture for a committed store whose producer hook is delayed. */
JNIEXPORT void JNICALL
Java_M3ObjectNativeTest_storeReferenceOnly(JNIEnv *env, jclass test_class,
                                         jobject source, jobject value)
{
    jclass declaring_type;
    jfieldID field;

    (void)test_class;
    if (source == NULL) {
        m3_object_throw(env, "java/lang/NullPointerException", "source");
        return;
    }
    declaring_type = (*env)->GetObjectClass(env, source);
    if (declaring_type == NULL) {
        return;
    }
    field = (*env)->GetFieldID(env, declaring_type, "reference",
                              "Ljava/lang/Object;");
    (*env)->DeleteLocalRef(env, declaring_type);
    if (field == NULL) {
        return;
    }
    (*env)->SetObjectField(env, source, field, value);
}

JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_writeByte(JNIEnv *env, jclass test_class,
                                 jbyteArray source, jint index, jbyte value)
{
    jbyte previous;

    (void)test_class;
    if (source == NULL) {
        return m3_object_throw(env, "java/lang/NullPointerException", "source");
    }
    (*env)->GetByteArrayRegion(env, source, index, 1, &previous);
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    (*env)->SetByteArrayRegion(env, source, index, 1, &value);
    if ((*env)->ExceptionCheck(env) || previous == value) {
        return JNI_FALSE;
    }
    return M3Object_NotifyContentChanged(env, source);
}

JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_notifyContent(JNIEnv *env, jclass test_class,
                                     jobject source)
{
    (void)test_class;
    return M3Object_NotifyContentChanged(env, source);
}

JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_notifyField(JNIEnv *env, jclass test_class,
                                   jobject source, jclass declaring_type,
                                   jstring field_name, jclass field_type,
                                   jint mask)
{
    (void)test_class;
    return M3Object_NotifyFieldChanged(env, source, declaring_type, field_name,
                                      field_type, mask);
}

JNIEXPORT jboolean JNICALL
Java_M3ObjectNativeTest_pendingIsPreserved(JNIEnv *env, jclass test_class,
                                          jobject source, jthrowable expected)
{
    jboolean content_result;
    jboolean field_result;
    jboolean same;
    jthrowable actual;

    (void)test_class;
    if ((*env)->Throw(env, expected) != JNI_OK) {
        return JNI_FALSE;
    }
    content_result = M3Object_NotifyContentChanged(env, source);
    field_result = M3Object_NotifyFieldChanged(env, source, NULL, NULL, NULL, 0);
    actual = (*env)->ExceptionOccurred(env);
    if (actual == NULL) {
        return JNI_FALSE;
    }
    /* The fixture clears only to compare the preserved exception identity.
     * The product bridge must never clear or replace that exception. */
    (*env)->ExceptionClear(env);
    same = (*env)->IsSameObject(env, actual, expected);
    (*env)->DeleteLocalRef(env, actual);
    return !content_result && !field_result && same;
}

JNIEXPORT jint JNICALL
Java_M3ObjectNativeTest_mutateMany(JNIEnv *env, jclass test_class,
                                  jbyteArray source, jint count)
{
    jbyte value;
    jint completed;

    (void)test_class;
    if (source == NULL) {
        m3_object_throw(env, "java/lang/NullPointerException", "source");
        return 0;
    }
    (*env)->GetByteArrayRegion(env, source, 0, 1, &value);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }
    for (completed = 0; completed < count; completed++) {
        value = (jbyte)(value ^ 1);
        (*env)->SetByteArrayRegion(env, source, 0, 1, &value);
        if ((*env)->ExceptionCheck(env) ||
                !M3Object_NotifyContentChanged(env, source)) {
            break;
        }
    }
    return completed;
}
