// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.*;
import java.math.BigInteger;
public final class M3KthPairDistanceContract {
 static int assertions;
 static void yes(boolean p,String msg){assertions++;if(!p)throw new AssertionError(msg);}
 static void equals(long expected,long actual){yes(expected==actual,"expected="+expected+" actual="+actual);}
 static void exception(Class<? extends Throwable> type,Runnable task){assertions++;try{task.run();}catch(Throwable e){if(type.isInstance(e))return;throw new AssertionError("wrong exception "+e,e);}throw new AssertionError("missing exception "+type);}
 static long[] brute(long[] sorted){
  ArrayList<BigInteger> dist=new ArrayList<>();
  for(int i=0;i<sorted.length;i++)for(int j=i+1;j<sorted.length;j++){
    dist.add(BigInteger.valueOf(sorted[j]).subtract(BigInteger.valueOf(sorted[i])));
  }
  Collections.sort(dist);
  long[] out=new long[dist.size()];
  for(int k=0;k<dist.size();k++)out[k]=dist.get(k).compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0?-1:dist.get(k).longValueExact();
  return out;
 }
 public static void main(String[] args){
  boolean required=Arrays.asList(args).contains("--native");
  System.out.println("native="+M3LongRankBatchNative.nativeAvailable());
  if(required)yes(M3LongRankBatchNative.nativeAvailable(),"require native library");
  exception(IndexOutOfBoundsException.class,()->M3LongRankBatchNative.kthPairDistance(new long[]{1},1));
  exception(ArithmeticException.class,()->M3LongRankBatchNative.kthPairDistance(new long[]{Long.MIN_VALUE,Long.MAX_VALUE},1));
  equals(0,M3LongRankBatchNative.kthPairDistance(new long[]{1,1,3},1));
  equals(2,M3LongRankBatchNative.kthPairDistance(new long[]{1,1,3},2));
  equals(2,M3LongRankBatchNative.kthPairDistance(new long[]{1,1,3},3));
  long[] sortedExt={Long.MIN_VALUE,0,Long.MAX_VALUE};
  equals(Long.MAX_VALUE,M3LongRankBatchNative.kthPairDistance(sortedExt,1));
  exception(ArithmeticException.class,()->M3LongRankBatchNative.kthPairDistance(sortedExt,2));
  long[] alias={1,2};
  exception(IllegalArgumentException.class,()->M3LongRankBatchNative.kthPairDistancesInto(new long[]{1,2},alias,0,1,alias,0,false));
  Random rnd=new Random(20261026);
  for(int run=0;run<320;run++){
    int n=rnd.nextInt(17);
    long[] input=new long[n];
    for(int i=0;i<n;i++)input[i]=switch(rnd.nextInt(11)){case 0->Long.MIN_VALUE;case 1->Long.MAX_VALUE;case 2->0;default->rnd.nextInt(40)-20;};
    Arrays.sort(input);
    long[] oracle=brute(input);
    for(int i=0;i<oracle.length;i++){
      long k=i+1;
      if(oracle[i]>=0){equals(oracle[i],M3LongRankBatchNative.kthPairDistance(input,k));}
      else exception(ArithmeticException.class,()->M3LongRankBatchNative.kthPairDistance(input,k));
    }
    long[] usable=new long[(int)Arrays.stream(oracle).filter(v->v>=0).count()];
    for(int i=0;i<usable.length;i++)usable[i]=i+1;
    long[] actual=M3LongRankBatchNative.kthPairDistances(input,usable);
    for(int i=0;i<actual.length;i++)equals(oracle[i],actual[i]);
    if(required && usable.length>0){long[] nativeOut=M3LongRankBatchNative.kthPairDistancesNativeRequired(input,usable);for(int i=0;i<actual.length;i++)equals(actual[i],nativeOut[i]);}
  }
  long[] large=new long[300];for(int i=0;i<large.length;i++)large[i]=i/3-100;
  long[] ranks=new long[]{1,2,7,80,500,2000,10000,large.length*(large.length-1L)/2};
  long[] actual=M3LongRankBatchNative.kthPairDistances(large,ranks);
  for(int i=0;i<ranks.length;i++)equals(M3LongRankBatchNative.kthPairDistance(large,ranks[i]),actual[i]);
  if(required){long[] requiredOut=M3LongRankBatchNative.kthPairDistancesNativeRequired(large,ranks);yes(Arrays.equals(actual,requiredOut),"JNI large equality");}
  long[] out=new long[]{-777,-777,-777,-777};
  M3LongRankBatchNative.kthPairDistancesInto(new long[]{1,1,2},new long[]{1,2},0,2,out,1,required);
  yes(Arrays.equals(new long[]{-777,0,1,-777},out),"window padding");
  long[] input={Long.MIN_VALUE,0,Long.MAX_VALUE};
  long[] sent={444,555,666};
  exception(ArithmeticException.class,()->M3LongRankBatchNative.kthPairDistancesInto(input,new long[]{1,2},0,2,sent,1,required));
  yes(Arrays.equals(new long[]{444,555,666},sent),"atomic rejection");
  System.out.println("PASS M3_KTH_PAIR_DISTANCE checks="+assertions);
 }
}
