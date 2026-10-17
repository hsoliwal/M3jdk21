// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Reusable finite owner-backed plan. No user callback executes until a terminal starts.
 * Owner reads/keys must remain stable during each terminal. Version checks detect reported
 * changes; they are not locking, transactional snapshots, or automatic thread safety. */
public final class M3Flow<T> {
    interface Sink<T> {
        boolean accept(int index,T value);
    }
    interface Gate<T> {
        default void configureBudget(int maximum) {
        }
        default boolean valueNeeded() {
            return false;
        }
        default boolean stopped() {
            return false;
        }
        int test(int i,T value);
        // 1 pass, 0 skip, -1 terminate
    }
    static final class Source<T> {
        final IntSupplier size;
        final IntFunction<? extends T> read;
        final LongSupplier version;
        Source(IntSupplier size,IntFunction<? extends T> read,LongSupplier version) {
            this.size=Objects.requireNonNull(size);
            this.read=Objects.requireNonNull(read);
            this.version=Objects.requireNonNull(version);
        }
        void guard(long stamp) {
            if(version.getAsLong()!=stamp) throw new ConcurrentModificationException("source version changed");
        }
    }
    record Snapshot(int size,long version) {
    }
    final Source<T> source;
    final int maxRows,maxGroups;
    private final List<Supplier<Gate<T>>> gates;
    private M3Flow(Source<T> source,List<Supplier<Gate<T>>> gates,int rows,int groups) {
        this.source=source;
        this.gates=gates;
        maxRows=rows;
        maxGroups=groups;
    }
    public static <T> M3Flow<T> indexed(IntSupplier size,IntFunction<? extends T> read) {
        return indexed(size,read,()->0L);
    }
    public static <T> M3Flow<T> indexed(IntSupplier size,IntFunction<? extends T> read,LongSupplier version) {
        return new M3Flow<>(new Source<>(size,read,version),List.of(),Integer.MAX_VALUE-8,Integer.MAX_VALUE-8);
    }
    public M3Flow<T> budget(int rows,int groups) {
        if(rows<0||groups<0) throw new IllegalArgumentException("negative budget");
        return new M3Flow<>(source,gates,rows,groups);
    }
    private M3Flow<T> add(Supplier<Gate<T>> gate) {
        var next=new ArrayList<>(gates);
        next.add(gate);
        return new M3Flow<>(source,List.copyOf(next),maxRows,maxGroups);
    }
    public M3Flow<T> whereIndex(IntPredicate p) {
        Objects.requireNonNull(p);
        return add(()->new Gate<>() {
            public int test(int i,T v) {
                return p.test(i)?1:0;
            }
        }
        );
    }
    public M3Flow<T> filter(Predicate<? super T> p) {
        Objects.requireNonNull(p);
        return add(()->new Gate<>() {
            public boolean valueNeeded() {
                return true;
            }
            public int test(int i,T v) {
                return p.test(v)?1:0;
            }
        }
        );
    }
    public M3Flow<T> limit(long limit) {
        if(limit<0)throw new IllegalArgumentException("negative limit");
        return add(()->new Gate<>() {
            long left=limit;
            public boolean stopped() {
                return left==0;
            }
            public int test(int i,T v) {
                left--;
                return 1;
            }
        }
        );
    }
    public M3Flow<T> skip(long count) {
        if(count<0)throw new IllegalArgumentException("negative skip");
        return add(()->new Gate<>() {
            long left=count;
            public int test(int i,T v) {
                if(left>0) {
                    left--;
                    return 0;
                }
                return 1;
            }
        }
        );
    }
    public M3Flow<T> takeWhile(Predicate<? super T> p) {
        Objects.requireNonNull(p);
        return add(()->new Gate<>() {
            public boolean valueNeeded() {
                return true;
            }
            public int test(int i,T v) {
                return p.test(v)?1:-1;
            }
        }
        );
    }
    public M3Flow<T> dropWhile(Predicate<? super T> p) {
        Objects.requireNonNull(p);
        return add(()->new Gate<>() {
            boolean dropping=true;
            public boolean valueNeeded() {
                return dropping;
            }
            public int test(int i,T v) {
                if(!dropping)return 1;
                if(p.test(v))return 0;
                dropping=false;
                return 1;
            }
        }
        );
    }
    public <K> M3Flow<T> distinctBy(Function<? super T,? extends K> key) {
        Objects.requireNonNull(key);
        return distinct(true,(i,v)->key.apply(v));
    }
    public <K> M3Flow<T> distinctByIndex(IntFunction<? extends K> key) {
        Objects.requireNonNull(key);
        return distinct(false,(i,v)->key.apply(i));
    }
    private <K> M3Flow<T> distinct(boolean needs,M3KeyedFlow.Key<T,K> key) {
        return add(()->new Gate<>() {
            final Set<K> seen=new HashSet<>();
            private int maximum;
            @Override public void configureBudget(int maximum) {
                this.maximum=maximum;
            }
            public boolean valueNeeded() {
                return needs;
            }
            public int test(int i,T v) {
                K k=key.get(i,v);
                if(seen.contains(k))return 0;
                if(seen.size()==maximum)throw new IllegalArgumentException("distinct budget");
                seen.add(k);
                return 1;
            }
        }
        );
    }
    public <K> M3KeyedFlow<T,K> groupBy(Function<? super T,? extends K> key) {
        Objects.requireNonNull(key);
        return new M3KeyedFlow<>(this,true,(i,v)->key.apply(v));
    }
    public <K> M3KeyedFlow<T,K> groupByIndex(IntFunction<? extends K> key) {
        Objects.requireNonNull(key);
        return new M3KeyedFlow<>(this,false,(i,v)->key.apply(i));
    }
    public <K> M3KeyedFlow<T,K> partitionBy(Function<? super T,? extends K> key) {
        return groupBy(key);
    }
    public <K> M3KeyedFlow<T,K> partitionByIndex(IntFunction<? extends K> key) {
        return groupByIndex(key);
    }
    public M3LongFlow mapToLong(ToLongFunction<? super T> mapper) {
        Objects.requireNonNull(mapper);
        return new M3LongFlow((sink,m)->walk(true,(i,v)->sink.accept(mapper.applyAsLong(v)),m),maxRows);
    }
    public M3LongFlow mapIndexToLong(IntToLongFunction mapper) {
        Objects.requireNonNull(mapper);
        return new M3LongFlow((sink,m)->walk(false,(i,v)->sink.accept(mapper.applyAsLong(i)),m),maxRows);
    }
    public <K> M3Runs<K> runsBy(Function<? super T,? extends K> key) {
        Objects.requireNonNull(key);
        return M3Runs.of(this,true,(i,v)->key.apply(v));
    }
    public <K> M3Runs<K> runsByIndex(IntFunction<? extends K> key) {
        Objects.requireNonNull(key);
        return M3Runs.of(this,false,(i,v)->key.apply(i));
    }
    public M3Windows<T> windowed(int width,int step,M3Windows.Tail tail) {
        return new M3Windows<>(this,width,step,tail);
    }
    public M3TopK<T> topKByIndex(int k,IntBinaryOperator order) {
        return new M3TopK<>(this,k,order);
    }
    public long count() {
        return count(null);
    }
    public long count(M3ProgressMonitor m) {
        long[] n= {
            0
        }
        ;
        walk(false,(i,v)-> {
            n[0]++;
            return true;
        }
        ,m);
        return n[0];
    }
    public void forEach(Consumer<? super T> action) {
        forEach(action,null);
    }
    public void forEach(Consumer<? super T> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        walk(true,(i,v)-> {
            action.accept(v);
            return true;
        }
        ,m);
    }
    public void forEachIndex(IntConsumer action) {
        forEachIndex(action,null);
    }
    public void forEachIndex(IntConsumer action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        walk(false,(i,v)-> {
            action.accept(i);
            return true;
        }
        ,m);
    }
    public boolean anyMatch(Predicate<? super T> p) {
        return anyMatch(p,null);
    }
    public boolean anyMatch(Predicate<? super T> p,M3ProgressMonitor m) {
        Objects.requireNonNull(p);
        boolean[] yes= {
            false
        }
        ;
        walk(true,(i,v)->!(yes[0]=p.test(v)),m);
        return yes[0];
    }
    public boolean allMatch(Predicate<? super T> p) {
        return allMatch(p,null);
    }
    public boolean allMatch(Predicate<? super T> p,M3ProgressMonitor m) {
        Objects.requireNonNull(p);
        boolean[] yes= {
            true
        }
        ;
        walk(true,(i,v)->yes[0]=p.test(v),m);
        return yes[0];
    }
    public List<T> toList() {
        return toList(null);
    }
    public List<T> toList(M3ProgressMonitor m) {
        var list=new ArrayList<T>();
        forEach(list::add,m);
        return Collections.unmodifiableList(list);
    }
    public M3IndexView<T> virtualize() {
        return virtualize(null);
    }
    public M3IndexView<T> virtualize(M3ProgressMonitor m) {
        PrimitiveIndices a=new PrimitiveIndices(maxRows);
        Snapshot s=walk(false,(i,v)-> {
            a.add(i);
            return true;
        }
        ,m);
        return new M3IndexView<>(source,s.version(),a.array());
    }
    public <R> R withView(Function<? super M3IndexView<T>,? extends R> action) {
        return withView(action,null);
    }
    public <R> R withView(Function<? super M3IndexView<T>,? extends R> action,M3ProgressMonitor m) {
        return M3TemporaryCollectors.withView(this,action,m);
    }
    Snapshot snapshot(M3ProgressMonitor m) {
        check(m);
        long v=source.version.getAsLong();
        int n=source.size.getAsInt();
        source.guard(v);
        if(n<0||n>maxRows)throw new IllegalArgumentException("row budget exceeded");
        return new Snapshot(n,v);
    }
    Snapshot walk(boolean valueNeeded,Sink<T> sink,M3ProgressMonitor m) {
        return walk(snapshot(m),valueNeeded,sink,m);
    }
    Snapshot walk(Snapshot snap,boolean valueNeeded,Sink<T> sink,M3ProgressMonitor m) {
        List<Gate<T>> active=gates.stream().map(Supplier::get).toList();
        for(Gate<T> gate:active)gate.configureBudget(maxGroups);
        outer:for(int i=0;i<snap.size();i++) {
            check(m);
            source.guard(snap.version());
            for(Gate<T> gate:active)if(gate.stopped())break outer;
            T value=null;
            boolean loaded=false,keep=true;
            for(Gate<T> gate:active) {
                if(gate.valueNeeded()&&!loaded) {
                    value=source.read.apply(i);
                    loaded=true;
                    source.guard(snap.version());
                    check(m);
                }
                int state=gate.test(i,value);
                source.guard(snap.version());
                check(m);
                if(state<0)break outer;
                if(state==0) {
                    keep=false;
                    break;
                }
            }
            if(keep) {
                if(valueNeeded&&!loaded) {
                    value=source.read.apply(i);
                    source.guard(snap.version());
                    check(m);
                }
                boolean more=sink.accept(i,value);
                source.guard(snap.version());
                worked(m);
                if(!more)break;
            }
            else worked(m);
        }
        check(m);
        source.guard(snap.version());
        return snap;
    }
    static void check(M3ProgressMonitor m) {
        if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("interrupted");
        if(m!=null)synchronized(m) {
            m.checkCanceled();
        }
    }
    static void worked(M3ProgressMonitor m) {
        if(m!=null)synchronized(m) {
            m.worked(1);
        }
    }
}
