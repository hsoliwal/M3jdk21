/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project */
/* SPDX-License-Identifier: Apache-2.0 */
/* Standalone C17 kernel fixture; never linked into the production shared library. */
#define M3_RUNLENGTH_PROBES 1
#include "../m3_runlength_postings.c"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <limits.h>
#include <string.h>

static unsigned long long checks;
static uint32_t state=UINT32_C(0x20802080);
static uint32_t random32(void) {
    state^=state<<13; state^=state>>17; state^=state<<5; return state;
}
static void require(int valid,const char*where){
    checks++;
    if (!valid){fprintf(stderr,"M3_RUNLENGTH_BATCH_KERNEL_FAIL %s check=%llu\n",where,checks);exit(2);}
}
static jint scalar(const jint *values,const jint *ends,jsize runs,jint x) {
    for(jsize i=0;i<runs;i++) if(values[i]==x)return ends[i]-(i==0?0:ends[i-1]);
    return 0;
}
static void verify(const jint* v,const jint* e,jsize n,const jint*q,jsize nq,const char*context){
    jint *output=calloc((size_t)nq+1,sizeof(jint));
    require(output!=NULL,"allocation");
    output[nq]=INT_MAX;
    m3_batch_counts(v,e,n,q,nq,output);
    for(jsize i=0;i<nq;i++){
        require(output[i]==scalar(v,e,n,q[i]),context);
        require(output[i]==m3_run_count(v,e,n,q[i]),"binary parity");
    }
    require(output[nq]==INT_MAX,"output canary");
    free(output);
}
int main(void) {
    const jint extremes[]={INT_MIN,-3,0,4,INT_MAX};
    const jint ends[]={5,6,9,100,121};
    const jint sorted[]={INT_MIN,INT_MIN,-4,-3,-3,0,0,2,4,4,INT_MAX,INT_MAX};
    const jint unsorted[]={INT_MAX,-3,0,INT_MIN,4,INT_MIN,1,4};
    verify(extremes,ends,5,sorted,12,"signed sorted");
    verify(extremes,ends,5,unsorted,8,"signed unsorted");
    verify(extremes,ends,5,unsorted,0,"empty queries");
    verify(extremes,ends,0,unsorted,8,"empty runs");
    // Shape/lane randomization independent of the native binary and merge logic.
    jint values[190], cumulative[190], query[410];
    for(unsigned trial=0; trial<2500;trial++){
        jsize runs=(jsize)(random32()%191u);
        jsize nq=(jsize)(random32()%411u);
        jint prev=-50000; jint sum=0;
        for(jsize j=0;j<runs;j++){
            prev+=1+(jint)(random32()%53u);sum+=1+(jint)(random32()%97u);
            values[j]=prev;cumulative[j]=sum;
        }
        for(jsize i=0;i<nq;i++){
            int choice=(int)(random32()%4u);
            query[i]=(choice<2 && runs!=0)?values[random32()%(uint32_t)runs]
                    :(jint)((int)(random32()%150001u)-75000);
        }
        if((trial%3u)==0){
            // Stable numeric sort, duplicates retained, test monotone path with Q>=64.
            for(jsize i=1;i<nq;i++){
                const jint x=query[i];jsize j=i;
                while(j>0 && query[j-1]>x){query[j]=query[j-1];j--;}
                query[j]=x;
            }
        }
        verify(values,cumulative,runs,query,nq,"random fixture");
    }
    // Probe counts measure run-value comparisons, NOT elapsed time or cache.
    const jsize n=4096,nq=32768;
    jint *wide=malloc((size_t)n*sizeof(jint));
    jint *sizes=malloc((size_t)n*sizeof(jint));
    jint *queries=malloc((size_t)nq*sizeof(jint));
    jint *out=malloc((size_t)nq*sizeof(jint));
    require(wide!=NULL && sizes!=NULL && queries!=NULL && out!=NULL,"large allocation");
    for(jsize i=0;i<n;i++){wide[i]=(jint)(i*3-6144);sizes[i]=i*7+7;}
    for(jsize i=0;i<nq;i++)queries[i]=(jint)((i/8)*3-6144);
    m3_runlength_comparisons=0;
    for(jsize i=0;i<nq;i++)out[i]=m3_run_count(wide,sizes,n,queries[i]);
    unsigned long long binary=m3_runlength_comparisons;
    m3_runlength_comparisons=0;
    m3_batch_counts(wide,sizes,n,queries,nq,out);
    unsigned long long adapted=m3_runlength_comparisons;
    require(adapted * 5u < binary,"sorted batch comparison reduction");
    verify(wide,sizes,n,queries,nq,"large sorted");
    for(jsize i=0;i<nq/2;i++){
        jint tmp=queries[i];queries[i]=queries[nq-i-1];queries[nq-i-1]=tmp;
    }
    verify(wide,sizes,n,queries,nq,"large descending fallback");
    free(wide);free(sizes);free(queries);free(out);
    printf("M3_RUNLENGTH_BATCH_KERNEL_PASS checks=%llu binary_comparisons=%llu monotone_comparisons=%llu\n",checks,binary,adapted);
    return 0;
}
