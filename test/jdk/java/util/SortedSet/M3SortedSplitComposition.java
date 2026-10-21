/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Preserve sorted comparator, lazy callbacks and occurrence order across recursive splits.
 * @run main M3SortedSplitComposition
 */
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/** Original synthetic contract oracle invoking the actual java.base SortedSet default. */
public final class M3SortedSplitComposition {
    private static long checks;
    private M3SortedSplitComposition() { }
    private static final class Probe extends AbstractSet<Integer> implements SortedSet<Integer> {
        final SortedSet<Integer> data;
        final AtomicInteger comparatorCalls = new AtomicInteger();
        boolean comparatorFails;
        Probe(SortedSet<Integer> data) { this.data = data; }
        @Override public Iterator<Integer> iterator() { return data.iterator(); }
        @Override public int size() { return data.size(); }
        @Override public boolean add(Integer value) { return data.add(value); }
        @Override public boolean remove(Object value) { return data.remove(value); }
        @Override public Comparator<? super Integer> comparator() {
            comparatorCalls.incrementAndGet();
            if (comparatorFails) throw new IllegalStateException("synthetic comparator");
            return data.comparator();
        }
        @Override public SortedSet<Integer> subSet(Integer a,Integer b) { return new Probe(data.subSet(a,b)); }
        @Override public SortedSet<Integer> headSet(Integer b) { return new Probe(data.headSet(b)); }
        @Override public SortedSet<Integer> tailSet(Integer a) { return new Probe(data.tailSet(a)); }
        @Override public Integer first() { return data.first(); }
        @Override public Integer last() { return data.last(); }
        @Override public Spliterator<Integer> spliterator() { return SortedSet.super.spliterator(); }
    }
    public static void main(String[] args) {
        // Frozen minimal baseline counterexample: a split of reverse order reports natural order.
        Probe witness = new Probe(new TreeSet<>(Comparator.reverseOrder()));
        witness.addAll(List.of(1,2,3,4));
        Spliterator<Integer> child = witness.spliterator().trySplit();
        check(child != null && child.getComparator() == witness.data.comparator(), "split lost owner comparator");
        List<Comparator<Integer>> orders = Arrays.asList(null, Comparator.naturalOrder(),
                Comparator.reverseOrder(), Comparator.nullsFirst(Comparator.naturalOrder()),
                Comparator.nullsLast(Comparator.reverseOrder()));
        for (Comparator<Integer> order : orders) {
            for (int n : new int[]{0,1,2,3,7,63,255,256,257,1023,1024,1025,4097}) {
                TreeSet<Integer> data = new TreeSet<>(order);
                for(int i=n-1;i>=0;i--) data.add(i);
                if (order != null && (orders.indexOf(order)>=3)) data.add(null);
                Probe set = new Probe(data);
                List<Integer> expected = new ArrayList<>(data);
                ArrayList<Integer> actual = new ArrayList<>();
                Spliterator<Integer> split = set.spliterator();
                check(set.comparatorCalls.get()==0, "eager comparator at construction");
                recurse(split, data.comparator(), actual, set);
                check(actual.equals(expected), "recursive encounter order / duplicates");
                check(StreamSupport.stream(set.spliterator(),true).toList().equals(expected), "parallel encounter order");
                List<Integer> reverse = new ArrayList<>(expected); java.util.Collections.reverse(reverse);
                check(new ArrayList<>(set.reversed()).equals(reverse), "reversed view order");
                // No hidden element replacement. All references returned by iteration survive.
                for(int i=0;i<expected.size();i++) check(expected.get(i)==actual.get(i), "reference identity");
            }
        }
        lifecycle();
        System.out.println("SORTED_SPLIT_COMPOSITION checks="+checks+" PASS");
    }
    private static void recurse(Spliterator<Integer> spl, Comparator<? super Integer> cmp,
                                List<Integer> out, Probe owner) {
        check(spl.hasCharacteristics(Spliterator.SORTED|Spliterator.ORDERED|Spliterator.DISTINCT), "characteristics");
        check(spl.getComparator()==cmp, "recursive comparator identity");
        long before=spl.estimateSize();
        int calls=owner.comparatorCalls.get();
        Spliterator<Integer> prefix=spl.trySplit();
        check(owner.comparatorCalls.get()==calls,"splitting invoked comparator");
        if(prefix==null) {
            spl.forEachRemaining(out::add);
            check(owner.comparatorCalls.get()==calls,"traversal invoked comparator");
            return;
        }
        check(prefix.estimateSize()+spl.estimateSize()==before,"split sizes");
        recurse(prefix,cmp,out,owner);
        recurse(spl,cmp,out,owner);
    }
    private static void lifecycle() {
        Probe set=new Probe(new TreeSet<>(Comparator.reverseOrder()));set.addAll(List.of(1,2,3));
        Spliterator<Integer> lazy=set.spliterator();set.add(4);
        ArrayList<Integer> got=new ArrayList<>();lazy.forEachRemaining(got::add);
        check(got.equals(List.of(4,3,2,1)),"late binding lost");
        Spliterator<Integer> bound=set.spliterator();bound.estimateSize();set.add(5);
        refuses(ConcurrentModificationException.class,()->bound.tryAdvance(x->{}));
        set.comparatorFails=true;
        Spliterator<Integer> a=set.spliterator(), b=a.trySplit();
        check(b!=null,"batch absent");
        refuses(IllegalStateException.class,b::getComparator);
        ArrayList<Integer> list=new ArrayList<>();b.forEachRemaining(list::add);
        check(list.equals(List.of(5,4,3,2,1)),"traversal coupled to comparator");
        set.comparatorFails=false;
        Spliterator<Integer> c=set.spliterator().trySplit();
        refuses(NullPointerException.class,()->c.tryAdvance(null));
        refuses(NullPointerException.class,()->c.forEachRemaining(null));
        RuntimeException sentinel=new RuntimeException("consumer");
        try { c.tryAdvance(x->{throw sentinel;}); throw new AssertionError("consumer error swallowed"); }
        catch(RuntimeException thrown){check(thrown==sentinel,"consumer error changed");}
        SortedSet<Integer> view=set.headSet(3);
        check(new ArrayList<>(view).equals(List.of(5,4)),"bounded reverse view");
        set.add(6);check(view.contains(6),"backing not live");
        view.remove(6);check(!set.contains(6),"remove not backed");
        refuses(UnsupportedOperationException.class,()->set.addFirst(7));
        refuses(UnsupportedOperationException.class,()->set.addLast(7));
    }
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static void refuses(Class<? extends Throwable> type,Runnable action){
        checks++;try{action.run();}catch(Throwable t){if(type.isInstance(t))return;throw new AssertionError(t);}
        throw new AssertionError("did not refuse "+type);
    }
}
