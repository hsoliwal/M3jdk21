// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongPredicate;
public final class M3LongDeepFilterSelfTest {
  private static long checks;
  private static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
  public static void main(String[] args){ genericDepth(); sortedDepth(); shortCircuit(); customAndFallback(); System.out.println("M3_DEEP_FILTER_PASS checks="+checks); }
  static void genericDepth(){ M3LongCollection c=new One(); for(int i=0;i<100000;i++) c=new M3LongFilteredView(c,v->true); check(c.contains(1),"100k generic contains"); check(c.iterator().nextLong()==1,"100k generic iterate"); }
  static void sortedDepth(){ M3LongSortedSet s=new One(); for(int i=0;i<100000;i++) s=M3LongSortedViews.filter(s,v->true); check(s.contains(1),"100k sorted contains"); check(s.first()==1 && s.last()==1,"100k sorted endpoints"); }
  static void shortCircuit(){ StringBuilder b=new StringBuilder(); M3LongCollection c=new One(); for(int i=0;i<10;i++){ final int n=i; c=new M3LongFilteredView(c,v->{b.append(n);return n!=7;}); } check(!c.contains(1),"reject"); check(b.toString().equals("01234567"),"order "+b); }
  static void customAndFallback(){ class P implements LongPredicate { final AtomicInteger calls=new AtomicInteger(); public boolean test(long v){return true;} public LongPredicate and(LongPredicate o){calls.incrementAndGet(); return v->o.test(v)&&v!=42;} } P p=new P(); M3LongCollection c=new M3LongFilteredView(new One(),p); c=new M3LongFilteredView(c,v->true); check(p.calls.get()==1,"custom and dispatch"); check(c.contains(1),"custom result"); }
  static final class One implements M3LongSortedSet {
    public int size(){return 1;} public boolean contains(long v){return v==1;} public void clear(){throw new UnsupportedOperationException();}
    public M3LongIterator iterator(){return new M3LongIterator(){boolean done;public boolean hasNext(){return !done;}public long nextLong(){if(done)throw new NoSuchElementException();done=true;return 1;}};}
    public long[] toArray(){return new long[]{1};} public boolean add(long v){return false;} public boolean remove(long v){return false;}
    public long first(){return 1;} public long last(){return 1;} public long floor(long v,long d){return v>=1?1:d;} public long ceiling(long v,long d){return v<=1?1:d;} public long lower(long v,long d){return v>1?1:d;} public long higher(long v,long d){return v<1?1:d;} public int filterInPlace(LongPredicate p){return 0;}
  }
}
