// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
/** Dependency-light behavioral proof; not a replacement for repository JUnit/reactor tests. */
public final class FlowV2Proof {
    private static int tests,randomCases;
    private FlowV2Proof() {
    }
    record Row(String key,long value) {
    }
    record Collision(int value) {
        @Override public int hashCode() {
            return 1;
        }
    }
    interface Work {
        void run()throws Exception;
    }
    static void test(String name,Work task)throws Exception {
        task.run();
        tests++;
        System.out.println("PASS "+name);
    }
    static void yes(boolean b) {
        if(!b)throw new AssertionError();
    }
    static void eq(Object a,Object b) {
        if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);
    }
    static void arr(long[] a,long[] b) {
        if(!Arrays.equals(a,b))throw new AssertionError(Arrays.toString(a)+" != "+Arrays.toString(b));
    }
    static void arr(int[] a,int[] b) {
        if(!Arrays.equals(a,b))throw new AssertionError(Arrays.toString(a)+" != "+Arrays.toString(b));
    }
    static void raises(Class<? extends Throwable> type,Work task)throws Exception {
        try {
            task.run();
        }
        catch(Throwable e) {
            if(type.isInstance(e))return;
            throw new AssertionError("expected "+type+", got "+e,e);
        }
        throw new AssertionError("missing "+type);
    }
    static M3Flow<Integer> range(int n) {
        return M3Flow.indexed(()->n,i->i);
    }
    public static void main(String[] args)throws Exception {
        boolean nativeLoaded=args.length>0;
        if(nativeLoaded)System.load(args[0]);
        test("construction is lazy, aggregation metadata never loads payload",()-> {
            AtomicInteger read=new AtomicInteger(),keys=new AtomicInteger();
            var stage=M3Flow.<Row>indexed(()->100,i-> {
                read.incrementAndGet();
                throw new AssertionError();
            }
            ) .whereIndex(i->i%2==0).groupByIndex(i-> {
                keys.incrementAndGet();
                return i%4;
            }
            ).sumLongAt(i->i).filterGroups((k,v)->v>0);
            eq(read.get()+keys.get(),0);
            eq(stage.toMap(),Map.of(0,1200L,2,1250L));
            eq(read.get(),0);
            eq(keys.get(),50);
        }
        );
        test("late budget applies to previously composed distinct stages",()-> {
            raises(IllegalArgumentException.class,()->range(3).distinctByIndex(i->i).budget(3,2).count());
            eq(range(5).distinctByIndex(i->i%2).budget(5,2).count(),2L);
            eq(range(3).distinctByIndex(i->i).budget(3,2).limit(2).count(),2L);
            eq(range(2).budget(2,0).distinctByIndex(i->i).budget(2,2).count(),2L);
        });
        test("object filter/key/value share one loaded row",()-> {
            AtomicInteger n=new AtomicInteger();
            var result=M3Flow.indexed(()->10,i-> {
                n.incrementAndGet();
                return new Row("x",i);
            }
            ).filter(r->r.value()>4).groupBy(Row::key).sumLong(Row::value).toMap();
            eq(result,Map.of("x",35L));
            eq(n.get(),10);
        }
        );
        test("declared predicate ordering preserved",()-> {
            StringBuilder trace=new StringBuilder();
            eq(range(3).filter(i-> {
                trace.append('a');
                return true;
            }
            ).whereIndex(i-> {
                trace.append('b');
                return true;
            }
            ).filter(i-> {
                trace.append('c');
                return true;
            }
            ).count(),3L);
            eq(trace.toString(),"abcabcabc");
        }
        );
        test("null and colliding keys, first encounter order",()-> {
            Object[] keys= {
                new Collision(2),null,new Collision(1),new Collision(2),null
            }
            ;
            var m=range(5).groupByIndex(i->keys[i]).counting().toMap();
            eq(new ArrayList<>(m.keySet()),Arrays.asList(new Collision(2),null,new Collision(1)));
            eq(m.get(null),2L);
        }
        );
        test("summary count/sum/min/max and wrapped sum",()-> {
            long[] v= {
                Long.MAX_VALUE,1,Long.MIN_VALUE,-1
            }
            ;
            var s=range(4).groupByIndex(i->0).summarizingLongAt(i->v[i]).toMap().get(0);
            eq(s,new M3LongSummary(4,-1,Long.MIN_VALUE,Long.MAX_VALUE));
            eq(s.average(),-0.25);
        }
        );
        test("summary continuation filtering",()-> {
            eq(range(6).groupByIndex(i->i%2).summarizingLongAt(i->i).filterGroups((k,s)->s.sum()>7).toMap().size(),1);
        }
        );
        test("aggregate primitive map and explicit owning terminal",()-> {
            var map=range(6).groupByIndex(i->i%2).sumLongAt(i->i).mapLong(x->2*x).filterGroups((k,v)->v>12).toMap();
            eq(map,Map.of(1,18L));
            raises(UnsupportedOperationException.class,()->map.put(5,1L));
        }
        );
        test("empty zero budget and distinct budget",()-> {
            eq(range(0).budget(0,0).groupByIndex(i->i).counting().countGroups(),0L);
            raises(IllegalArgumentException.class,()->range(3).budget(2,3).count());
            raises(IllegalArgumentException.class,()->range(3).budget(3,2).distinctByIndex(i->i).count());
        }
        );
        test("limit zero prevents upstream callbacks",()-> {
            AtomicInteger n=new AtomicInteger();
            eq(range(9).filter(i-> {
                n.incrementAndGet();
                return true;
            }
            ).limit(0).count(),0L);
            eq(n.get(),0);
        }
        );
        test("skip/limit compose in declared order",()-> {
            eq(range(20).filter(i->i%2==0).skip(2).limit(3).toList(),List.of(4,6,8));
            eq(range(9).limit(1).filter(i->false).count(),0L);
            eq(range(9).filter(i->i>4).limit(1).toList(),List.of(5));
        }
        );
        test("take/drop and early stop callback counts",()-> {
            AtomicInteger n=new AtomicInteger();
            eq(range(20).takeWhile(i-> {
                n.incrementAndGet();
                return i<3;
            }
            ).toList(),List.of(0,1,2));
            eq(n.get(),4);
            n.set(0);
            eq(range(6).dropWhile(i-> {
                n.incrementAndGet();
                return i<3;
            }
            ).toList(),List.of(3,4,5));
            eq(n.get(),4);
        }
        );
        test("short-circuit matches and empty identity",()-> {
            AtomicInteger n=new AtomicInteger();
            yes(range(100).anyMatch(i-> {
                n.incrementAndGet();
                return i==2;
            }
            ));
            eq(n.get(),3);
            yes(!range(0).anyMatch(i->true));
            yes(range(0).allMatch(i->false));
            yes(!range(8).allMatch(i->i<4));
        }
        );
        test("distinct-by null/stability and fresh state on repeated terminal",()-> {
            Object[] a= {
                null,"b",null,"a","b"
            }
            ;
            var p=range(5).distinctByIndex(i->a[i]);
            eq(p.toList(),List.of(0,1,3));
            eq(p.toList(),List.of(0,1,3));
        }
        );
        test("primitive scan and pairwise ordering",()-> {
            arr(range(5).mapIndexToLong(i->i+1).scan(10,Long::sum).toArray(),new long[] {
                11,13,16,20,25
            }
            );
            arr(range(5).mapIndexToLong(i->i*i).pairwise((a,b)->b-a).toArray(),new long[] {
                1,3,5,7
            }
            );
        }
        );
        test("primitive nonassociative scan and modular overflow",()-> {
            arr(range(3).mapIndexToLong(i->i+1).scan(20,(a,b)->a-b).toArray(),new long[] {
                19,17,14
            }
            );
            arr(range(2).mapIndexToLong(i->i==0?Long.MAX_VALUE:1).scan(0,Long::sum).toArray(),new long[] {
                Long.MAX_VALUE,Long.MIN_VALUE
            }
            );
        }
        );
        test("primitive pipeline stop, filter and empty pair",()-> {
            AtomicInteger calls=new AtomicInteger();
            arr(range(100).mapIndexToLong(i-> {
                calls.incrementAndGet();
                return i;
            }
            ).scan(0,Long::sum).limit(3).toArray(),new long[] {
                0,1,3
            }
            );
            eq(calls.get(),3);
            eq(range(1).mapIndexToLong(i->7).pairwise(Long::sum).count(),0L);
            arr(range(10).mapIndexToLong(i->i).filterLong(x->x%2==0).takeWhile(x->x<6).toArray(),new long[] {
                0,2,4
            }
            );
        }
        );
        test("zipShortest consumes no surplus left values",()-> {
            AtomicInteger reads=new AtomicInteger();
            arr(range(9).mapIndexToLong(i-> {
                reads.incrementAndGet();
                return i;
            }
            ).zipShortest(new long[] {
                10,20
            }
            ,Long::sum).toArray(),new long[] {
                10,21
            }
            );
            eq(reads.get(),2);
            reads.set(0);
            eq(range(9).mapIndexToLong(i-> {
                reads.incrementAndGet();
                return i;
            }
            ).zipShortest(new long[0],Long::sum).count(),0L);
            eq(reads.get(),0);
        }
        );
        test("adjacent runs keep repeated nonadjacent keys",()-> {
            String[] a= {
                "a","a","b","a","a",null,null
            }
            ;
            List<String> keys=new ArrayList<>();
            List<Long> lengths=new ArrayList<>();
            range(a.length).runsByIndex(i->a[i]).forEach((k,n)-> {
                keys.add(k);
                lengths.add(n);
            }
            );
            eq(keys,Arrays.asList("a","b","a",null));
            eq(lengths,List.of(2L,1L,2L,2L));
            arr(range(a.length).runsByIndex(i->a[i]).counts().limit(1).toArray(),new long[] {
                2
            }
            );
        }
        );
        test("virtual view no payload reads, nulls, slices and closure",()-> {
            AtomicInteger reads=new AtomicInteger();
            var flow=M3Flow.<Object>indexed(()->6,i-> {
                reads.incrementAndGet();
                return null;
            }
            );
            var view=flow.whereIndex(i->i%2==1).virtualize();
            eq(reads.get(),0);
            eq(view.size(),3);
            arr(view.copyIndices(),new int[] {
                1,3,5
            }
            );
            var slice=view.subList(1,3);
            eq(slice.get(0),null);
            eq(reads.get(),1);
            view.close();
            raises(IllegalStateException.class,slice::size);
        }
        );
        test("temporary views release source on success and failure",()-> {
            AtomicReference<M3IndexView<Integer>> ref=new AtomicReference<>();
            int[] a=range(5).withView(v-> {
                ref.set(v);
                return v.copyIndices();
            }
            );
            arr(a,new int[] {
                0,1,2,3,4
            }
            );
            raises(IllegalStateException.class,()->ref.get().get(0));
            raises(IllegalArgumentException.class,()->range(5).withView(v-> {
                ref.set(v);
                throw new IllegalArgumentException();
            }
            ));
            raises(IllegalStateException.class,()->ref.get().size());
        }
        );
        test("view reports version drift without materialization",()-> {
            AtomicLong version=new AtomicLong();
            var view=M3Flow.indexed(()->2,i->i,version::get).virtualize();
            version.incrementAndGet();
            raises(ConcurrentModificationException.class,view::size);
            view.close();
        }
        );
        test("standard collector order/duplicates/null payloads",()-> {
            var f=M3Flow.<Integer>indexed(()->8,i->null);
            try(var v=Stream.of(5,1,5,0).collect(M3VirtualCollectors.toView(f))) {
                arr(v.copyIndices(),new int[] {
                    5,1,5,0
                }
                );
                eq(v.materialize(),Arrays.asList(null,null,null,null));
            }
            yes(M3VirtualCollectors.toView(f).characteristics().isEmpty());
        }
        );
        test("parallel primitive collector preserves encounter order",()-> {
            try(var v=M3VirtualCollectors.collectIndices(range(10000),IntStream.range(0,10000).parallel(),null)) {
                arr(v.copyIndices(),IntStream.range(0,10000).toArray());
            }
        }
        );
        test("collector rejects null/invalid input and consumed state",()-> {
            var f=range(3);
            raises(NullPointerException.class,()->Stream.<Integer>of((Integer)null).collect(M3VirtualCollectors.toView(f)));
            raises(IndexOutOfBoundsException.class,()->IntStream.of(3).boxed().collect(M3VirtualCollectors.toView(f)));
            var c=M3VirtualCollectors.toView(f);
            var a=c.supplier().get();
            c.accumulator().accept(a,0);
            try(var v=c.finisher().apply(a)) {
                eq(v.size(),1);
                raises(IllegalStateException.class,()->c.accumulator().accept(a,1));
            }
        }
        );
        test("window full/partial/gap semantics",()-> {
            eq(windows(7,3,2,M3Windows.Tail.DROP),List.of(List.of(0,1,2),List.of(2,3,4),List.of(4,5,6)));
            eq(windows(5,3,2,M3Windows.Tail.PARTIAL),List.of(List.of(0,1,2),List.of(2,3,4),List.of(4)));
            eq(windows(7,2,4,M3Windows.Tail.PARTIAL),List.of(List.of(0,1),List.of(4,5)));
            eq(windows(2,10,1,M3Windows.Tail.PARTIAL),List.of(List.of(0,1),List.of(1)));
        }
        );
        test("window metadata no loads and generation never resurrects",()-> {
            AtomicInteger reads=new AtomicInteger();
            AtomicReference<M3Windows.Window<Integer>> first=new AtomicReference<>();
            M3Flow.indexed(()->5,i-> {
                reads.incrementAndGet();
                return i;
            }
            ).windowed(2,1,M3Windows.Tail.DROP).forEach(w-> {
                if(first.get()==null)first.set(w);
                else {
                    try {
                        raises(IllegalStateException.class,()->first.get().size());
                    }
                    catch(Exception e) {
                        throw new AssertionError(e);
                    }
                }
                yes(w.size()==2);
                w.copyIndices();
            }
            );
            eq(reads.get(),0);
            raises(IllegalStateException.class,()->first.get().size());
        }
        );
        test("window temporary scope invalidates on callback exception",()-> {
            AtomicReference<M3Windows.Window<Integer>> ref=new AtomicReference<>();
            raises(IllegalStateException.class,()->range(4).windowed(2,1,M3Windows.Tail.DROP).forEach(w-> {
                ref.set(w);
                throw new IllegalStateException("client");
            }
            ));
            raises(IllegalStateException.class,()->ref.get().get(0));
        }
        );
        test("window primitive continuation and early termination",()-> {
            AtomicInteger rows=new AtomicInteger();
            arr(range(100).whereIndex(i-> {
                rows.incrementAndGet();
                return true;
            }
            ).windowed(3,1,M3Windows.Tail.PARTIAL).mapToLong(w->w.sourceIndex(0)+w.sourceIndex(2)).limit(2).toArray(),new long[] {
                2,4
            }
            );
            eq(rows.get(),4);
        }
        );
        test("top-k stable ties, reversed order and extreme comparator values",()-> {
            int[] a= {
                9,1,1,7,2,1
            }
            ;
            var f=range(a.length);
            try(var v=f.topKByIndex(3,(i,j)->Integer.compare(a[i],a[j])).toView()) {
                arr(v.copyIndices(),new int[] {
                    1,2,5
                }
                );
            }
            eq(f.topKByIndex(2,(i,j)->a[i]==a[j]?0:a[i]>a[j]?Integer.MIN_VALUE:1).toList(),List.of(0,3));
        }
        );
        test("top-k zero is no comparison/filter/materialization",()-> {
            AtomicInteger calls=new AtomicInteger();
            eq(range(8).filter(i-> {
                calls.incrementAndGet();
                return true;
            }
            ).topKByIndex(0,(i,j)-> {
                calls.incrementAndGet();
                return 0;
            }
            ).toList(),List.of());
            eq(calls.get(),0);
        }
        );
        test("top-k primitive continuation retains no payload",()-> {
            AtomicInteger loads=new AtomicInteger();
            long sum=M3Flow.<Integer>indexed(()->10,i-> {
                loads.incrementAndGet();
                return i;
            }
            ).topKByIndex(3,(a,b)->Integer.compare(b,a)).mapIndexToLong(i->i).sum();
            eq(sum,24L);
            eq(loads.get(),0);
        }
        );
        test("argument and state budget validation",()-> {
            raises(IllegalArgumentException.class,()->range(9).windowed(0,1,M3Windows.Tail.DROP));
            raises(IllegalArgumentException.class,()->range(9).limit(-1));
            raises(IllegalArgumentException.class,()->range(9).skip(-1));
            raises(IllegalArgumentException.class,()->range(9).topKByIndex(-1,Integer::compare));
            raises(IllegalArgumentException.class,()->range(9).budget(9,1).windowed(3,1,M3Windows.Tail.DROP).forEach(w-> {
            }
            ));
            raises(IllegalArgumentException.class,()->range(9).budget(9,1).topKByIndex(2,Integer::compare).toList());
        }
        );
        test("reported mutation fails on callback boundary",()-> {
            AtomicLong epoch=new AtomicLong();
            var f=M3Flow.indexed(()->3,i->i,epoch::get);
            raises(ConcurrentModificationException.class,()->f.filter(i-> {
                epoch.incrementAndGet();
                return true;
            }
            ).count());
        }
        );
        test("progress cancellation before action and reuse",()-> {
            Monitor m=new Monitor();
            eq(range(5).count(m),5L);
            eq(m.work,5L);
            m.cancel=true;
            raises(CancellationException.class,()->range(5).count(m));
            AtomicInteger actions=new AtomicInteger();
            Monitor m2=new Monitor();
            raises(CancellationException.class,()->range(5).filter(i-> {
                m2.cancel=true;
                return true;
            }
            ).forEach(i->actions.incrementAndGet(),m2));
            eq(actions.get(),0);
        }
        );
        test("parallel key order and bounded worker count",()-> {
            Map<Integer,List<Integer>> seen=new ConcurrentHashMap<>();
            AtomicInteger active=new AtomicInteger(),peak=new AtomicInteger();
            range(1000).groupByIndex(i->i%7).forEachParallel(3,i-> {
                int now=active.incrementAndGet();
                peak.accumulateAndGet(now,Math::max);
                try {
                    seen.computeIfAbsent(i%7,k->new ArrayList<>()).add(i);
                }
                finally {
                    active.decrementAndGet();
                }
            }
            );
            yes(peak.get()<=3);
            eq(active.get(),0);
            for(int key=0;key<7;key++) {
                final int k=key;
                eq(seen.get(k),IntStream.range(0,1000).filter(i->i%7==k).boxed().toList());
            }
        }
        );
        test("distinct keys truly overlap",()-> {
            CountDownLatch latch=new CountDownLatch(2);
            range(2).groupByIndex(i->i).forEachParallel(2,i-> {
                latch.countDown();
                try {
                    yes(latch.await(5,TimeUnit.SECONDS));
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            );
        }
        );
        test("failure quiesces before return",()-> {
            AtomicInteger active=new AtomicInteger();
            raises(IllegalStateException.class,()->range(100).groupByIndex(i->i%4).forEachParallel(4,i-> {
                active.incrementAndGet();
                try {
                    if(i==0)throw new IllegalStateException("client");
                }
                finally {
                    active.decrementAndGet();
                }
            }
            ));
            eq(active.get(),0);
        }
        );
        test("parent interruption joins workers and preserves interrupt",()-> {
            Thread parent=Thread.currentThread();
            CountDownLatch entered=new CountDownLatch(2);
            Thread interrupter=new Thread(()-> {
                try {
                    if(entered.await(5,TimeUnit.SECONDS))parent.interrupt();
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            );
            interrupter.start();
            try {
                raises(CancellationException.class,()->range(2).groupByIndex(i->i).forEachParallel(2,i-> {
                    entered.countDown();
                    try {
                        Thread.sleep(10000);
                    }
                    catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                ));
                yes(Thread.interrupted());
            }
            finally {
                Thread.interrupted();
                interrupter.join();
            }
        }
        );
        test("partitions metadata source order and lifetime",()-> {
            AtomicInteger loads=new AtomicInteger();
            AtomicReference<M3Partitions<Integer,Integer>> ref=new AtomicReference<>();
            M3Flow.indexed(()->8,i-> {
                loads.incrementAndGet();
                return i;
            }
            ).partitionByIndex(i->i%2).withPartitions(p-> {
                ref.set(p);
                eq(p.rowCount(),8);
                arr(p.partition(0).copyIndices(),new int[] {
                    0,2,4,6
                }
                );
                eq(loads.get(),0);
                eq(p.partition(1).get(0),1);
                return null;
            }
            );
            raises(IllegalStateException.class,()->ref.get().size());
        }
        );
        test("500 randomized windows vs owning oracle",()-> {
            Random r=new Random(24);
            for(int t=0;t<500;t++) {
                int n=r.nextInt(60),w=1+r.nextInt(20),step=1+r.nextInt(25);
                for(var tail:M3Windows.Tail.values()) {
                    List<List<Integer>> expected=new ArrayList<>();
                    for(int start=0;start<n;start+=step) {
                        if(tail==M3Windows.Tail.DROP&&start+w>n)break;
                        expected.add(IntStream.range(start,Math.min(n,start+w)).boxed().toList());
                    }
                    eq(windows(n,w,step,tail),expected);
                }
                randomCases++;
            }
        }
        );
        test("500 randomized stable top-k selections",()-> {
            Random r=new Random(89);
            for(int t=0;t<500;t++) {
                int n=r.nextInt(80),k=r.nextInt(90);
                int[] a=r.ints(n,-10,10).toArray();
                List<Integer> expected=IntStream.range(0,n).filter(i->i%3!=0).boxed().sorted(Comparator.comparingInt(i->a[i])).limit(k).toList();
                eq(range(n).whereIndex(i->i%3!=0).topKByIndex(k,(i,j)->Integer.compare(a[i],a[j])).toList(),expected);
                randomCases++;
            }
        }
        );
        test("200 randomized summaries vs JDK oracle",()-> {
            Random r=new Random(93);
            for(int t=0;t<200;t++) {
                List<Row> rows=IntStream.range(0,r.nextInt(200)).mapToObj(i->new Row("k"+r.nextInt(10),r.nextLong())).toList();
                var expected=rows.stream().collect(Collectors.groupingBy(Row::key,LinkedHashMap::new,Collectors.summarizingLong(Row::value)));
                var actual=M3Flow.indexed(rows::size,rows::get).groupBy(Row::key).summarizingLong(Row::value).toMap();
                eq(new ArrayList<>(actual.keySet()),new ArrayList<>(expected.keySet()));
                expected.forEach((k,v)->eq(actual.get(k),new M3LongSummary(v.getCount(),v.getSum(),v.getMin(),v.getMax())));
                randomCases++;
            }
        }
        );
        test("200 randomized collector splits match unpartitioned",()-> {
            Random r=new Random(4);
            for(int t=0;t<200;t++) {
                int[] indices=r.ints(r.nextInt(500),0,50).toArray();
                try(var v=M3VirtualCollectors.collectIndices(range(50),Arrays.stream(indices).parallel(),null)) {
                    arr(v.copyIndices(),indices);
                }
                randomCases++;
            }
        }
        );
        test("Java dense sum and prefix validation",()->kernelValidation(M3NativeLongs.Backend.JAVA));
        test("Java cancellation during primitive batch",()->kernelCancel(M3NativeLongs.Backend.JAVA));
        if(nativeLoaded) {
            test("native primitive validation",()->kernelValidation(M3NativeLongs.Backend.NATIVE));
            test("native cancellation and thrown checkpoint exception",()->kernelCancel(M3NativeLongs.Backend.NATIVE));
            test("JNI boundary and long overflow differential",()-> {
                Random r=new Random(54);
                for(int n:new int[] {
                    0,1,1023,1024,1025,2049,10000
                }
                ) {
                    long[] v=r.longs(n).toArray();
                    arr(M3NativeLongs.prefixSum(v,Long.MAX_VALUE),M3NativeLongs.prefixSum(v,Long.MAX_VALUE,M3NativeLongs.Backend.NATIVE,null));
                    for(int g:new int[] {
                        1,7,1025
                    }
                    ) {
                        int[] keys=r.ints(n,0,g).toArray();
                        arr(M3NativeLongs.sumByDenseKey(keys,v,g),M3NativeLongs.sumByDenseKey(keys,v,g,M3NativeLongs.Backend.NATIVE,null));
                        randomCases++;
                    }
                }
            }
            );
            test("retained legacy JNI export still callable",()-> {
                var method=M3NativeLongs.class.getDeclaredMethod("sumNative",int[].class,long[].class,int.class);
                method.setAccessible(true);
                arr((long[])method.invoke(null,new int[] {
                    0,0
                }
                ,new long[] {
                    Long.MAX_VALUE,1
                }
                ,1),new long[] {
                    Long.MIN_VALUE
                }
                );
            }
            );
        }
        else test("native mode never silently falls back",()->raises(UnsatisfiedLinkError.class,()->M3NativeLongs.prefixSum(new long[0],0,M3NativeLongs.Backend.NATIVE,null)));
        System.out.println("TEST_GROUPS="+tests+" RANDOM_CASES="+randomCases+" PASS");
    }
    static List<List<Integer>> windows(int n,int w,int step,M3Windows.Tail tail) {
        List<List<Integer>> result=new ArrayList<>();
        range(n).windowed(w,step,tail).forEach(win->result.add(win.materialize()));
        return result;
    }
    static void kernelValidation(M3NativeLongs.Backend b)throws Exception {
        arr(M3NativeLongs.sumByDenseKey(new int[0],new long[0],0,b,null),new long[0]);
        arr(M3NativeLongs.prefixSum(new long[] {
            Long.MAX_VALUE,1,Long.MIN_VALUE,-1
        }
        ,0,b,null),new long[] {
            Long.MAX_VALUE,Long.MIN_VALUE,0,-1
        }
        );
        raises(IllegalArgumentException.class,()->M3NativeLongs.sumByDenseKey(new int[] {
            -1
        }
        ,new long[1],1,b,null));
        raises(IllegalArgumentException.class,()->M3NativeLongs.sumByDenseKey(new int[] {
            1
        }
        ,new long[1],1,b,null));
        raises(IllegalArgumentException.class,()->M3NativeLongs.sumByDenseKey(new int[1],new long[0],1,b,null));
        raises(NullPointerException.class,()->M3NativeLongs.prefixSum(null,0,b,null));
    }
    static void kernelCancel(M3NativeLongs.Backend b)throws Exception {
        Monitor m=new Monitor();
        m.cutoff=1500;
        raises(CancellationException.class,()->M3NativeLongs.prefixSum(new long[10000],0,b,m));
        eq(m.work,2048L);
        Monitor m2=new Monitor();
        m2.fail=true;
        raises(IllegalStateException.class,()->M3NativeLongs.sumByDenseKey(new int[10000],new long[10000],1,b,m2));
        eq(m2.work,1024L);
    }
    static final class Monitor implements M3ProgressMonitor {
        long work,cutoff=Long.MAX_VALUE;
        boolean cancel,fail;
        public void beginTask(String n,long total) {
        }
        public void done() {
        }
        public void worked(long n) {
            work+=n;
            if(fail)throw new IllegalStateException("monitor fault");
            if(work>=cutoff)cancel=true;
        }
        public void setProgress(long n,long total) {
            work=n;
        }
        public void setTaskName(String n) {
        }
        public void subTask(String n) {
        }
        public boolean isCanceled() {
            return cancel;
        }
        public void setCanceled(boolean b) {
            cancel=b;
        }
        public M3ProgressState state() {
            return new M3ProgressState("proof","",work,UNKNOWN,cancel,false,0,0);
        }
    }
}
