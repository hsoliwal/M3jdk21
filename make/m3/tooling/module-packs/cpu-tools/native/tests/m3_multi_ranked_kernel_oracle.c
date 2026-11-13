/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <limits.h>
#include "m3_multi_ranked_substrings.c"

static unsigned long long checks;
static uint32_t seed = UINT32_C(0xdec031a5);
static uint32_t next_u32(void) {
    seed ^= seed << 13;
    seed ^= seed >> 17;
    seed ^= seed << 5;
    return seed;
}
static void require(int accepted, const char *reason) {
    checks++;
    if (!accepted) {
        fprintf(stderr,"M3_MULTI_RANKED_KERNEL_FAIL %s check=%llu\n",reason,checks);
        exit(2);
    }
}
static void verify(const jint *doc, const jint *offset, const jint *starts,
                   jsize documents, const jint *lcp, const jlong *cum, jsize n,
                   const jlong *ranks, jsize queries) {
    jint *actual = malloc((size_t)(3 * queries + 1) * sizeof(jint));
    require(actual != NULL,"malloc");
    actual[3*queries] = INT_MIN;
    require(m3_select_triples(doc, offset, starts, documents, lcp, cum, n, ranks,
                               queries, actual) == 0,"kernel success");
    require(actual[3*queries] == INT_MIN,"canary");
    for (jsize i = 0; i < queries; i++) {
        jsize first = 0;
        while (cum[first] < ranks[i]) first++;
        jlong previous = first == 0 ? 0 : cum[first-1];
        require(actual[3*i] == doc[first],"doc");
        require(actual[3*i+1] == offset[first] - starts[doc[first]],"offset");
        require(actual[3*i+2] == lcp[first] + ranks[i]-previous,"length");
    }
    free(actual);
}
int main(void) {
    for (int trial=0;trial<28000;trial++) {
        jsize docs=4, count=1+(jsize)(next_u32()%64u), queries=1+(jsize)(next_u32()%72u);
        jint starts[4]={0,2000,4000,6000};
        jint doc[64],offset[64],lcp[64];
        jlong prefix[64];
        jlong total=0;
        for(jsize i=0;i<count;i++) {
            doc[i]=(jint)(next_u32()%4u);
            offset[i]=starts[doc[i]]+(jint)(next_u32()%500u);
            lcp[i]=(jint)(next_u32()%5u);
            total+=lcp[i]+1+(jlong)(next_u32()%30u);
            prefix[i]=total;
        }
        jlong *ranks=malloc((size_t)queries*sizeof(jlong));
        require(ranks!=NULL,"ranks allocation");
        for(jsize i=0;i<queries;i++)ranks[i]=1+(jlong)((uint64_t)next_u32()%(uint64_t)total);
        verify(doc,offset,starts,docs,lcp,prefix,count,ranks,queries);
        free(ranks);
    }
    const jint docs[]={0,1}, offsets[]={0,200}, starts[]={0,200};
    const jint lcps[]={0,0};
    const jlong sums[]={2,5};
    const jlong bad[]={0};
    jint output[3]={0};
    require(m3_select_triples(docs,offsets,starts,2,lcps,sums,2,bad,1,output)!=0,
            "reject invalid rank");
    const jlong last[]={5};
    verify(docs,offsets,starts,2,lcps,sums,2,last,1);
    printf("M3_MULTI_RANKED_C_KERNEL_PASS checks=%llu\n",checks);
    return 0;
}
