/* SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls != NULL) { (void)(*env)->ThrowNew(env, cls, message); (*env)->DeleteLocalRef(env, cls); }
}
JNIEXPORT jboolean JNICALL Java_NativeBoundaryTest_utf16be(JNIEnv *env, jclass ignored, jobject data, jcharArray expected) {
    (void)ignored;
    if (data == NULL || expected == NULL) { fail(env,"java/lang/NullPointerException","null buffer/array"); return JNI_FALSE; }
    jlong bytes = (*env)->GetDirectBufferCapacity(env,data);
    if (bytes < 0 || (bytes & 1) != 0) { fail(env,"java/lang/IllegalArgumentException","requires even direct byte capacity"); return JNI_FALSE; }
    jsize count = (*env)->GetArrayLength(env,expected);
    if (bytes != (jlong)count * 2) { fail(env,"java/lang/IllegalArgumentException","UTF-16 capacity differs"); return JNI_FALSE; }
    if (count == 0) return JNI_TRUE;
    const uint8_t *raw = (const uint8_t *)(*env)->GetDirectBufferAddress(env,data);
    if (raw == NULL) { fail(env,"java/lang/IllegalArgumentException","no direct address"); return JNI_FALSE; }
    jchar *chars = (*env)->GetCharArrayElements(env,expected,NULL);
    if (chars == NULL) return JNI_FALSE;
    jboolean equal = JNI_TRUE;
    for (jsize i=0;i<count;i++) {
        size_t at=(size_t)i*2u;
        jchar unit=(jchar)(((uint16_t)raw[at] << 8u) | (uint16_t)raw[at+1u]);
        if (unit != chars[i]) { equal=JNI_FALSE;break; }
    }
    (*env)->ReleaseCharArrayElements(env,expected,chars,JNI_ABORT);
    return equal;
}
JNIEXPORT jstring JNICALL Java_NativeBoundaryTest_modifiedUtf8RoundTrip(JNIEnv *env, jclass ignored, jstring text) {
    (void)ignored;
    if (text==NULL) { fail(env,"java/lang/NullPointerException","null text"); return NULL; }
    jsize length=(*env)->GetStringUTFLength(env,text);
    if (length<0 || (uint64_t)(uint32_t)length+1u>SIZE_MAX) {
        fail(env,"java/lang/OutOfMemoryError","modified UTF-8 length overflow");return NULL;
    }
    char *copy=(char *)malloc((size_t)length+1u);
    if (copy==NULL) { fail(env,"java/lang/OutOfMemoryError","allocation");return NULL; }
    const char *utf=(*env)->GetStringUTFChars(env,text,NULL);
    if (utf==NULL) { free(copy);return NULL; }
    memcpy(copy,utf,(size_t)length);copy[length]='\0';
    (*env)->ReleaseStringUTFChars(env,text,utf);
    jstring result=(*env)->NewStringUTF(env,copy);
    free(copy);
    return result;
}
JNIEXPORT jstring JNICALL Java_NativeBoundaryTest_criticalCopy(JNIEnv *env, jclass ignored, jstring text) {
    (void)ignored;
    if (text==NULL) { fail(env,"java/lang/NullPointerException","null text"); return NULL; }
    jsize length=(*env)->GetStringLength(env,text);
    if ((size_t)length > SIZE_MAX/sizeof(jchar)) { fail(env,"java/lang/OutOfMemoryError","length overflow");return NULL; }
    size_t bytes=(size_t)length*sizeof(jchar);
    jchar *copy=(jchar *)malloc(bytes==0?sizeof(jchar):bytes);
    if (copy==NULL) { fail(env,"java/lang/OutOfMemoryError","allocation");return NULL; }
    const jchar *units=(*env)->GetStringCritical(env,text,NULL);
    if (units==NULL) { free(copy);return NULL; }
    /* No allocation, blocking, or other JNI calls until release. */
    if (bytes>0)memcpy(copy,units,bytes);
    (*env)->ReleaseStringCritical(env,text,units);
    jstring result=(*env)->NewString(env,copy,length);
    free(copy);
    return result;
}
