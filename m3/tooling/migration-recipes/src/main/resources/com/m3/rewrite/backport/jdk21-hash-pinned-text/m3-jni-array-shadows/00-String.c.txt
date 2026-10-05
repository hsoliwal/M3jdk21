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

static jmethodID m3_method(
        JNIEnv *env, jobject value, const char *name, const char *signature)
{
    jclass type = (*env)->GetObjectClass(env, value);
    if (type == NULL) return NULL;
    return (*env)->GetMethodID(env, type, name, signature);
}

JNIEXPORT jbyteArray JNICALL
Java_java_lang_M3String_nativeAllocateByteShadow(
        JNIEnv *env, jclass ignored, jint length)
{
    if (length < 0) {
        m3_throw(env, "java/lang/NegativeArraySizeException",
                 "negative M3 byte shadow length");
        return NULL;
    }
    return (*env)->NewByteArray(env, length);
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

    jmethodID get_bytes = m3_method(env, value, "getBytes", "([BIIBI)V");
    if (get_bytes == NULL) return NULL;

    jvalue args[5];
    args[0].l = result;
    args[1].i = start;
    args[2].i = 0;
    args[3].b = coder;
    args[4].i = length;
    (*env)->CallVoidMethodA(env, value, get_bytes, args);
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

    jmethodID get_chars = m3_method(env, value, "getChars", "(II[CI)V");
    if (get_chars == NULL) return NULL;

    jvalue args[4];
    args[0].i = start;
    args[1].i = start + length;
    args[2].l = result;
    args[3].i = 0;
    (*env)->CallVoidMethodA(env, value, get_chars, args);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}
