/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Real native owner with an instrumented JNI table: effects, failures and pin lifetime. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef EXPECT_TWO_SPANS
#define EXPECT_TWO_SPANS 0
#endif
#define CHECK(c) do { checks++; if (!(c)) { fprintf(stderr, "failed line %d: %s\n", __LINE__, #c); exit(1); } } while (0)
typedef struct { jsize length; void *data; } Array;
static int pending, pinned, regions, pins, releases, writes, fail_region, fail_pin;
static unsigned long checks;
static const char *error_kind;
static Array *array(jarray a) { return (Array *)(void *) a; }
static void available(void) { CHECK(pinned == 0); }
static jsize JNICALL get_length(JNIEnv *e, jarray a) { (void)e; available(); return array(a)->length; }
static jboolean JNICALL exception(JNIEnv *e) { (void)e; available(); return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find(JNIEnv *e, const char *name) { (void)e; available(); error_kind = name; return (jclass)(uintptr_t)1; }
static jint JNICALL throw_new(JNIEnv *e, jclass c, const char *message) { (void)e; (void)c; (void)message; available(); pending = 1; return 0; }
static void range(jarray a, jsize start, jsize length) { available(); CHECK(start >= 0 && length >= 0 && start <= array(a)->length - length); }
static void JNICALL get_int(JNIEnv *e, jintArray a, jsize start, jsize length, jint *out) {
    (void)e; range(a, start, length); memcpy(out, (jint *)array(a)->data + start, (size_t)length * sizeof(jint));
}
static void JNICALL get_long(JNIEnv *e, jlongArray a, jsize start, jsize length, jlong *out) {
    (void)e; range(a, start, length); regions++;
    if (regions == fail_region) { pending = 1; return; }
    memcpy(out, (jlong *)array(a)->data + start, (size_t)length * sizeof(jlong));
}
static void JNICALL set_long(JNIEnv *e, jlongArray a, jsize start, jsize length, const jlong *in) {
    (void)e; range(a, start, length); CHECK(!pending); writes++;
    memcpy((jlong *)array(a)->data + start, in, (size_t)length * sizeof(jlong));
}
static void * JNICALL critical(JNIEnv *e, jarray a, jboolean *copy) {
    (void)e; (void)copy; available(); pins++;
    if (fail_pin) { pending = 1; return NULL; }
    pinned = 1; return array(a)->data;
}
static void JNICALL release(JNIEnv *e, jarray a, void *data, jint mode) {
    (void)e; CHECK(pinned == 1); CHECK(data == array(a)->data); CHECK(mode == JNI_ABORT); pinned = 0; releases++;
}
static const struct JNINativeInterface_ table = {
    .FindClass=find, .ThrowNew=throw_new, .ExceptionCheck=exception, .GetArrayLength=get_length,
    .GetIntArrayRegion=get_int, .GetLongArrayRegion=get_long, .SetLongArrayRegion=set_long,
    .GetPrimitiveArrayCritical=critical, .ReleasePrimitiveArrayCritical=release
};
extern void Java_com_m3_collections_M3LongAddressKernel_gatherNative(
    JNIEnv *, jclass, jlongArray, jintArray, jint, jint, jlongArray, jint);
static void invoke(Array *payload, Array *ids, int start, int length, Array *output, int to) {
    JNIEnv env = &table;
    Java_com_m3_collections_M3LongAddressKernel_gatherNative(&env, NULL,
        (jlongArray)(void *)payload, (jintArray)(void *)ids, start, length,
        (jlongArray)(void *)output, to);
    CHECK(pinned == 0);
}
static void reset(void) { pending=pinned=regions=pins=releases=writes=fail_region=fail_pin=0; error_kind=NULL; }

int main(void) {
    jlong payload[2048], output[600]; jint ids[600];
    Array p={2048,payload}, i={600,ids}, o={600,output};
    for (int k=0;k<2048;k++) payload[k]=INT64_MIN + ((jlong)k * INT64_C(123457));
    for (int seam=1;seam<256;seam++) {
        for (int k=0;k<256;k++) ids[3+k]=k<seam ? 10+k : 1000+k-seam;
        reset(); for (int k=0;k<600;k++) output[k]=71;
        invoke(&p,&i,3,256,&o,5); CHECK(!pending); CHECK(writes==1);
        CHECK(EXPECT_TWO_SPANS ? (regions==2 && pins==0 && releases==0) : (regions==0 && pins==1 && releases==1));
        for (int k=0;k<600;k++) CHECK(output[k] == (k>=5 && k<261 ? payload[ids[k-2]] : 71));
    }
    /* Three or more spans retain the bounded critical-pointer fallback. */
    for (int k=0;k<513;k++) ids[3+k]=(k*31)%2048;
    reset(); invoke(&p,&i,3,513,&o,5); CHECK(!pending); CHECK(pins==2 && releases==2 && regions==1 && writes==3);
    /* Invalid physical addresses publish exactly the valid prefix, across chunk seams. */
    const jint bad[]={-1,2048,INT32_MIN,INT32_MAX};
    for (int length=1;length<=513;length+=32) {
        for (int at=0;at<length;at++) {
            for (unsigned b=0;b<sizeof(bad)/sizeof(bad[0]);b++) {
                for (int k=0;k<length;k++) ids[3+k]=k%256 < 128 ? k : 1200+k;
                ids[3+at]=bad[b]; reset(); for(int k=0;k<600;k++) output[k]=71;
                invoke(&p,&i,3,length,&o,5); CHECK(pending); CHECK(strcmp(error_kind,"java/lang/IndexOutOfBoundsException")==0);
                for(int k=0;k<600;k++) CHECK(output[k] == (k>=5 && k<5+at ? payload[ids[k-2]] : 71));
            }
        }
    }
    /* A failed second region must not publish a partially prepared chunk. */
    for (int k=0;k<256;k++) ids[3+k]=k<128 ? k : 1000+k;
    reset(); fail_region=2; for(int k=0;k<600;k++) output[k]=71;
    invoke(&p,&i,3,256,&o,5);
    if (EXPECT_TWO_SPANS) { CHECK(pending && writes==0); for(int k=0;k<600;k++) CHECK(output[k]==71); }
    for (int k=0;k<256;k++) ids[3+k]=(k*31)%2048;
    reset(); fail_pin=1; for(int k=0;k<600;k++) output[k]=71;
    invoke(&p,&i,3,256,&o,5); CHECK(pending && pins==1 && releases==0 && writes==0);
    for(int k=0;k<600;k++) CHECK(output[k]==71);
    reset(); invoke(&p,&i,-1,1,&o,0); CHECK(pending && writes==0 && pins==0 && regions==0);
    reset(); invoke(&p,&i,0,0,&o,0); CHECK(!pending && writes==0 && pins==0 && regions==0);
    printf("M3_GATHER_SPANS_PASS checks=%lu two_spans=%d scratch_bytes=3072\n",checks,EXPECT_TWO_SPANS);
    return 0;
}
