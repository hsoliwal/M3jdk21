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

/*
 * M3 known-slice array materialization. Source-of-truth M3String composition is
 * owner/coordinate based; these four receivers produce only one final genuine
 * Java array when an explicit caller requires a contiguous mutable result.
 * No Java heap array is pinned and no native pointer survives this invocation.
 */
enum { M3_SLICE_BLOCK = 2048 };

static int m3_slice_valid(JNIEnv *env, jlong capacity, jint start, jint count) {
    if (start < 0 || count < 0
            || (jlong)start > capacity || (jlong)count > capacity - start) {
        m3_throw(env, "java/lang/IndexOutOfBoundsException", "invalid M3 source slice");
        return 0;
    }
    return 1;
}

static jint m3_concat_length(JNIEnv *env, jint left, jint right) {
    jlong total = (jlong)left + right;
    if (total > INT_MAX) {
        m3_throw(env, "java/lang/OutOfMemoryError", "M3 destination array too large");
        return -1;
    }
    return (jint)total;
}

static int m3_copy_heap_chars(JNIEnv *env, jcharArray source, jint from,
        jcharArray target, jint destination, jint count) {
    jchar block[M3_SLICE_BLOCK];
    for (jint at = 0; at < count;) {
        jint amount = count - at;
        if (amount > M3_SLICE_BLOCK) amount = M3_SLICE_BLOCK;
        (*env)->GetCharArrayRegion(env, source, from + at, amount, block);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->SetCharArrayRegion(env, target, destination + at, amount, block);
        if ((*env)->ExceptionCheck(env)) return 0;
        at += amount;
    }
    return 1;
}

static int m3_copy_heap_bytes(JNIEnv *env, jbyteArray source, jint from,
        jbyteArray target, jint destination, jint count) {
    jbyte block[M3_SLICE_BLOCK];
    for (jint at = 0; at < count;) {
        jint amount = count - at;
        if (amount > M3_SLICE_BLOCK) amount = M3_SLICE_BLOCK;
        (*env)->GetByteArrayRegion(env, source, from + at, amount, block);
        if ((*env)->ExceptionCheck(env)) return 0;
        (*env)->SetByteArrayRegion(env, target, destination + at, amount, block);
        if ((*env)->ExceptionCheck(env)) return 0;
        at += amount;
    }
    return 1;
}

JNIEXPORT jcharArray JNICALL
Java_java_lang_M3String_nativeConcatKnownChars(JNIEnv *env, jclass ignored,
        jcharArray left, jint left_from, jint left_count,
        jcharArray right, jint right_from, jint right_count) {
    (void)ignored;
    if (left == NULL || right == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "M3 char source");
        return NULL;
    }
    if (!m3_slice_valid(env, (*env)->GetArrayLength(env, left), left_from, left_count)
            || !m3_slice_valid(env, (*env)->GetArrayLength(env, right), right_from, right_count))
        return NULL;
    jint length = m3_concat_length(env, left_count, right_count);
    if (length < 0) return NULL;
    jcharArray result = (*env)->NewCharArray(env, length);
    if (result == NULL) return NULL;
    if (!m3_copy_heap_chars(env, left, left_from, result, 0, left_count)
            || !m3_copy_heap_chars(env, right, right_from, result, left_count, right_count)) {
        (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_java_lang_M3String_nativeConcatKnownBytes(JNIEnv *env, jclass ignored,
        jbyteArray left, jint left_from, jint left_count,
        jbyteArray right, jint right_from, jint right_count) {
    (void)ignored;
    if (left == NULL || right == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "M3 byte source");
        return NULL;
    }
    if (!m3_slice_valid(env, (*env)->GetArrayLength(env, left), left_from, left_count)
            || !m3_slice_valid(env, (*env)->GetArrayLength(env, right), right_from, right_count))
        return NULL;
    jint length = m3_concat_length(env, left_count, right_count);
    if (length < 0) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, length);
    if (result == NULL) return NULL;
    if (!m3_copy_heap_bytes(env, left, left_from, result, 0, left_count)
            || !m3_copy_heap_bytes(env, right, right_from, result, left_count, right_count)) {
        (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}

/* The JNI argument owners retain the direct buffers for the complete call.
 * This does not protect against an *explicit concurrent unmap* by the caller:
 * M3 canonical arena ownership must prohibit it until all views have finished.
 */
static const unsigned char *m3_direct_slice(JNIEnv *env, jobject buffer,
        jint start, jint count, jint width) {
    if (buffer == NULL) {
        m3_throw(env, "java/lang/NullPointerException", "M3 direct source");
        return NULL;
    }
    jlong capacity = (*env)->GetDirectBufferCapacity(env, buffer);
    void *address = (*env)->GetDirectBufferAddress(env, buffer);
    if (capacity < 0 || (capacity > 0 && address == NULL)
            || (capacity % width) != 0) {
        m3_throw(env, "java/lang/IllegalArgumentException", "aligned direct array required");
        return NULL;
    }
    if (!m3_slice_valid(env, capacity / width, start, count))
        return NULL;
    return address == NULL ? NULL : (const unsigned char *)address + (size_t)start * width;
}

static int m3_copy_direct_chars(JNIEnv *env, const unsigned char *source,
        jcharArray target, jint at, jint count, jboolean big_endian) {
    jchar block[M3_SLICE_BLOCK];
    for (jint done = 0; done < count;) {
        jint amount = count - done;
        if (amount > M3_SLICE_BLOCK) amount = M3_SLICE_BLOCK;
        for (jint i = 0; i < amount; i++) {
            const size_t offset = (size_t)(done + i) * 2;
            const unsigned char first = source[offset];
            const unsigned char second = source[offset + 1];
            block[i] = big_endian
                    ? (jchar)((first << 8) | second)
                    : (jchar)(first | (second << 8));
        }
        (*env)->SetCharArrayRegion(env, target, at + done, amount, block);
        if ((*env)->ExceptionCheck(env)) return 0;
        done += amount;
    }
    return 1;
}

static int m3_copy_direct_bytes(JNIEnv *env, const unsigned char *source,
        jbyteArray target, jint at, jint count) {
    for (jint done = 0; done < count;) {
        jint amount = count - done;
        if (amount > M3_SLICE_BLOCK) amount = M3_SLICE_BLOCK;
        (*env)->SetByteArrayRegion(env, target, at + done, amount,
                (const jbyte *)(source + done));
        if ((*env)->ExceptionCheck(env)) return 0;
        done += amount;
    }
    return 1;
}

JNIEXPORT jcharArray JNICALL
Java_java_lang_M3String_nativeConcatMappedUtf16(JNIEnv *env, jclass ignored,
        jobject left, jint left_from, jint left_count, jboolean left_big_endian,
        jobject right, jint right_from, jint right_count, jboolean right_big_endian) {
    (void)ignored;
    const unsigned char *a = m3_direct_slice(env, left, left_from, left_count, 2);
    if ((*env)->ExceptionCheck(env)) return NULL;
    const unsigned char *b = m3_direct_slice(env, right, right_from, right_count, 2);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jint length = m3_concat_length(env, left_count, right_count);
    if (length < 0) return NULL;
    jcharArray result = (*env)->NewCharArray(env, length);
    if (result == NULL) return NULL;
    if ((left_count != 0 && !m3_copy_direct_chars(env, a, result, 0, left_count, left_big_endian))
            || (right_count != 0 && !m3_copy_direct_chars(
                    env, b, result, left_count, right_count, right_big_endian))) {
        (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_java_lang_M3String_nativeConcatMappedBytes(JNIEnv *env, jclass ignored,
        jobject left, jint left_from, jint left_count,
        jobject right, jint right_from, jint right_count) {
    (void)ignored;
    const unsigned char *a = m3_direct_slice(env, left, left_from, left_count, 1);
    if ((*env)->ExceptionCheck(env)) return NULL;
    const unsigned char *b = m3_direct_slice(env, right, right_from, right_count, 1);
    if ((*env)->ExceptionCheck(env)) return NULL;
    jint length = m3_concat_length(env, left_count, right_count);
    if (length < 0) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, length);
    if (result == NULL) return NULL;
    if ((left_count != 0 && !m3_copy_direct_bytes(env, a, result, 0, left_count))
            || (right_count != 0 && !m3_copy_direct_bytes(
                    env, b, result, left_count, right_count))) {
        (*env)->DeleteLocalRef(env, result);
        return NULL;
    }
    return result;
}
