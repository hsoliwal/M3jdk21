/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Instrument the actual JNI owner to measure primitive row traffic, not elapsed-time claims. */
#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifndef EXPECT_SPARSE
#define EXPECT_SPARSE 1
#endif
typedef struct { jsize length; void *data; } Array;
static unsigned long checks, row_reads, row_units;
static int pending, fail_read, fail_alloc;
static Array output;
static union { jlong longs[2048]; jint ints[2048]; } storage;
#define CHECK(x) do { checks++; if (!(x)) { fprintf(stderr,"failure %d: %s\n",__LINE__,#x); exit(1); } } while (0)
static Array *a(jarray value) { return (Array *)(void *) value; }
static void range(jarray value, jsize offset, jsize length) {
    CHECK(!pending); CHECK(offset >= 0 && length >= 0 && offset <= a(value)->length - length);
}
static jsize JNICALL size(JNIEnv *env, jarray value) { (void)env; CHECK(!pending); return a(value)->length; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find(JNIEnv *env, const char *name) { (void)env; (void)name; return (jclass)(uintptr_t)1; }
static jint JNICALL raise(JNIEnv *env, jclass type, const char *message) { (void)env; (void)type; (void)message; pending=1; return 0; }
static void JNICALL release(JNIEnv *env, jobject object) { (void)env; (void)object; }
static void JNICALL bytes(JNIEnv *env, jbyteArray value, jsize offset, jsize length, jbyte *out) {
    (void)env; range(value,offset,length); memcpy(out,(jbyte *)a(value)->data+offset,(size_t)length);
}
static void JNICALL ints(JNIEnv *env, jintArray value, jsize offset, jsize length, jint *out) {
    (void)env; range(value,offset,length); row_reads++; row_units+=(unsigned long)length;
    if (fail_read) { pending=1; return; }
    memcpy(out,(jint *)a(value)->data+offset,(size_t)length*sizeof(jint));
}
static void JNICALL longs(JNIEnv *env, jlongArray value, jsize offset, jsize length, jlong *out) {
    (void)env; range(value,offset,length); row_reads++; row_units+=(unsigned long)length;
    if (fail_read) { pending=1; return; }
    memcpy(out,(jlong *)a(value)->data+offset,(size_t)length*sizeof(jlong));
}
static jintArray JNICALL new_ints(JNIEnv *env, jsize length) {
    (void)env; CHECK(!pending && length >= 0 && length <= 2048);
    if (fail_alloc) { pending=1; return NULL; }
    output.length=length; output.data=storage.ints; return (jintArray)(void *)&output;
}
static jlongArray JNICALL new_longs(JNIEnv *env, jsize length) { jlongArray result=(jlongArray)new_ints(env,length); output.data=storage.longs; return result; }
static void JNICALL put_ints(JNIEnv *env, jintArray value, jsize offset, jsize length, const jint *data) {
    (void)env; range(value,offset,length); memcpy((jint *)a(value)->data+offset,data,(size_t)length*sizeof(jint));
}
static void JNICALL put_longs(JNIEnv *env, jlongArray value, jsize offset, jsize length, const jlong *data) {
    (void)env; range(value,offset,length); memcpy((jlong *)a(value)->data+offset,data,(size_t)length*sizeof(jlong));
}
static const struct JNINativeInterface_ table={
    .GetArrayLength=size,.ExceptionCheck=exception,.FindClass=find,.ThrowNew=raise,.DeleteLocalRef=release,
    .GetByteArrayRegion=bytes,.GetIntArrayRegion=ints,.GetLongArrayRegion=longs,
    .NewIntArray=new_ints,.NewLongArray=new_longs,.SetIntArrayRegion=put_ints,.SetLongArrayRegion=put_longs
};
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectNative(JNIEnv *,jclass,jintArray,jbyteArray,jint);
extern jintArray Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(JNIEnv *,jclass,jintArray,jint,jbyteArray,jint,jint,jint);
extern jlongArray Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(JNIEnv *,jclass,jlongArray,jint,jbyteArray,jint,jint,jint);
static void reset(void) { pending=fail_read=fail_alloc=0;row_reads=row_units=0;memset(&storage,0,sizeof(storage)); }
int main(void) {
    JNIEnv env=&table; jint ids[1025]; jlong values[1025]; jbyte marks[1025];
    Array rows={1025,ids}, long_rows={1025,values}, states={1025,marks};
    for(int i=0;i<1025;i++){ids[i]=i-513;values[i]=INT64_MIN+(jlong)i;}
    for(int selected=-1;selected<1025;selected++) {
        memset(marks,0,sizeof(marks)); if(selected>=0)marks[selected]=(jbyte)0x80;
        for(int mode=0;mode<3;mode++) {
            reset(); jobject result;
            if(mode==0) result=Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env,NULL,(jintArray)(void *)&rows,(jbyteArray)(void *)&states,128);
            else if(mode==1) result=Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env,NULL,(jintArray)(void *)&rows,0,(jbyteArray)(void *)&states,0,1025,128);
            else result=Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(&env,NULL,(jlongArray)(void *)&long_rows,0,(jbyteArray)(void *)&states,0,1025,128);
            CHECK(result && !pending); CHECK(output.length==(selected<0?0:1));
            CHECK(row_units==(EXPECT_SPARSE ? (selected<0?0UL:1UL) : 1025UL));
            CHECK(row_reads==(EXPECT_SPARSE ? (selected<0?0UL:1UL) : 5UL));
            if(selected>=0) CHECK(mode==2 ? storage.longs[0]==values[selected] : storage.ints[0]==ids[selected]);
        }
    }
    memset(marks,0,sizeof(marks)); reset();
    CHECK(Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(&env,NULL,(jintArray)(void *)&rows,7,(jbyteArray)(void *)&states,11,513,0));
    CHECK(row_units==513 && row_reads==3 && output.length==513);
    for(int i=0;i<513;i++)CHECK(storage.ints[i]==ids[i+7]);
    reset(); fail_alloc=1;
    CHECK(!Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env,NULL,(jintArray)(void *)&rows,(jbyteArray)(void *)&states,0));
    CHECK(pending && row_reads==0);
    reset(); fail_read=1;
    CHECK(!Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env,NULL,(jintArray)(void *)&rows,(jbyteArray)(void *)&states,0));
    CHECK(pending && row_reads==1);
    printf("M3_SPARSE_MASK_READS_PASS checks=%lu sparse=%d single_match_row_units=%d\n",checks,EXPECT_SPARSE,EXPECT_SPARSE?1:1025);
    return 0;
}
