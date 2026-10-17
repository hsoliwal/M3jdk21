// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Adversarial, non-differential cases complementing the stable TreeMap oracle corpus. */
public final class M3FilteredSortedMultisetEdges {
    private static int checks;
    private M3FilteredSortedMultisetEdges() { }
    private static void eq(Object a,Object b,String cause) {
        checks++;if(!Objects.equals(a,b))throw new AssertionError(cause+" expected="+a+" actual="+b);
    }
    private static void yes(boolean v,String cause){eq(true,v,cause);}
    private static void fail(Class<? extends Throwable> expected,Runnable work,String cause){
        try{work.run();}catch(Throwable t){yes(expected.isInstance(t),cause+" "+t);return;}
        throw new AssertionError(cause+" no exception");
    }
    private static <T> List<T> copy(Iterable<T> items){var out=new ArrayList<T>();for(T item:items)out.add(item);return out;}
    private static void largeGroupSkip() {
        var space=new M3FilteredSortedMultisetContract.Domain<Integer>();
        var source=new M3IndexSortedMultiset<>(space);
        source.add(1,Integer.MAX_VALUE);source.add(2,2);source.add(3,Integer.MAX_VALUE);
        int[] visibilityCalls={0};
        var visible=M3FilteredSortedMultisets.filtered(source,v->{visibilityCalls[0]++;return v==2;});
        Iterator<Integer> iterator=visible.iterator();
        eq(true,iterator.hasNext(),"fast skip hidden 2^31 group");
        eq(2,iterator.next(),"first permitted occurrence");
        eq(2,iterator.next(),"second permitted occurrence");
        eq(false,iterator.hasNext(),"nothing more without expanding hidden count");
        yes(visibilityCalls[0]<=4,"visibility is per group, not per hidden occurrence");
        eq(2L,visible.longSize(),"selected logical count");
        eq(4294967296L,source.longSize(),"backing huge counts unchanged");
        var membership=Set.of(2);
        eq(true,visible.removeAll(membership),"bulk group removal");
        eq(0,source.count(2),"selected group removed");
        eq(Integer.MAX_VALUE,source.count(1),"first hidden group intact");
        eq(Integer.MAX_VALUE,source.count(3),"last hidden group intact");
        var visible3=M3FilteredSortedMultisets.filtered(source,v->v==3);
        eq(false,visible3.retainAll(Set.of(3)),"retain selected group no mutation");
        eq(Integer.MAX_VALUE,source.count(3),"retain group does not expand");
        eq(true,visible3.retainAll(Set.of()),"retain none removes group in one operation");
        eq(0,source.count(3),"retained group removed");
    }
    private static void descendingAndNull() {
        var space=new M3FilteredSortedMultisetContract.Domain<Integer>();
        Comparator<Integer> comparator=Comparator.nullsLast(Comparator.reverseOrder());
        var source=new M3IndexSortedMultiset<Integer>(space,comparator);
        source.add(null,3);
        for(int i=0;i<=9;i++)source.add(i,i+1);
        Predicate<Integer> predicate=e->e==null||e%3==0;
        var view=M3FilteredSortedMultisets.filtered(source,predicate);
        var expected=new TreeMap<Integer,Integer>(comparator);
        expected.put(9,10);expected.put(6,7);expected.put(3,4);expected.put(0,1);expected.put(null,3);
        List<Integer> expanded=new ArrayList<>();
        for(var e:expected.entrySet())for(int i=0;i<e.getValue();i++)expanded.add(e.getKey());
        eq(expanded,copy(view),"null last descending occurrence order");
        eq(expanded.reversed(),copy(view.descendingMultiset()),"ascending reverse traversal");
        NavigableSet<Integer> distinct=view.elementSet();
        eq(List.of(9,6,3,0),copy(distinct.headSet(null,false)),"descending head excluding null");
        eq(9,distinct.first(),"descending first");eq(null,distinct.last(),"descending null last");
        eq(9,distinct.floor(8),"floor in descending comparator");
        eq(6,distinct.ceiling(8),"ceiling in descending comparator");
        long rank=0;
        for(int n=0;n<=9;n++) {
            int value=n;
            rank=expanded.stream().filter(e->comparator.compare(e,value)<0).count();
            eq(rank,view.rankOf(n,false),"descending rank "+n);
        }
        eq((long)expanded.size(),view.rankOf(null,true),"inclusive final null rank");
        eq(null,view.elementAtOccurrence(expanded.size()-1),"select null last");
        eq(true,view.entrySet().contains(new M3IndexSortedMultiset.Entry<>(null,3)),"null count entry");
        eq(true,view.entrySet().remove(new M3IndexSortedMultiset.Entry<>(null,3)),"null entry removes group");
        eq(0,source.count(null),"null group removed");
        eq(false,view.entrySet().remove(new M3IndexSortedMultiset.Entry<>(9,3)),"mismatched snapshot refused");
        eq(10,source.count(9),"mismatched group retained");
    }
    private static void mutationAndException() {
        var space=new M3FilteredSortedMultisetContract.Domain<Integer>();
        var source=new M3IndexSortedMultiset<Integer>(space);
        source.add(1,3);source.add(2,2);source.add(3,1);
        var view=M3FilteredSortedMultisets.filtered(source,v->v%2==1);
        var cursor=view.iterator();
        yes(cursor.hasNext(),"prepared first group");
        source.add(2,1);
        fail(ConcurrentModificationException.class,cursor::next,"hidden count mutation invalidates prepared iterator");
        cursor=view.iterator();yes(cursor.hasNext(),"new iterator prepared");
        source.setCount(1,4);
        Iterator<Integer> stale=cursor;
        fail(ConcurrentModificationException.class,stale::hasNext,"count mutation invalidates hasNext");
        var readonly=M3FilteredSortedMultisets.readOnlyFiltered(source,v->v%2==1);
        Collection<?> empty=List.of();
        fail(UnsupportedOperationException.class,()->readonly.elementSet().removeAll(empty),"readOnly element empty removeAll");
        fail(UnsupportedOperationException.class,()->readonly.elementSet().retainAll(readonly.elementSet()),"readOnly element retain itself");
        fail(UnsupportedOperationException.class,()->readonly.entrySet().removeAll(empty),"readOnly entry empty removeAll");
        fail(UnsupportedOperationException.class,()->readonly.entrySet().retainAll(readonly.entrySet()),"readOnly entry retain itself");
        fail(UnsupportedOperationException.class,()->readonly.elementSet().pollFirst(),"readOnly poll first");
        fail(UnsupportedOperationException.class,()->readonly.elementSet().pollLast(),"readOnly poll last");
        fail(UnsupportedOperationException.class,()->readonly.filtered(v->true).add(1,0),"readOnly nested empty add");
        int[] calls={0};
        fail(IllegalStateException.class,()->view.removeIf(v->{
            if(++calls[0]==3)throw new IllegalStateException("intentional callback abort");
            return v==1;
        }),"callback throws and leaves completed prefix");
        eq(2,source.count(1),"one occurrence removed before abort");
        // A throwing visibility predicate is terminal for this iterator, never silent exhaustion.
        var throwing=M3FilteredSortedMultisets.filtered(source,v->{throw new IllegalArgumentException("visibility");});
        var filteredIterator=throwing.iterator();
        fail(IllegalArgumentException.class,filteredIterator::hasNext,"visibility failure visible");
        fail(IllegalStateException.class,filteredIterator::hasNext,"terminal failure after callback");
        // Range source mutation after entry snapshot must not turn into a writable live Entry.
        var snapshot=view.entrySet().iterator().next();
        source.setCount(snapshot.element(),3);
        eq(true,snapshot.count()!=source.count(snapshot.element()),"entry snapshot count detached");
    }
    public static void main(String[] args){largeGroupSkip();descendingAndNull();mutationAndException();
        System.out.println("PASS M3FilteredSortedMultisetEdges checks="+checks);}
}
