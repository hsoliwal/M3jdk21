/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#include "jvm.h"
#include "java_lang_String.h"
#include <limits.h>
#include <stdint.h>
#include <stdlib.h>

JNIEXPORT jobject JNICALL
Java_java_lang_String_intern(JNIEnv *env, jobject this)
{
    return JVM_InternString(env, this);
}

JNIEXPORT jboolean JNICALL
Java_java_lang_StringUTF16_isBigEndian(JNIEnv *env, jclass cls)
{
  unsigned int endianTest = 0xff000000;
  if (((char*)(&endianTest))[0] != 0) {
    return JNI_TRUE;
  } else {
    return JNI_FALSE;
  }
}


/*
 * M3String compatibility shadows.
 *
 * Canonical M3 text remains behind the owner/coordinate representation.
 * These entry points allocate final Java arrays only at an explicit compatibility boundary.
 */
static void m3_throw(JNIEnv *env, const char *name, const char *message) {
    jclass type = (*env)->FindClass(env, name);
    if (type != NULL) {
        (*env)->ThrowNew(env, type, message);
    }
}

static jmethodID m3_char_at_method(JNIEnv *env, jobject value) {
    jclass type = (*env)->GetObjectClass(env, value);
    if (type == NULL) return NULL;
    return (*env)->GetMethodID(env, type, "charAt", "(I)C");
}

JNIEXPORT jbyteArray JNICALL
Java_java_lang_M3String_nativeByteShadow(
        JNIEnv *env, jclass ignored, jobject value, jint start, jint length, jbyte coder)
{
    if (value == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "M3String");
        return NULL;
    }
    if (start < 0 || length < 0 || (coder != 0 && coder != 1)) {
        m3_throw(env, "java/lang/IllegalArgumentException", "invalid M3 shadow range/coder");
        return NULL;
    }
    jlong byte_length = ((jlong)length) << coder;
    if (byte_length > INT_MAX) {
        m3_throw(env, "java/lang/OutOfMemoryError", "M3 byte shadow too large");
        return NULL;
    }

    jbyteArray result = (*env)->NewByteArray(env, (jsize)byte_length);
    if (result == NULL || byte_length == 0) return result;

    jmethodID char_at = m3_char_at_method(env, value);
    if (char_at == NULL) return NULL;

    jbyte *bytes = (jbyte *)malloc((size_t)byte_length);
    if (bytes == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "M3 byte shadow");
        return NULL;
    }

    uint16_t endian_test = UINT16_C(1);
    int little_endian = *((uint8_t *)&endian_test) == 1u;
    for (jint index = 0; index < length; index++) {
        jchar unit = (*env)->CallCharMethod(env, value, char_at, start + index);
        if ((*env)->ExceptionCheck(env)) {
            free(bytes);
            return NULL;
        }
        if (coder == 0) {
            bytes[index] = (jbyte)unit;
        } else {
            jint at = index << 1;
            if (little_endian) {
                bytes[at] = (jbyte)unit;
                bytes[at + 1] = (jbyte)(unit >> 8);
            } else {
                bytes[at] = (jbyte)(unit >> 8);
                bytes[at + 1] = (jbyte)unit;
            }
        }
    }
    (*env)->SetByteArrayRegion(env, result, 0, (jsize)byte_length, bytes);
    free(bytes);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

JNIEXPORT jcharArray JNICALL
Java_java_lang_M3String_nativeCharShadow(
        JNIEnv *env, jclass ignored, jobject value, jint start, jint length)
{
    if (value == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "M3String");
        return NULL;
    }
    if (start < 0 || length < 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "invalid M3 char shadow range");
        return NULL;
    }

    jcharArray result = (*env)->NewCharArray(env, length);
    if (result == NULL || length == 0) return result;

    jmethodID char_at = m3_char_at_method(env, value);
    if (char_at == NULL) return NULL;
    jchar *chars = (jchar *)malloc((size_t)length * sizeof(jchar));
    if (chars == NULL) {
        m3_throw(env, "java/lang/OutOfMemoryError", "M3 char shadow");
        return NULL;
    }
    for (jint index = 0; index < length; index++) {
        chars[index] = (*env)->CallCharMethod(env, value, char_at, start + index);
        if ((*env)->ExceptionCheck(env)) {
            free(chars);
            return NULL;
        }
    }
    (*env)->SetCharArrayRegion(env, result, 0, length, chars);
    free(chars);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
