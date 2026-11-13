/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "m3_ranked_byte_substrings.c"

static unsigned long long checks;
#define REQUIRE(condition) do { checks++; if (!(condition)) { \
    fprintf(stderr,"kernel failure line=%d assertion=%llu\n",__LINE__,checks); exit(2); \
} } while (0)
static void probe(const jint *suffix,const jint *lcp,const jlong *prefix,jsize n) {
    jlong total=prefix[n-1];
    for(jlong k=1;k<=total;k++) {
        jint out[2]={-1,-1};
        m3_ranked_span(suffix,lcp,prefix,n,k,out);
        jsize i=0;
        while(i<n && prefix[i]<k)i++;
        jlong previous=i==0?0:prefix[i-1];
        REQUIRE(i<n && out[0]==suffix[i]);
        REQUIRE(out[1]==lcp[i]+(jint)(k-previous));
        REQUIRE(out[1]>0);
    }
}
int main(void) {
    /* banana suffix order: a, ana, anana, banana, na, nana. */
    const jint banana_suffix[]={5,3,1,0,4,2};
    const jint banana_lcp[]={0,1,3,0,0,2};
    const jlong banana_prefix[]={1,3,5,11,13,15};
    probe(banana_suffix,banana_lcp,banana_prefix,6);
    uint32_t seed=0x91a2b3c4u;
    for(int run=0;run<7000;run++) {
        jint suffix[40],lcp[40];
        jlong pref[40];
        jsize n=1+(run%40);
        jlong cursor=0;
        for(jsize i=0;i<n;i++) {
            seed ^= seed<<13;seed ^= seed>>17;seed ^= seed<<5;
            suffix[i]=(jint)(seed%100000u);
            lcp[i]=(jint)(seed%9u);
            cursor+=1+(seed%9u);
            pref[i]=cursor;
        }
        probe(suffix,lcp,pref,n);
    }
    printf("M3_RANKED_C_KERNEL_PASS checks=%llu\n",checks);
    return 0;
}
