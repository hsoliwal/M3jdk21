/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Deterministic JNI function-table probe, not a JVM substitute. Real JVM tests are separate. */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#ifndef PRODUCT_SOURCE
#error PRODUCT_SOURCE is required
#endif
#include PRODUCT_SOURCE

typedef struct { jsize length; size_t width; void *data; } Array;
static jbyte flags[1100];
static jint narrow[1100], result_int[1100];
static jlong wide[1100], result_long[1100];
static Array a_flags, a_int, a_long, output;
static int pending, mutations, byte_reads, row_reads, row_elements, writes, fault, calls;
static const char *exception_name;
static unsigned long assertions;
static void verify(int pass, const char *label) {
    assertions++;
    if (!pass) { fprintf(stderr,"NATIVE_PROBE_FAIL %s\n",label); exit(1); }
}
static void live(void) { verify(!pending,"no JNI operation under pending exception"); calls++; }
static int region(Array *a, jsize from, jsize count, size_t width) {
    verify(a != NULL && a->width == width,"array kind");
    verify(from >= 0 && count >= 0 && from <= a->length && count <= a->length-from,"region geometry");
    if (fault && calls == fault) { pending=1; exception_name="injected"; return 0; }
    return 1;
}
static jsize JNICALL length_of(JNIEnv *env, jarray ref) {
    (void)env; live(); return ((Array *)(void *)ref)->length;
}
static jboolean JNICALL has_exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env; live(); exception_name=name; return (jclass)(void *)&output;
}
static jint JNICALL throw_new(JNIEnv *env, jclass type, const char *message) {
    (void)env; (void)type; (void)message; live(); pending=1; return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject ref) { (void)env; (void)ref; }
static void mutate(void) {
    if (mutations==1) flags[11+512]=(jbyte)128;
    if (mutations==2) memset(flags+11,0,1024);
}
static jintArray JNICALL new_int(JNIEnv *env, jsize n) {
    (void)env; live(); verify(n>=0 && n<=1100,"int output size");
    if (fault && calls==fault) { pending=1; exception_name="injected"; return NULL; }
    output=(Array){n,sizeof(jint),result_int}; mutate(); return (jintArray)(void *)&output;
}
static jlongArray JNICALL new_long(JNIEnv *env, jsize n) {
    (void)env; live(); verify(n>=0 && n<=1100,"long output size");
    if (fault && calls==fault) { pending=1; exception_name="injected"; return NULL; }
    output=(Array){n,sizeof(jlong),result_long}; mutate(); return (jlongArray)(void *)&output;
}
static void JNICALL get_byte(JNIEnv *env, jbyteArray ref, jsize at, jsize n, jbyte *out) {
    (void)env; live(); Array *a=(Array *)(void *)ref;
    if (!region(a,at,n,sizeof(jbyte))) return;
    byte_reads++; memcpy(out,(jbyte *)a->data+at,(size_t)n*sizeof(jbyte));
}
static void JNICALL get_int(JNIEnv *env, jintArray ref, jsize at, jsize n, jint *out) {
    (void)env; live(); Array *a=(Array *)(void *)ref;
    if (!region(a,at,n,sizeof(jint))) return;
    row_reads++; row_elements+=n; memcpy(out,(jint *)a->data+at,(size_t)n*sizeof(jint));
}
static void JNICALL get_long(JNIEnv *env, jlongArray ref, jsize at, jsize n, jlong *out) {
    (void)env; live(); Array *a=(Array *)(void *)ref;
    if (!region(a,at,n,sizeof(jlong))) return;
    row_reads++; row_elements+=n; memcpy(out,(jlong *)a->data+at,(size_t)n*sizeof(jlong));
}
static void JNICALL set_int(JNIEnv *env, jintArray ref, jsize at, jsize n, const jint *in) {
    (void)env; live(); Array *a=(Array *)(void *)ref;
    if (!region(a,at,n,sizeof(jint))) return;
    writes++; memcpy((jint *)a->data+at,in,(size_t)n*sizeof(jint));
}
static void JNICALL set_long(JNIEnv *env, jlongArray ref, jsize at, jsize n, const jlong *in) {
    (void)env; live(); Array *a=(Array *)(void *)ref;
    if (!region(a,at,n,sizeof(jlong))) return;
    writes++; memcpy((jlong *)a->data+at,in,(size_t)n*sizeof(jlong));
}
static const struct JNINativeInterface_ table={
    .FindClass=find_class,.ThrowNew=throw_new,.DeleteLocalRef=delete_ref,.ExceptionCheck=has_exception,
    .GetArrayLength=length_of,.NewIntArray=new_int,.NewLongArray=new_long,
    .GetByteArrayRegion=get_byte,.GetIntArrayRegion=get_int,.GetLongArrayRegion=get_long,
    .SetIntArrayRegion=set_int,.SetLongArrayRegion=set_long
};
static void reset(int pattern, int mutation, int injected) {
    pending=0; mutations=mutation; fault=injected; calls=0; byte_reads=0;
    row_reads=0; row_elements=0; writes=0; exception_name=NULL;
    memset(flags,0,sizeof(flags));
    for (int i=0;i<1100;i++) { narrow[i]=i%17-8; wide[i]=INT64_MIN+(int64_t)i; }
    for (int i=0;i<1024;i++) flags[11+i]=(jbyte)(pattern==2 || (pattern==1 && i/256==2) ? 128 : 0);
    a_flags=(Array){1100,sizeof(jbyte),flags}; a_int=(Array){1100,sizeof(jint),narrow};
    a_long=(Array){1100,sizeof(jlong),wide};
}
static jarray execute(int long_mode, int mask) {
    JNIEnv env=&table;
    if (long_mode) return (jarray)Java_com_m3_collections_M3IndexMaskCompactor_selectLongWindowNative(
        &env,NULL,(jlongArray)(void *)&a_long,7,(jbyteArray)(void *)&a_flags,11,1024,mask);
    return (jarray)Java_com_m3_collections_M3IndexMaskCompactor_selectWindowNative(
        &env,NULL,(jintArray)(void *)&a_int,7,(jbyteArray)(void *)&a_flags,11,1024,mask);
}
int main(void) {
    for (int width=0;width<2;width++) {
        for (int pattern=0;pattern<4;pattern++) {
            int mask=pattern==3 ? 0 : 128;
            reset(pattern,0,0); jarray result=execute(width,mask);
            verify(result!=NULL && !pending,"successful output");
            int expected=pattern==3 || pattern==2 ? 1024 : pattern==1 ? 256 : 0;
            verify(output.length==expected,"exact output cardinality");
            int out=0;
            for (int i=0;i<1024;i++) if (mask==0 || (unsigned char)flags[11+i]==128u) {
                if (width) verify(result_long[out]==wide[7+i],"long stable order");
                else verify(result_int[out]==narrow[7+i],"int stable order");
                out++;
            }
            verify(byte_reads==(mask==0 ? 0 : 8),"two state scans, zero-mask no scan");
            printf("WORK width=%d pattern=%d row_reads=%d row_elements=%d state_reads=%d writes=%d\n",
                width,pattern,row_reads,row_elements,byte_reads,writes);
            int success_calls=calls;
            /* Inject only at real fallible region/NewArray sites (calls 1,2 are GetArrayLength). */
            for (int at=3;at<=success_calls;at++) {
                reset(pattern,0,at); result=execute(width,mask);
                verify(pending && result==NULL && exception_name!=NULL,"fallible call refuses");
                verify(calls==at,"no operations after pending failure");
            }
        }
        for (int mutation=1;mutation<=2;mutation++) {
            reset(mutation==1 ? 0 : 1,mutation,0); jarray result=execute(width,128);
            verify(result==NULL && pending,"count drift refusal");
            verify(strcmp(exception_name,"java/util/ConcurrentModificationException")==0,"original drift exception");
        }
    }
    printf("NATIVE_PROBE_PASS assertions=%lu\n",assertions);
    return 0;
}
