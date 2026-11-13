/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#define M3_INTERVAL_PROBES 1
#include "../m3_interval_set.c"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static unsigned long long checks;
static uint32_t seed = UINT32_C(0xf00d3121);
static uint32_t rand32(void) {
    seed ^= seed << 13;
    seed ^= seed >> 17;
    seed ^= seed << 5;
    return seed;
}
static void require(int condition, const char *message) {
    checks++;
    if (!condition) {
        fprintf(stderr, "M3_INTERVAL_KERNEL_FAIL %s check=%llu\n",message,checks);
        exit(5);
    }
}
static jbyte scalar(const jint *pairs, jsize count, jint point) {
    for (jsize i = 0; i < count; i++) {
        if (pairs[2*i] <= point && point < pairs[2*i+1]) return 1;
    }
    return 0;
}
static void test(const jint *pairs, jsize count, const jint *queries, jsize n) {
    jbyte *actual = malloc((size_t)(n+1)*sizeof(jbyte));
    require(actual != NULL, "malloc");
    actual[n] = 19;
    m3_interval_batch(pairs,count,queries,n,actual);
    require(actual[n] == 19,"canary");
    for(jsize i=0;i<n;i++) require(actual[i]==scalar(pairs,count,queries[i]),"scalar parity");
    free(actual);
}
int main(void) {
    jint queries[512];
    for (int i = 0; i < 512; ++i) queries[i] = (jint)(i / 32 - 8);
    for (unsigned mask = 0; mask < 1u<<10; mask++) {
        jint intervals[20];
        jsize count=0;
        for (int b=0;b<10;b++) {
            if ((mask&(1u<<b))==0) continue;
            const jint start=b-5;
            if (count==0 || intervals[2*(count-1)+1]<start) {
                intervals[2*count]=start;
                intervals[2*count+1]=start+1;
                count++;
            } else intervals[2*(count-1)+1]=start+1;
        }
        test(intervals,count,queries,512);
        for (int i=0;i<512;i++) queries[i]= (jint)((rand32()%41u)-20);
        test(intervals,count,queries,512);
        for (int i=0;i<512;i++) queries[i]=(jint)(i/32-8);
    }
    jint extremes[] = {INT_MIN, INT_MIN+1, -1, 0, INT_MAX-1,INT_MAX};
    jint edge_queries[] = {INT_MIN,INT_MIN+1,-1,0,INT_MAX-1,INT_MAX};
    test(extremes,3,edge_queries,6);
    jint *intervals = malloc(1000 * 2 * sizeof(jint));
    jint *ordered = malloc(12000*sizeof(jint));
    require(intervals != NULL && ordered != NULL,"big malloc");
    for (int i=0;i<1000;i++) {
        intervals[2*i]=i*7-3500;
        intervals[2*i+1]=intervals[2*i]+3;
    }
    for(int i=0;i<12000;i++) ordered[i]=i-5500;
    m3_interval_comparisons=0;
    test(intervals,1000,ordered,12000);
    unsigned long long sorted_cost=m3_interval_comparisons;
    /* Count independent binary-search work as a comparison budget, not a timing. */
    m3_interval_comparisons=0;
    for(int i=0;i<12000;i++) {
        require(m3_interval_single(intervals,1000,ordered[i])==scalar(intervals,1000,ordered[i]),
                "baseline binary parity");
    }
    unsigned long long binary_cost=m3_interval_comparisons;
    require(sorted_cost * 3 < binary_cost,"sorted batch improvement (comparisons)");
    free(intervals);free(ordered);
    printf("M3_INTERVAL_C_KERNEL_PASS checks=%llu sorted_comparisons=%llu binary_comparisons=%llu\n",
            checks,sorted_cost,binary_cost);
    return 0;
}
