/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.m3.collections.M3IndexMaskCompactor;
public final class M3FiveFamilyPreparedSelectionSelfTest {
private static int checks;
private static void check(boolean b){checks++;if(!b)throw new AssertionError("check="+checks);}
public static void main(String[] args){
Random rnd=new Random(0x5ee0a11L);
for(int n=0;n<500;n++){
int length=rnd.nextInt(130), count=rnd.nextInt(300);
Integer[] keys=new Integer[length]; String[] values=new String[length]; byte[] state=new byte[length];int[] physical=new int[count];
for(int i=0;i<length;i++){keys[i]=rnd.nextInt(8)-2;values[i]="v"+(i%5);state[i]=(byte)rnd.nextInt(256);}
for(int i=0;i<count;i++)physical[i]=length==0?0:rnd.nextInt(length);
if(length==0&&count!=0)continue;
var source=M3FiveLibraryIndexViews.capture(keys,values,physical,state);
AtomicInteger attempts=new AtomicInteger();
var even=M3FiveFamilyPreparedSelection.where(source,(key,val)->{attempts.incrementAndGet();return key!=null&&(key&1)==0;});
var statePlan=M3FiveFamilyPreparedSelection.fromStates(source,0x80,0x02);
BitSet oracleEven=new BitSet(),oracleState=new BitSet();
for(int i=0;i<count;i++){if((keys[physical[i]]&1)==0)oracleEven.set(i);
int st=state[physical[i]]&255;if((st&0x80)==0x80&&(st&0x02)==0)oracleState.set(i);}
check(even.bitsCopy().equals(oracleEven));check(statePlan.bitsCopy().equals(oracleState));
BitSet andBits=(BitSet)oracleEven.clone();andBits.and(oracleState);
var joint=even.and(statePlan);check(joint.bitsCopy().equals(andBits));
var inv=even.not();BitSet invOracle=(BitSet)oracleEven.clone();invOracle.flip(0,count);check(inv.bitsCopy().equals(invOracle));
var projected=joint.projection();check(projected==joint.projection());check(projected.size()==andBits.cardinality());
var jdk=projected.asJdkList();check(jdk.size()==andBits.cardinality());
int p=0;for(int idx=andBits.nextSetBit(0);idx>=0;idx=andBits.nextSetBit(idx+1)){check(projected.originalOrdinal(p)==idx);check(Objects.equals(jdk.get(p),values[physical[idx]]));p++;}
check(projected.asGuavaListMultimap().size()==p);check(projected.asApacheListValuedMap().size()==p);check(projected.asEclipseListMultimap().size()==p);
var spring=projected.asSpringMultiValueMap();int total=spring.values().stream().mapToInt(List::size).sum();check(total==p);
var sorted=projected.sortedByKey(Comparator.naturalOrder());for(int i=1;i<sorted.size();i++){check(sorted.keyAt(i-1)<=sorted.keyAt(i));}
check(attempts.get()==count);check(Arrays.equals(projected.slice(0,projected.size()).asJdkList().toArray(),jdk.toArray()));
if(M3IndexMaskCompactor.nativeAvailable()){var nativeView=joint.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);check(nativeView.size()==projected.size());for(int t=0;t<nativeView.size();t++)check(nativeView.originalOrdinal(t)==projected.originalOrdinal(t));}else{try{joint.projection(M3IndexMaskCompactor.Backend.NATIVE_REQUIRED);throw new AssertionError();}catch(UnsatisfiedLinkError expected){checks++;}}
}
System.out.println("PASS M3FiveFamilyPreparedSelectionSelfTest checks="+checks);
}
}
