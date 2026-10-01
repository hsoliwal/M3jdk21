/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
JNIEXPORT jstring JNICALL Java_StringApiProbe_roundTrip(JNIEnv *env,jclass cls,jstring s){
 jsize n=(*env)->GetStringLength(env,s);const jchar *chars=(*env)->GetStringChars(env,s,NULL);if(chars==NULL)return NULL;
 jstring result=(*env)->NewString(env,chars,n);(*env)->ReleaseStringChars(env,s,chars);
 const char *utf=(*env)->GetStringUTFChars(env,s,NULL);if(utf==NULL)return NULL;
 jstring utfCopy=(*env)->NewStringUTF(env,utf);(*env)->ReleaseStringUTFChars(env,s,utf);
 if(utfCopy==NULL)return NULL;(*env)->DeleteLocalRef(env,utfCopy);return result;
}
