// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import jdk.internal.m3.collections.M3InternalLeaseRecycler;
public final class M3InternalLeaseRecyclerLazySelfTest {
 private static int checks;
 private static void check(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
 private static int capacity(M3InternalLeaseRecycler<?> r) {
  try {Field f=M3InternalLeaseRecycler.class.getDeclaredField("idle");f.setAccessible(true);return ((Object[])f.get(r)).length;}
  catch(ReflectiveOperationException e){throw new AssertionError(e);}
 }
 static final class Item {long id=-1;Object payload;}
 private static M3InternalLeaseRecycler<Item> pool(int cap){return new M3InternalLeaseRecycler<>(Item::new,(v,id)->{
  check(v.id==-1 && v.payload==null,"dirty acquired");v.id=id;
 },v->{v.payload=null;v.id=-1;},cap);}
 public static void main(String[] args)throws Exception {
  try(var p=pool(65536)) {
   check(capacity(p)==0,"eager array allocated");
   check(p.stats().idle()==0,"created idle slot");
   AtomicReference<Item> first=new AtomicReference<>();
   for(int i=0;i<10000;i++){long id=i;p.withBorrowed(id,item->{
    if(first.get()==null)first.set(item);else check(first.get()==item,"not reused");
    item.payload=new Object();
   });}
   check(p.stats().created()==1&&p.stats().reused()==9999,"loop no reuse");
   check(capacity(p)<=4,"idle cap too large");
   // nested synchronous borrows cannot expose the same instance
   p.withBorrowed(100,outer->p.withBorrowed(101,inner->check(inner!=outer,"nested share")));
   check(capacity(p)<=4,"nested >4");
   for(int depth=1;depth<=60;depth++)nested(p,depth);
   check(capacity(p)>=60 && capacity(p)<512,"geometric growth bound");
   int n=capacity(p);
   check(p.onPressure(M3RuntimeHeap.Pressure.HIGH)>=0,"high pressure");
   check(capacity(p)==0&&p.stats().idle()==0,"high pressure backing retained");
   check(n>0,"no growth happened");
   p.onPressure(M3RuntimeHeap.Pressure.NORMAL);
   p.withBorrowed(102,ignored->{});
   check(capacity(p)<=4 && p.stats().idle()==1,"normal recovery did not allocate lazily");
   p.trimIdle(0);
   check(capacity(p)==0,"explicit zero trim retained backing");
  }
  try(var p=pool(0)){p.withBorrowed(3,ignored->{});check(capacity(p)==0 && p.stats().idle()==0,"zero-cap contract");}
  try(var p=pool(20)) {
   nested(p,10);
   check(p.stats().idle()==10,"10 ids expected");
   p.onPressure(M3RuntimeHeap.Pressure.WARM);
   check(p.stats().idle()==5,"warm should retain maxIdle/4");
   p.onPressure(M3RuntimeHeap.Pressure.HIGH);
   check(capacity(p)==0,"high should drop backing");
  }
  // Concurrent checked-out objects never alias.
  try(var p=pool(4)) {
   CountDownLatch acquired=new CountDownLatch(2),release=new CountDownLatch(1);
   AtomicReference<Item> a=new AtomicReference<>(),b=new AtomicReference<>();
   List<Throwable> errors=new ArrayList<>();
   Runnable ra=()->{try{p.withBorrowed(1,v->{a.set(v);acquired.countDown();await(release);});}catch(Throwable t){synchronized(errors){errors.add(t);}}};
   Runnable rb=()->{try{p.withBorrowed(2,v->{b.set(v);acquired.countDown();await(release);});}catch(Throwable t){synchronized(errors){errors.add(t);}}};
   Thread t1=Thread.ofPlatform().start(ra),t2=Thread.ofPlatform().start(rb);
   check(acquired.await(10,java.util.concurrent.TimeUnit.SECONDS),"workers timeout");
   check(a.get()!=b.get(),"concurrent alias");
   release.countDown();t1.join();t2.join();
   check(errors.isEmpty(),"worker error");
   check(p.stats().active()==0,"unbalanced active");
  }
  System.out.println("PASS " + checks + " checked assertions (lazy backing, 10k reuse, 60 nesting, pressure and concurrency)");
 }
 private static void nested(M3InternalLeaseRecycler<Item> p,int depth){if(depth==0)return;p.withBorrowed(depth,x->nested(p,depth-1));}
 private static void await(CountDownLatch l){try{l.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
