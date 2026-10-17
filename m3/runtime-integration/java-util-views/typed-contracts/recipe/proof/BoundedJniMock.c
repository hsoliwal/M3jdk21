/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
 * Fault injection around the actual C source, supplementary to actual JVM JNI.
 */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct { jint *data; jsize length; } Array;
static int checks, cases, pending, calls, largest, pins, releases, throw_calls;
static int after_exception, inject_at;
static const char *last_message;
static void require(int value, const char *message) {
    checks++;
    if (!value) { fprintf(stderr,"MOCK_FAILURE %s checks=%d\n",message,checks); exit(1); }
}
static Array *array(jintArray input) { return (Array *)(void *)input; }
static jsize JNICALL length(JNIEnv *env, jarray input) {
    (void)env; if (pending) after_exception++; return ((Array *)(void *)input)->length;
}
static void JNICALL region(JNIEnv *env,jintArray input,jsize start,jsize count,jint *out) {
    (void)env; if(pending) after_exception++;
    calls++; if(count>largest)largest=count;
    require(count>=0 && start>=0 && start<=array(input)->length && count<=array(input)->length-start,"REGION_BOUNDS");
    if(calls==inject_at){pending=1;last_message="injected pending exception";return;}
    memcpy(out,array(input)->data+start,(size_t)count*sizeof(jint));
}
static jboolean JNICALL exception(JNIEnv *env) {(void)env;return pending?JNI_TRUE:JNI_FALSE;}
static jclass JNICALL find(JNIEnv *env,const char *name) {
    (void)env;(void)name;if(pending)after_exception++;return (jclass)(void *)&checks;
}
static jint JNICALL throw_new(JNIEnv *env,jclass type,const char *message) {
    (void)env;(void)type;if(pending)after_exception++;throw_calls++;pending=1;last_message=message;return 0;
}
static void JNICALL delete_ref(JNIEnv *env,jobject ref){(void)env;(void)ref;}
static jint *JNICALL elements(JNIEnv *env,jintArray input,jboolean *copied) {
    (void)env;if(pending)after_exception++;pins++;if(copied!=NULL)*copied=JNI_FALSE;return array(input)->data;
}
static void JNICALL release(JNIEnv *env,jintArray input,jint *data,jint mode) {
    (void)env;(void)input;(void)data;require(mode==JNI_ABORT,"READ_ONLY_RELEASE");releases++;
}
static const struct JNINativeInterface_ functions={
    .GetArrayLength=length,.GetIntArrayRegion=region,.ExceptionCheck=exception,.FindClass=find,
    .ThrowNew=throw_new,.DeleteLocalRef=delete_ref,.GetIntArrayElements=elements,.ReleaseIntArrayElements=release
};
#ifndef NATIVE_SOURCE
#error NATIVE_SOURCE must name the exact source under test
#endif
#include NATIVE_SOURCE
static void reset(int fault) {
    pending=calls=largest=pins=releases=throw_calls=after_exception=0;inject_at=fault;last_message=NULL;cases++;
}
static jint invoke(Array *input,int runs,int query) {
    JNIEnv env=&functions;
    return Java_RunSearchJni_first(&env,NULL,(jintArray)(void *)input,runs,query);
}
int main(void) {
    const int sizes[]={0,1,2,511,512,513,1024,1025,32768};
    const int queries[]={-2147483647-1,-1,0,1,1023,32768,65535,2147483647};
    for(size_t s=0;s<sizeof sizes/sizeof sizes[0];s++) {
        int n=sizes[s];Array input={calloc((size_t)(2*n+4),sizeof(jint)),2*n+4};
        require(input.data!=NULL,"FIXTURE_ALLOCATION");
        for(int i=0;i<n;i++){input.data[2*i]=2*i;input.data[2*i+1]=2*i+1;}
        input.data[2*n]=-999;input.data[2*n+1]=2147483647;
        for(size_t q=0;q<sizeof queries/sizeof queries[0];q++) {
            int query=queries[q],want=0;while(want<n&&input.data[2*want+1]<=query)want++;
            reset(0);int result=invoke(&input,n,query);
            require(!pending&&result==want,"EXACT_UPPER_BOUND");
            require(pins==0&&releases==0,"PINNED_OR_UNBOUNDED");
            require(calls==(n+511)/512&&largest<=1024,"BOUNDED_FULL_VALIDATION");
            require(after_exception==0,"POST_EXCEPTION_JNI");
        }
        if(n>0) {
            for(int fault=1;fault<=(n+511)/512;fault++) {
                reset(fault);int result=invoke(&input,n,-1);
                require(result==-1&&pending&&calls==fault,"PENDING_EXCEPTION_STOP");
                require(throw_calls==0&&after_exception==0,"PENDING_EXCEPTION_PRESERVED");
                require(strcmp(last_message,"injected pending exception")==0,"PENDING_EXCEPTION_IDENTITY");
            }
            input.data[2*n-1]=65537;
            reset(0);(void)invoke(&input,n,-1);
            require(pending&&throw_calls==1,"LATE_INVALID_MUST_REJECT");
            input.data[2*n-1]=2*(n-1)+1;
        }
        if(n>512) {
            int old=input.data[1024];input.data[1024]=input.data[1023];
            reset(0);(void)invoke(&input,n,2147483647);
            require(pending&&throw_calls==1,"CROSS_REGION_ORDER");input.data[1024]=old;
        }
        free(input.data);
    }
    reset(0);(void)invoke(NULL,0,0);require(pending&&calls==0,"NULL_REFUSAL");
    Array empty={NULL,0};reset(0);(void)invoke(&empty,-1,0);require(pending&&calls==0,"NEGATIVE_COUNT");
    reset(0);(void)invoke(&empty,1,0);require(pending&&calls==0,"COUNT_REFUSAL");
    printf("MOCK_PASS cases=%d checks=%d max_region_ints=1024 retained_pins=0\n",cases,checks);
    return 0;
}
