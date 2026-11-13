/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Standalone C17 oracle; never linked into the shipping shared library. */
#define M3_SORTED_SET_PROBES 1
#include "../m3_sorted_int_set.c"
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>

static unsigned long long checks;
#define REQUIRE(condition, label) do { \
    checks++; if (!(condition)) {fprintf(stderr, "FAIL: %s check=%llu\n", label, checks); return 1;} \
} while (0)

static jsize reference(const jint *a, jsize na, const jint *b, jsize nb, jint *out,
                       unsigned long long *probes) {
    jsize i=0,j=0,used=0;
    while (i<na && j<nb) {
        (*probes)++;
        if (a[i]<b[j])i++;
        else if (a[i]>b[j])j++;
        else {out[used++]=a[i];i++;j++;}
    }
    return used;
}

static int exhaustive(void) {
    for (unsigned left=0; left<512; left++) {
        jint a[9]; jsize na=0;
        for(unsigned k=0;k<9;k++)if((left>>k)&1u)a[na++]=(jint)k-4;
        for(unsigned right=0;right<512;right++) {
            jint b[9]; jsize nb=0;
            for(unsigned k=0;k<9;k++)if((right>>k)&1u)b[nb++]=(jint)k-4;
            jint expected[9]={0}, actual[9]={0};
            unsigned long long probes=0;
            const jsize n=reference(a,na,b,nb,expected,&probes);
            const jsize matched=m3_intersect_sorted(a,na,b,nb,NULL);
            REQUIRE(matched==n,"exhaustive count");
            REQUIRE(m3_intersect_sorted(a,na,b,nb,actual)==n,"exhaustive emit");
            for(jsize x=0;x<n;x++)REQUIRE(actual[x]==expected[x],"exhaustive members");
            REQUIRE(m3_intersect_sorted(b,nb,a,na,NULL)==n,"symmetric count");
        }
    }
    return 0;
}

static int skew(void) {
    const jsize small_len=1000, big_len=1000000;
    jint *a=malloc((size_t)small_len*sizeof(jint));
    jint *b=malloc((size_t)big_len*sizeof(jint));
    jint *out=malloc((size_t)small_len*sizeof(jint));
    if(a==NULL||b==NULL||out==NULL) {free(a);free(b);free(out);return 1;}
    for(jsize i=0;i<big_len;i++) b[i]=(jint)i-500000;
    for(jsize i=0;i<small_len;i++) a[i]=(jint)((size_t)i*1000)-500000;
    unsigned long long baseline=0;
    jint *ref=malloc((size_t)small_len*sizeof(jint));
    if(ref==NULL){free(a);free(b);free(out);return 1;}
    jsize expected=reference(a,small_len,b,big_len,ref,&baseline);
    m3_sorted_set_comparisons=0;
    jsize matched=m3_intersect_sorted(a,small_len,b,big_len,NULL);
    const unsigned long long adapted=m3_sorted_set_comparisons;
    REQUIRE(matched==expected,"skew cardinality");
    REQUIRE(m3_intersect_sorted(a,small_len,b,big_len,out)==expected,"skew emit");
    for(jsize i=0;i<expected;i++)REQUIRE(out[i]==ref[i],"skew ordered members");
    REQUIRE(adapted*8u<baseline,"galloping must use fewer comparisons on skew fixture");
    printf("SKEW baseline_comparisons=%llu adaptive_comparisons=%llu\n",baseline,adapted);
    free(a);free(b);free(out);free(ref);
    return 0;
}

int main(void) {
    if(exhaustive()!=0)return 1;
    if(skew()!=0)return 2;
    const jint a[]={INT_MIN,-1,0,INT_MAX};
    const jint b[]={INT_MIN,0,INT_MAX};
    jint out[3];
    REQUIRE(m3_intersect_sorted(a,4,b,3,out)==3,"extreme count");
    REQUIRE(out[0]==INT_MIN&&out[1]==0&&out[2]==INT_MAX,"extreme ordered");
    printf("M3_SORTED_JNI_KERNEL_PASS checks=%llu\n",checks);
    return 0;
}
