/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#define M3_INTERVAL_PROBES 1
#include "../m3_interval_set.c"
#include <limits.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static unsigned long long checks;
static uint32_t rng = UINT32_C(0x22ca2026);
static uint32_t next_u32(void) {
    rng ^= rng << 13;
    rng ^= rng >> 17;
    rng ^= rng << 5;
    return rng;
}
static void require(int valid, const char *message) {
    checks++;
    if (!valid) {fprintf(stderr,"M3_INTERVAL_STREAM_FAIL %s check=%llu\n",message,checks);exit(4);}
}
static jbyte scalar(const jint *pairs, jsize intervals, jint value) {
    for (jsize i = 0; i < intervals; i++) {
        if (pairs[2*i] <= value && value < pairs[2*i+1]) return 1;
    }
    return 0;
}
static void verify(const jint *pairs, jsize intervals, const jint *queries, jsize length) {
    jbyte *out = malloc((size_t)(length + 1) * sizeof(jbyte));
    require(out != NULL,"output allocation");
    out[length] = (jbyte)29;
    m3_interval_batch(pairs,intervals,queries,length,out);
    require(out[length] == (jbyte)29,"output canary");
    for(jsize i = 0; i < length; i++) {
        require(out[i] == scalar(pairs,intervals,queries[i]),"independent interval parity");
    }
    free(out);
}
int main(void) {
    jint lane[32];
    jint queries[320];
    /* Exhaustive subset of small normalized signed half-open endpoints. */
    for (unsigned mask=0; mask<512u; mask++) {
        jsize used=0;
        for (int k=0; k<9; k++) {
            if ((mask & (1u<<k)) == 0) continue;
            jint start=(jint)k-5;
            if (used==0 || lane[2*(used-1)+1]<start) {
                lane[2*used]=start;
                lane[2*used+1]=start+1;
                used++;
            } else lane[2*(used-1)+1]=start+1;
        }
        const jsize lengths[]={0,1,63,64,65,320};
        for(jsize scenario=0;scenario<6;scenario++) {
            for(jsize l=0;l<6;l++) {
                const jsize n=lengths[l];
                for(jsize i=0;i<n;i++) {
                    switch(scenario) {
                    case 0: queries[i]=(jint)(i/8)-10;break;
                    case 1: queries[i]=(jint)(n-i)-160;break;
                    case 2: queries[i]=(jint)(i/8)-10; if (i==1)queries[i]=INT_MIN;break;
                    case 3: queries[i]=(jint)(i/8)-10; if (i==n-1)queries[i]=INT_MIN;break;
                    case 4: queries[i]=(jint)((next_u32()%31u))-15;break;
                    default: queries[i]=(i%2==0) ? INT_MAX : INT_MIN; break;
                    }
                }
                verify(lane,used,queries,n);
            }
        }
    }
    jint endpoints[]={INT_MIN,INT_MIN+2,-1,0,INT_MAX-1,INT_MAX};
    jint edge[]={INT_MIN,INT_MIN+1,INT_MIN+2,-1,0,INT_MAX-1,INT_MAX};
    verify(endpoints,3,edge,7);
    /* A long proven prefix followed by one descent must retain binary-search fallback. */
    const jsize n=12000,intervals=1000;
    jint *bounds=malloc((size_t)intervals*2*sizeof(jint));
    jint *ordered=malloc((size_t)n*sizeof(jint));
    require(bounds!=NULL && ordered!=NULL,"large allocations");
    for(jsize i=0;i<intervals;i++) {
        bounds[2*i]=i*7-3500;
        bounds[2*i+1]=bounds[2*i]+3;
    }
    for(jsize i=0;i<n;i++)ordered[i]=i-5500;
    ordered[n-1]=INT_MIN;
    m3_interval_comparisons=0;
    verify(bounds,intervals,ordered,n);
    const unsigned long long stream=m3_interval_comparisons;
    m3_interval_comparisons=0;
    for(jsize i=0;i<n;i++)require(m3_interval_single(bounds,intervals,ordered[i])==scalar(bounds,intervals,ordered[i]),"binary baseline");
    const unsigned long long binary=m3_interval_comparisons;
    require(stream * 3 < binary,"late-inversion comparison budget");
    ordered[1]=INT_MIN;
    verify(bounds,intervals,ordered,n);
    ordered[1]=ordered[0];
    ordered[n-1]=INT_MAX;
    verify(bounds,intervals,ordered,n);
    free(bounds);free(ordered);
    printf("M3_INTERVAL_STREAM_KERNEL_PASS checks=%llu stream_comparisons=%llu binary_comparisons=%llu\n",checks,stream,binary);
    return 0;
}
