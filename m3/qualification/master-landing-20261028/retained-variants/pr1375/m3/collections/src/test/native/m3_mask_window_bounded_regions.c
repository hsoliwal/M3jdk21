/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* Adapted from filtered-bounds-sparse-20261017/proof/native_regions.c at
 * Synexia 140c263a90f9803b9c46103c63355a286e0515f0, blob
 * 4ee28ebec3dc04a253a5528004ea423ebf3e652e.
 * Instrumented JNI table around the actual production function; not a JVM substitute claim.
 * Preserve the legacy cohort and add the current bounded export to the same region proof.
 */
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#ifndef M3_WINDOW_SOURCE
#error M3_WINDOW_SOURCE must name the exact production C source
#endif
#include M3_WINDOW_SOURCE

typedef struct { jint length; void *data; int type; } Array;
static long checks;
static int pending, region_calls, fail_at, byte_calls, int_calls, write_calls;
static long byte_lanes, int_lanes, write_lanes;
static jint maximum;
static void require(int ok, const char *label) {
    checks++;
    if (!ok) { fprintf(stderr, "NATIVE_REGION_ASSERT:%s\n", label); exit(1); }
}
static Array *array(jarray a) { return (Array *) (void *) a; }
static jsize JNICALL length(JNIEnv *env, jarray a) { (void)env; return array(a)->length; }
static jboolean JNICALL same(JNIEnv *env, jobject a, jobject b) { (void)env; return a == b ? JNI_TRUE : JNI_FALSE; }
static jboolean JNICALL exception(JNIEnv *env) { (void)env; return pending ? JNI_TRUE : JNI_FALSE; }
static jclass JNICALL find(JNIEnv *env, const char *name) { (void)env; (void)name; static int cls; return (jclass)(void *)&cls; }
static jint JNICALL throw_new(JNIEnv *env, jclass cls, const char *message) {
    (void)env; (void)cls; (void)message; pending = 1; return 0;
}
static void JNICALL delete_ref(JNIEnv *env, jobject obj) { (void)env; (void)obj; }
static int region(Array *a, jint start, jint size, int type) {
    require(!pending, "REGION_AFTER_PENDING_EXCEPTION");
    require(start >= 0 && size >= 0 && start <= a->length && size <= a->length - start && a->type == type, "JNI_GEOMETRY");
    require(size <= 128, "NATIVE_SCRATCH_CHUNK_BOUND");
    if (size > maximum) maximum = size;
    region_calls++;
    if (fail_at != 0 && region_calls == fail_at) { pending = 1; return 0; }
    return 1;
}
static void JNICALL get_bytes(JNIEnv *env, jbyteArray a, jsize start, jsize size, jbyte *out) {
    (void)env; byte_calls++; byte_lanes += size;
    if (region(array(a), start, size, 1)) memcpy(out, ((jbyte*)array(a)->data)+start, (size_t)size);
}
static void JNICALL get_ints(JNIEnv *env, jintArray a, jsize start, jsize size, jint *out) {
    (void)env; int_calls++; int_lanes += size;
    if (region(array(a), start, size, 2)) memcpy(out, ((jint*)array(a)->data)+start, (size_t)size*sizeof(jint));
}
static void JNICALL put_ints(JNIEnv *env, jintArray a, jsize start, jsize size, const jint *in) {
    (void)env; write_calls++; write_lanes += size;
    if (region(array(a), start, size, 2)) memcpy(((jint*)array(a)->data)+start, in, (size_t)size*sizeof(jint));
}
static const struct JNINativeInterface_ table = {
    .GetArrayLength=length, .IsSameObject=same, .ExceptionCheck=exception, .FindClass=find,
    .ThrowNew=throw_new, .DeleteLocalRef=delete_ref, .GetByteArrayRegion=get_bytes,
    .GetIntArrayRegion=get_ints, .SetIntArrayRegion=put_ints
};
static void reset(int fault) {
    pending=0;region_calls=0;fail_at=fault;byte_calls=0;int_calls=0;write_calls=0;
    byte_lanes=0;int_lanes=0;write_lanes=0;maximum=0;
}
static jlong call(Array *r, Array *s, int first, int required, int excluded, Array *o, int off, int cap) {
    JNIEnv env=&table;
    return Java_com_m3_collections_M3IndexMaskWindow_scanNative(&env,NULL,(jintArray)(void*)r,
        (jbyteArray)(void*)s,first,required,excluded,(jintArray)(void*)o,off,cap);
}
static jlong call_bounded(Array *r, Array *s, int first, int end, int required,
        int excluded, Array *o, int off, int cap) {
    JNIEnv env=&table;
    return Java_com_m3_collections_M3IndexMaskWindow_scanBoundedNative(&env,NULL,
        (jintArray)(void*)r,(jbyteArray)(void*)s,first,end,required,excluded,
        (jintArray)(void*)o,off,cap);
}

int main(int argc,char **argv) {
    int enforce=argc==1 || strcmp(argv[1],"observe") != 0;
    static jint rows[8193],output[4100]; static jbyte states[8193];
    for(int i=0;i<8193;i++){rows[i]=(i%19)-9;states[i]=(jbyte)i;}
    Array r={8193,rows,2},s={8193,states,1},o={4100,output,2};
    for(int i=0;i<4100;i++)output[i]=73;
    reset(0);jlong result=call(&r,&s,31,128,128,&o,2,4096);
    require(result==m3_window_result(8193,0)&&!pending,"CONTRADICTION_CURSOR");
    for(int i=0;i<4100;i++)require(output[i]==73,"CONTRADICTION_OUTPUT");
    printf("REGIONS contradiction byte_lanes=%ld int_lanes=%ld writes=%ld\n",byte_lanes,int_lanes,write_lanes);
    if(enforce)require(region_calls==0,"CONTRADICTORY_MASK_MUST_NOT_SCAN");
    reset(0);result=call(&r,&s,31,0,0,&o,2,4096);
    require(result==m3_window_result(4127,4096)&&!pending,"IDENTITY_CURSOR");
    require(output[0]==73&&output[1]==73&&output[4098]==73&&output[4099]==73,"IDENTITY_SENTINELS");
    for(int i=0;i<4096;i++)require(output[2+i]==rows[31+i],"IDENTITY_OFFSET_COPY");
    printf("REGIONS identity byte_lanes=%ld int_lanes=%ld writes=%ld max_chunk=%d\n",byte_lanes,int_lanes,write_lanes,maximum);
    if(enforce)require(byte_calls==0&&int_lanes==4096&&write_lanes==4096,"IDENTITY_MUST_NOT_READ_FLAGS");
    int operations=region_calls;
    for(int fault=1;fault<=operations;fault++){
        reset(fault);result=call(&r,&s,31,0,0,&o,2,4096);
        require(pending&&result==-1&&region_calls==fault,"STOP_AFTER_INJECTED_EXCEPTION");
    }
    reset(0);result=call(&r,&s,37,128,128,&o,2,0);
    require(result==m3_window_result(37,0)&&region_calls==0&&!pending,"ZERO_LIMIT_BEFORE_CONTRADICTION");
    reset(0);result=call(&r,&s,0,0,0,&r,0,0);
    require(result==-1&&pending&&region_calls==0,"ALIAS_VALIDATION_BEFORE_ZERO");
    reset(0);result=call(&r,&s,0,256,256,&o,0,0);
    require(result==-1&&pending&&region_calls==0,"MASK_VALIDATION_BEFORE_ZERO");
    reset(0);result=call(&r,&s,0,128,128,&o,4099,2);
    require(result==-1&&pending&&region_calls==0,"OUTPUT_VALIDATION_BEFORE_CONTRADICTION");
    /* Every possible rejected prefix, including non-zero scan origins and a tail chunk.
     * The state pass has already located this boundary; no second search is needed. */
    for (int start = 0; start < 3; start++) for (int prefix = 0; prefix < 128; prefix++) {
        for (int i = 0; i < 8193; i++) states[i] = 0;
        states[start + prefix] = (jbyte) 128;
        reset(0); result = call(&r, &s, start, 128, 0, &o, 2, 1);
        require(result == m3_window_result(start + prefix + 1, 1), "PREFIX_CURSOR");
        require(output[2] == rows[start + prefix], "PREFIX_INDEX_COORDINATE");
        require(byte_lanes == 128 && write_lanes == 1, "PREFIX_MASK_AND_OUTPUT_WORK");
        if (enforce) require(int_lanes == 128 - prefix, "REJECTED_PREFIX_ROW_READ");
        if (start == 0 && (prefix == 0 || prefix == 127)) {
            printf("PREFIX_WORK prefix=%d flags=%ld rows=%ld writes=%ld calls=%d\n",
                    prefix, byte_lanes, int_lanes, write_lanes, region_calls);
        }
        int calls = region_calls;
        for (int fault = 1; fault <= calls; fault++) {
            reset(fault); result = call(&r, &s, start, 128, 0, &o, 2, 1);
            require(pending && result == -1 && region_calls == fault, "PREFIX_PENDING_STOP");
        }
    }
    for (int i = 0; i < 8193; i++) states[i] = 0;
    states[8192] = (jbyte) 128;
    reset(0); result = call(&r, &s, 8180, 128, 0, &o, 2, 1);
    require(result == m3_window_result(8193, 1) && output[2] == rows[8192], "TAIL_PREFIX_COORDINATE");
    if (enforce) require(int_lanes == 1, "TAIL_PREFIX_ROW_READ");
    /* Current bounded ABI: every prefix at every sub-chunk length, nonzero
     * origin, and matching sentinels immediately outside the exclusive window. */
    for (int take = 1; take <= 128; take++) for (int prefix = 0; prefix < take; prefix++) {
        const int start = 7;
        const int end = start + take;
        for (int i = 0; i < 8193; i++) states[i] = 0;
        states[start - 1] = (jbyte) 128;
        states[start + prefix] = (jbyte) 128;
        states[end] = (jbyte) 128;
        output[1] = 73; output[3] = 73;
        reset(0);
        result = call_bounded(&r, &s, start, end, 128, 0, &o, 2, 1);
        require(result == m3_window_result(start + prefix + 1, 1) && !pending,
                "BOUNDED_PREFIX_CURSOR");
        require(output[2] == rows[start + prefix] && output[1] == 73 && output[3] == 73,
                "BOUNDED_PREFIX_OUTPUT");
        require(byte_lanes == take && write_lanes == 1, "BOUNDED_PREFIX_TRANSFER");
        if (enforce) require(int_lanes == take - prefix, "BOUNDED_PREFIX_ROW_READ");
        if (take == 128 && (prefix == 0 || prefix == 127)) {
            printf("BOUNDED_PREFIX_WORK prefix=%d flags=%ld rows=%ld writes=%ld calls=%d\n",
                    prefix, byte_lanes, int_lanes, write_lanes, region_calls);
        }
        int calls = region_calls;
        for (int fault = 1; fault <= calls; fault++) {
            reset(fault);
            result = call_bounded(&r, &s, start, end, 128, 0, &o, 2, 1);
            require(pending && result == -1 && region_calls == fault,
                    "BOUNDED_PREFIX_PENDING_STOP");
        }
    }

    for (int i = 0; i < 4100; i++) output[i] = 73;
    reset(0); result = call_bounded(&r, &s, 17, 333, 0, 0, &o, 3, 4096);
    require(result == m3_window_result(333, 316) && !pending, "BOUNDED_IDENTITY_CURSOR");
    require(byte_calls == 0 && int_lanes == 316 && write_lanes == 316,
            "BOUNDED_IDENTITY_TRANSFER");
    for (int i = 0; i < 316; i++)
        require(output[3 + i] == rows[17 + i], "BOUNDED_IDENTITY_VALUES");
    require(output[2] == 73 && output[319] == 73, "BOUNDED_IDENTITY_SENTINELS");
    operations = region_calls;
    for (int fault = 1; fault <= operations; fault++) {
        reset(fault); result = call_bounded(&r, &s, 17, 333, 0, 0, &o, 3, 4096);
        require(pending && result == -1 && region_calls == fault,
                "BOUNDED_IDENTITY_PENDING_STOP");
    }

    reset(0); result = call_bounded(&r, &s, 31, 333, 128, 128, &o, 2, 4096);
    require(result == m3_window_result(333, 0) && !pending && region_calls == 0,
            "BOUNDED_CONTRADICTION_END");
    reset(0); result = call_bounded(&r, &s, 31, 333, 128, 128, &o, 2, 0);
    require(result == m3_window_result(31, 0) && !pending && region_calls == 0,
            "BOUNDED_ZERO_BEFORE_CONTRADICTION");
    reset(0); result = call_bounded(&r, &s, 31, 31, 128, 0, &o, 2, 4096);
    require(result == m3_window_result(31, 0) && !pending && region_calls == 0,
            "BOUNDED_ZERO_EXAMINATION");
    reset(0); result = call_bounded(&r, &s, 31, 30, 0, 0, &o, 2, 0);
    require(result == -1 && pending && region_calls == 0, "BOUNDED_REVERSED_END");
    reset(0); result = call_bounded(&r, &s, 31, 8194, 0, 0, &o, 2, 0);
    require(result == -1 && pending && region_calls == 0, "BOUNDED_OUTSIDE_END");
    reset(0); result = call_bounded(&r, &s, 31, 333, 0, 0, &r, 2, 0);
    require(result == -1 && pending && region_calls == 0, "BOUNDED_ALIAS_BEFORE_ZERO");

    /* Sparse matches across multiple chunks must not inspect or emit the
     * admitted sentinel at end, including when the final pending batch flushes. */
    for (int i = 0; i < 8193; i++) states[i] = (jbyte) (i % 5 == 0 ? 128 : 0);
    const int capacities[] = {0, 1, 127, 128, 129, 300};
    const int ends[] = {31, 32, 130, 288, 8193};
    for (unsigned int e = 0; e < sizeof(ends) / sizeof(ends[0]); e++) {
        for (unsigned int c = 0; c < sizeof(capacities) / sizeof(capacities[0]); c++) {
            int next = 31;
            int written = 0;
            jint expected[300];
            while (next < ends[e] && written < capacities[c]) {
                if (next % 5 == 0) expected[written++] = rows[next];
                next++;
            }
            for (int i = 0; i < 4100; i++) output[i] = 73;
            reset(0); result = call_bounded(&r, &s, 31, ends[e], 128, 0,
                    &o, 2, capacities[c]);
            require(result == m3_window_result(next, written) && !pending,
                    "BOUNDED_MULTICHUNK_CURSOR");
            for (int i = 0; i < written; i++)
                require(output[2 + i] == expected[i], "BOUNDED_MULTICHUNK_VALUES");
            require(output[1] == 73 && output[2 + written] == 73,
                    "BOUNDED_MULTICHUNK_SENTINELS");
            operations = region_calls;
            for (int fault = 1; fault <= operations; fault++) {
                reset(fault); result = call_bounded(&r, &s, 31, ends[e], 128, 0,
                        &o, 2, capacities[c]);
                require(pending && result == -1 && region_calls == fault,
                        "BOUNDED_MULTICHUNK_PENDING_STOP");
            }
        }
    }
    puts("NATIVE_REGION_PROOF_PASS");printf("checks=%ld\n",checks);
    return 0;
}
