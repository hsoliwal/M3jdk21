/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <limits.h>
#include "../m3_runlength_postings.c"

static unsigned long long checks;
static uint32_t random_state=UINT32_C(0xf15e713a);
static uint32_t rand_u32(void) {
    random_state^=random_state<<13;
    random_state^=random_state>>17;
    random_state^=random_state<<5;
    return random_state;
}
static void require(int yes, const char *case_name) {
    checks++;
    if (!yes) {
        fprintf(stderr,"M3_RUNLENGTH_KERNEL_FAIL: %s check=%llu\n",case_name,checks);
        exit(2);
    }
}
static void fixture(const jint *values,const jint *ends,jsize runs,jint target) {
    jint oracle=0;
    for(jsize i=0;i<runs;i++) {
        if(values[i]==target) oracle=ends[i]-(i==0?0:ends[i-1]);
    }
    require(m3_run_count(values,ends,runs,target)==oracle,"independent linear run count");
}
int main(void) {
    const jint extremes[]={INT_MIN,-1,0,1,INT_MAX};
    const jint cumulative[]={3,4,8,9,12};
    for (unsigned i=0;i<5;i++) fixture(extremes,cumulative,5,extremes[i]);
    fixture(extremes,cumulative,5,INT_MIN+1);
    fixture(extremes,cumulative,5,INT_MAX-1);
    fixture(extremes,cumulative,5,23);
    fixture(extremes,cumulative,0,0);
    jint values[150];
    jint ends[150];
    for(unsigned trial=0;trial<20000;trial++) {
        const jsize count=(jsize)(rand_u32()%151u);
        jint previous=-1000000;
        jint total=0;
        for(jsize i=0;i<count;i++) {
            previous += 1 + (jint)(rand_u32()%13u);
            total += 1 + (jint)(rand_u32()%121u);
            values[i]=previous;
            ends[i]=total;
        }
        for(jsize probe=0;probe<40;probe++) {
            const jint query=probe%2==0 && count>0 ?
                values[(jsize)(rand_u32()%(uint32_t)count)] :
                (jint)((int)(rand_u32()%4001u)-1000100);
            fixture(values,ends,count,query);
        }
    }
    printf("M3_RUNLENGTH_KERNEL_PASS checks=%llu\n",checks);
    return 0;
}
