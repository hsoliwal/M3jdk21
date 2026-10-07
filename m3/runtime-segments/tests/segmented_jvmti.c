/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
#include <jni.h>
#include <jvmti.h>
#include <stdlib.h>
#include <string.h>
static jvmtiEnv *ti;
struct Expected { jchar *chars; jint length; int matches; int bad; };
static jint JNICALL string_value(jlong class_tag, jlong size, jlong *tag,
                                 const jchar *value, jint length, void *data) {
    (void)class_tag; (void)size;
    struct Expected *wanted=data;
    if (*tag==1234567) {
        wanted->matches++;
        if(length!=wanted->length || memcmp(value,wanted->chars,(size_t)length*sizeof(jchar))!=0) wanted->bad=1;
    }
    return 0;
}
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm,char *options,void *reserved) {
    (void)options;(void)reserved;
    if((*vm)->GetEnv(vm,(void**)&ti,JVMTI_VERSION_1_2)!=JNI_OK)return JNI_ERR;
    jvmtiCapabilities caps;memset(&caps,0,sizeof(caps));caps.can_tag_objects=1;
    return (*ti)->AddCapabilities(ti,&caps)==JVMTI_ERROR_NONE?JNI_OK:JNI_ERR;
}
JNIEXPORT jint JNICALL Java_SegmentedJvmti_walk(JNIEnv *env,jclass type,jstring target) {
    (void)type;
    struct Expected wanted={0};wanted.length=(*env)->GetStringLength(env,target);
    if((*env)->ExceptionCheck(env))return -1;
    wanted.chars=malloc(((size_t)wanted.length+1)*sizeof(jchar));if(!wanted.chars)return -2;
    (*env)->GetStringRegion(env,target,0,wanted.length,wanted.chars);
    if((*env)->ExceptionCheck(env)){free(wanted.chars);return -3;}
    if((*ti)->SetTag(ti,target,1234567)!=JVMTI_ERROR_NONE){free(wanted.chars);return -4;}
    jvmtiHeapCallbacks callbacks;memset(&callbacks,0,sizeof(callbacks));callbacks.string_primitive_value_callback=string_value;
    jvmtiError error=(*ti)->IterateThroughHeap(ti,JVMTI_HEAP_FILTER_UNTAGGED,NULL,&callbacks,&wanted);
    jvmtiError cleared=(*ti)->SetTag(ti,target,0);
    free(wanted.chars);
    return error==JVMTI_ERROR_NONE&&cleared==JVMTI_ERROR_NONE&&!wanted.bad?wanted.matches:-5;
}
