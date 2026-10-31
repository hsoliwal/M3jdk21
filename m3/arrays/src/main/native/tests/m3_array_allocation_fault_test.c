/* Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#define KINDS(X) X(Objects) X(Booleans) X(Bytes) X(Chars) X(Shorts) X(Ints) X(Longs) X(Floats) X(Doubles)
#define DECLARE(kind) extern jobject Java_jdk_m3_arrays_M3ArrayNative_nativeAllocate##kind(JNIEnv *, jclass, jint);
KINDS(DECLARE)

static int pending, allocations, finds, throws, deletes, mode;
static const char *exception_name;
static jint requested;
static jobject const result_handle = (jobject)(uintptr_t)0x100;
static jclass const class_handle = (jclass)(uintptr_t)0x200;
static jboolean JNICALL exception_check(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env; assert(!pending); finds++;
    if (strcmp(name, "java/lang/Object") != 0) exception_name = name;
    return class_handle;
}
static jint JNICALL throw_new(JNIEnv *env, jclass cls, const char *message) {
    (void)env; (void)message; assert(!pending); assert(cls == class_handle); throws++; pending = 1; return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject ref) { (void)env; assert(ref == (jobject)class_handle); deletes++; }
static jobject allocate(jint length) {
    assert(!pending); allocations++; requested = length;
    if (mode == 2) { pending = 7; return NULL; }
    if (mode == 1) return NULL;
    return result_handle;
}
#define ALLOCATOR(type, name) static type JNICALL name(JNIEnv *env, jsize length) { (void)env; return (type)allocate(length); }
ALLOCATOR(jbooleanArray, new_boolean)
ALLOCATOR(jbyteArray, new_byte)
ALLOCATOR(jcharArray, new_char)
ALLOCATOR(jshortArray, new_short)
ALLOCATOR(jintArray, new_int)
ALLOCATOR(jlongArray, new_long)
ALLOCATOR(jfloatArray, new_float)
ALLOCATOR(jdoubleArray, new_double)
static jobjectArray JNICALL new_object(JNIEnv *env, jsize length, jclass cls, jobject initial) {
    (void)env; assert(cls == class_handle); assert(initial == NULL); return (jobjectArray)allocate(length);
}
static const struct JNINativeInterface_ functions = {
    .ExceptionCheck = exception_check, .FindClass = find_class, .ThrowNew = throw_new,
    .DeleteLocalRef = delete_ref, .NewObjectArray = new_object, .NewBooleanArray = new_boolean,
    .NewByteArray = new_byte, .NewCharArray = new_char, .NewShortArray = new_short,
    .NewIntArray = new_int, .NewLongArray = new_long, .NewFloatArray = new_float, .NewDoubleArray = new_double
};
typedef jobject (*entry)(JNIEnv *, jclass, jint);
#define ENTRY(kind) Java_jdk_m3_arrays_M3ArrayNative_nativeAllocate##kind,
static entry const entries[] = { KINDS(ENTRY) };

static void reset(int selected_mode) {
    pending = allocations = finds = throws = deletes = 0; mode = selected_mode;
    requested = -1; exception_name = NULL;
}

int main(void) {
    JNIEnv env = &functions;
    size_t cases = 0;
    for (size_t i = 0; i < sizeof(entries) / sizeof(entries[0]); i++) {
        for (int length = 0; length <= 17; length += 17) {
            reset(0);
            assert(entries[i](&env, NULL, length) == result_handle);
            assert(allocations == 1 && requested == length && !pending && !throws);
            assert(finds == (i == 0 ? 1 : 0)); assert(deletes == finds); cases++;
        }
        reset(0); pending = 9;
        assert(entries[i](&env, NULL, 1) == NULL);
        assert(pending == 9 && allocations == 0 && finds == 0 && throws == 0 && deletes == 0); cases++;
        reset(0);
        assert(entries[i](&env, NULL, -1) == NULL);
        assert(allocations == 0 && pending == 1 && throws == 1 && finds == deletes);
        assert(strcmp(exception_name, "java/lang/NegativeArraySizeException") == 0); cases++;
        reset(1);
        assert(entries[i](&env, NULL, 17) == NULL);
        assert(allocations == 1 && requested == 17 && pending == 1 && throws == 1 && finds == deletes);
        assert(strcmp(exception_name, "java/lang/OutOfMemoryError") == 0); cases++;
        reset(2);
        assert(entries[i](&env, NULL, 17) == NULL);
        assert(allocations == 1 && requested == 17 && pending == 7 && throws == 0);
        assert(finds == (i == 0 ? 1 : 0) && deletes == finds); cases++;
    }
    printf("PASS allocation fault cases=%zu atoms=9\n", cases);
    return 0;
}
