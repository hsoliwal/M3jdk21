// SPDX-License-Identifier: Apache-2.0
/** LeetCode #34/#35/#88 signed-long windowed JNI differential and stale-ABI contract. */
package com.m3.collections;
import java.util.Arrays;
import java.util.Random;
public final class M3LongRankWindowContract {
    private static long checks;
    private static void expect(Class<? extends Throwable> t, Runnable r) {
        checks++;
        try { r.run(); } catch(Throwable x) { if (t.isInstance(x)) return; throw new AssertionError("wrong exception "+x,x); }
        throw new AssertionError("missing "+t.getSimpleName());
    }
    private static void eq(int[] x, int[] y, String reason) {
        checks++; if(!Arrays.equals(x,y)) throw new AssertionError(reason+" expected "+Arrays.toString(x)+" received "+Arrays.toString(y));
    }
    private static int lower(long[] a,long k){int i=0;while(i<a.length&&a[i]<k)i++;return i;}
    private static int upper(long[] a,long k){int i=0;while(i<a.length&&a[i]<=k)i++;return i;}
    private static void run(long[] sorted,long[] queries,int from,int len,int pad,boolean required) {
        int[] want=new int[2*len+pad+3];Arrays.fill(want,-777);
        for(int i=0;i<len;i++){want[pad+2*i]=lower(sorted,queries[from+i]);want[pad+2*i+1]=upper(sorted,queries[from+i]);}
        int[] got=new int[want.length];Arrays.fill(got,-777);
        M3LongRankBatchNative.equalRangesInto(sorted,queries,from,len,got,pad,required);
        eq(want,got,"window queryFrom="+from+" len="+len+" native="+required);
        long[] pureQueries=Arrays.copyOfRange(queries,from,from+len);
        int[] flat=new int[2*len];
        for(int i=0;i<len;i++){flat[2*i]=want[pad+2*i];flat[2*i+1]=want[pad+2*i+1];}
        eq(flat,M3LongRankBatchNative.equalRanges(sorted,pureQueries),"old API");
    }
    public static void main(String[] args){
        boolean required=Boolean.getBoolean("contract.requireNative");
        boolean older=Boolean.getBoolean("contract.olderNative");
        if(required&&!M3LongRankBatchNative.nativeAvailable())throw new AssertionError("missing native library");
        long[] edges={Long.MIN_VALUE,Long.MIN_VALUE,-9,-1,0,0,0,1,1,7,Long.MAX_VALUE};
        long[] queries={Long.MIN_VALUE,Long.MIN_VALUE+1,-10,-9,-8,-1,0,1,2,7,8,Long.MAX_VALUE-1,Long.MAX_VALUE};
        for(int length:new int[]{0,1,2,31,63,64,65,255,256,257,512,513,1024}){
            long[] q=new long[length+3];
            for(int i=0;i<q.length;i++)q[i]=queries[i%queries.length];
            run(edges,q,1,length,2,required&&!older);
            Arrays.sort(q);run(edges,q,1,length,1,required&&!older);
            run(new long[0],q,1,length,3,required&&!older);
        }
        Random rnd=new Random(0x3A55AE);
        for(int round=0;round<1800;round++){
            int size=rnd.nextInt(420);long[] sorted=new long[size];
            for(int i=0;i<size;i++)sorted[i]=rnd.nextInt(22)-10;
            if(size>0){sorted[0]=Long.MIN_VALUE;if(size>1)sorted[1]=Long.MAX_VALUE;}
            Arrays.sort(sorted);
            int len=rnd.nextInt(640);long[] q=new long[len+3];
            for(int i=0;i<q.length;i++)q[i]=rnd.nextInt(20)-8;
            if(round%3==0)Arrays.sort(q);
            run(sorted,q,2,len,round%3,required&&!older);
        }
        int[] out=new int[8];Arrays.fill(out,-777);
        for (Runnable bad:new Runnable[]{
          ()->M3LongRankBatchNative.equalRangesInto(null,queries,0,1,out,0,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,null,0,1,out,0,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,queries,0,1,null,0,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,queries,-1,1,out,0,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,queries,0,1,out,7,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,queries,0,-1,out,0,false),
          ()->M3LongRankBatchNative.equalRangesInto(edges,queries,0,7,out,0,false)
        }){checks++;try{bad.run();throw new AssertionError("did not fail");}catch(NullPointerException|IndexOutOfBoundsException expected){}}
        eq(new int[]{-777,-777,-777,-777,-777,-777,-777,-777},out,"failure never writes");
        if (older){
            run(edges,new long[80],0,80,0,false);
            expect(UnsatisfiedLinkError.class, ()->M3LongRankBatchNative.equalRangesInto(edges,new long[80],0,80,new int[160],0,true));
        }
        System.out.println("PASS M3_EQUAL_RANGE_WINDOW checks="+checks+" available="+M3LongRankBatchNative.nativeAvailable()+" nativeRequired="+required+" older="+older);
    }
}
