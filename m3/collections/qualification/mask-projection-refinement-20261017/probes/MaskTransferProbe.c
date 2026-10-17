/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* Test-only JNI adapter against the exact native source; never linked into product code. */
#include <jni.h>
#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int row_marker, state_marker, class_marker;
static jsize length, state_length, output_length;
static int shape, pending, kind, fail_state, fail_rows, fail_write, fail_new, drift;
static int state_calls, row_calls, write_calls, throws, new_calls;
static long row_items, copied_items;
static jint *output;
static int scenarios;
static jboolean JNICALL check_exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jsize JNICALL array_length(JNIEnv *env, jarray array) {
    (void)env; assert(!pending);
    return array == (jarray)&row_marker ? length : state_length;
}
static int mark(jsize i) {
    return shape == 0 ? 0 : shape == 1 ? 128 : shape == 2 ? (i % 17 == 0 ? 128 : 0)
            : (i / 256 % 4 == 0 ? 128 : 0);
}
static jint row_value(jsize i) { return i % 7 == 0 ? INT32_MIN : i % 7 == 1 ? INT32_MAX : i % 11 - 5; }
static void JNICALL get_states(JNIEnv *env, jbyteArray array, jsize first, jsize count, jbyte *dst) {
    (void)env; assert(!pending); assert(array == (jbyteArray)&state_marker);
    assert(first >= 0 && count > 0 && count <= 256 && first <= length - count);
    state_calls++;
    if (state_calls == fail_state) { pending = 1; kind = 10; return; }
    int chunks = (length + 255) / 256;
    for (jsize i = 0; i < count; i++) {
        int m = mark(first+i);
        if (state_calls > chunks && drift) m = drift > 0 ? 128 : 0;
        dst[i] = (jbyte)m;
    }
}
static void JNICALL get_rows(JNIEnv *env, jintArray array, jsize first, jsize count, jint *dst) {
    (void)env; assert(!pending); assert(array == (jintArray)&row_marker);
    assert(first >= 0 && count > 0 && count <= 256 && first <= length - count);
    row_calls++; row_items += count;
    if (row_calls == fail_rows) { pending = 1; kind = 11; return; }
    for (jsize i = 0; i < count; i++) dst[i] = row_value(first+i);
}
static jintArray JNICALL new_output(JNIEnv *env, jsize n) {
    (void)env;assert(!pending);new_calls++;
    if (fail_new) { pending = 1; kind = 12; return NULL; }
    output_length = n; output = calloc((size_t)n+1,sizeof(jint)); assert(output != NULL);
    return (jintArray)output;
}
static void JNICALL set_output(JNIEnv *env, jintArray array, jsize first, jsize count, const jint *src) {
    (void)env;assert(!pending);assert(array == (jintArray)output);
    assert(first >= 0 && count > 0 && count <= 256 && first <= output_length-count);
    write_calls++;
    if (write_calls == fail_write) { pending = 1; kind = 13; return; }
    copied_items += count;memcpy(output+first,src,(size_t)count*sizeof(jint));
}
static jclass JNICALL find_class(JNIEnv *env, const char *name) {
    (void)env;assert(!pending);
    kind = strcmp(name,"java/util/ConcurrentModificationException")==0 ? 1 : 2;
    return (jclass)&class_marker;
}
static jint JNICALL throw_new(JNIEnv *env, jclass cls, const char *message) {
    (void)env;(void)cls;(void)message;assert(!pending);pending = 1;throws++;return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject obj) { (void)env;(void)obj; }
static const struct JNINativeInterface_ functions = {
    .ExceptionCheck=check_exception,.GetArrayLength=array_length,.GetByteArrayRegion=get_states,
    .GetIntArrayRegion=get_rows,.NewIntArray=new_output,.SetIntArrayRegion=set_output,
    .FindClass=find_class,.ThrowNew=throw_new,.DeleteLocalRef=delete_ref
};
#ifndef M3_SOURCE
#error M3_SOURCE must reference the exact qualified native source
#endif
#include M3_SOURCE

static void reset(jsize n, int pattern) {
    free(output);output=NULL;length=n;state_length=n;shape=pattern;output_length=0;
    pending=kind=fail_state=fail_rows=fail_write=fail_new=drift=0;
    state_calls=row_calls=write_calls=throws=new_calls=0;row_items=copied_items=0;
}
static jintArray invoke(jint required, int null_rows) {
    JNIEnv env = &functions;scenarios++;
    return Java_com_m3_collections_M3IndexMaskCompactor_selectNative(&env,NULL,
            null_rows ? NULL : (jintArray)&row_marker,(jbyteArray)&state_marker,required);
}
int main(void) {
    const jsize sizes[] = {0,1,63,64,65,255,256,257,511,512,513,1025,4097};
    for (size_t n=0;n<sizeof(sizes)/sizeof(sizes[0]);n++) for (int pattern=0;pattern<4;pattern++) {
        reset(sizes[n],pattern);assert(invoke(128,0) != NULL && !pending);
        jsize accepted=0;
        for (jsize i=0;i<length;i++) if (mark(i)) { assert(accepted<output_length);assert(output[accepted++]==row_value(i)); }
        assert(accepted==output_length && copied_items==accepted);
        assert(state_calls == 2*((length+255)/256));
#ifdef REQUIRE_MASK_FIRST
        long expected_items=0;
        for (jsize first=0;first<length;first+=256) {
            jsize count=length-first<256 ? length-first : 256;int matched=0;
            for(jsize i=0;i<count;i++) matched |= mark(first+i);
            if (matched) expected_items+=count;
        }
        assert(row_items==expected_items);
#endif
    }
    for (int fail=1;fail<=6;fail++) {
        reset(513,1);fail_state=fail;assert(invoke(128,0)==NULL && pending && kind==10 && state_calls==fail);
    }
    for (int fail=1;fail<=3;fail++) {
        reset(513,1);fail_rows=fail;assert(invoke(128,0)==NULL && pending && kind==11 && row_calls==fail);
        reset(513,1);fail_write=fail;assert(invoke(128,0)==NULL && pending && kind==13 && write_calls==fail);
    }
    reset(513,1);fail_new=1;assert(invoke(128,0)==NULL && pending && kind==12 && row_calls==0);
    reset(513,0);drift=1;assert(invoke(128,0)==NULL && pending && kind==1 && write_calls==0);
    reset(513,1);drift=-1;assert(invoke(128,0)==NULL && pending && kind==1 && write_calls==0);
    reset(1,1);assert(invoke(-1,0)==NULL && kind==2 && new_calls==0);
    reset(1,1);assert(invoke(256,0)==NULL && kind==2 && new_calls==0);
    reset(1,1);assert(invoke(128,1)==NULL && kind==2 && new_calls==0);
    reset(1,1);state_length=2;assert(invoke(128,0)==NULL && kind==2 && new_calls==0);
    printf("JNI_MOCK_SCENARIOS=%d\n",scenarios);
    puts("shape\tinput_items\tstate_region_calls\trow_region_calls\trow_items\toutput_items");
    for (int pattern=0;pattern<4;pattern++) {
        reset(100000,pattern);assert(invoke(128,0)!=NULL && !pending);
        printf("%d\t%d\t%d\t%d\t%ld\t%ld\n",pattern,length,state_calls,row_calls,row_items,copied_items);
    }
    reset(0,0);return 0;
}
