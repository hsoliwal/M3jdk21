/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import java.util.*;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.impl.bag.mutable.AbstractMutableBag;
import org.eclipse.collections.impl.map.mutable.MapAdapter;

/** Counted bag backed by M3 parallel slot/index map, without stored occurrence nodes. */
public final class M3EclipseBag<E> extends AbstractMutableBag<E> {
    private final M3LinkedHashMap<E,Integer> counts = new M3LinkedHashMap<>();
    private int size;
    @Override public boolean equals(Object other){if(other==this)return true;if(!(other instanceof org.eclipse.collections.api.bag.Bag<?> b) || sizeDistinct()!=b.sizeDistinct())return false;for(var e:counts.entrySet())if(e.getValue()!=b.occurrencesOf(e.getKey()))return false;return true;}
    @Override public int hashCode(){int result=0;for(var e:counts.entrySet())result+=Objects.hashCode(e.getKey())^e.getValue();return result;}
    @Override protected RichIterable<E> getKeysView() {return MapAdapter.adapt(counts).keysView();}
    @Override public int size(){return size;}
    @Override public int sizeDistinct(){return counts.size();}
    @Override public boolean isEmpty(){return size==0;}
    @Override public boolean contains(Object e){return occurrencesOf(e)>0;}
    @Override public int occurrencesOf(Object e){return counts.getOrDefault(e,0);}
    @Override public int addOccurrences(E e,int n) {
        if(n<0) throw new IllegalArgumentException("negative occurrences");
        int old=occurrencesOf(e); if(n==0)return old;
        int next=Math.addExact(old,n); int total=Math.addExact(size,n);
        counts.put(e,next);size=total;return next;
    }
    @Override public boolean removeOccurrences(Object e,int n) {
        if(n<0)throw new IllegalArgumentException("negative occurrences");
        int old=occurrencesOf(e); if(old==0 || n==0)return false;
        int taken=Math.min(n,old); if(taken==old)counts.remove(e);else counts.put(cast(e),old-taken);
        size-=taken;return true;
    }
    @SuppressWarnings("unchecked") private E cast(Object e){return (E)e;}
    @Override public boolean setOccurrences(E e,int n) {
        if(n<0) throw new IllegalArgumentException("negative occurrences");
        int old=occurrencesOf(e);if(old==n)return false;
        int next=Math.addExact(size,n-old);
        if(n==0)counts.remove(e);else counts.put(e,n);size=next;return true;
    }
    @Override public boolean add(E e){addOccurrences(e,1);return true;}
    @Override public boolean remove(Object e){return removeOccurrences(e,1);}
    @Override public void clear(){counts.clear();size=0;}
    @Override public void forEachWithOccurrences(ObjectIntProcedure<? super E> proc) {
        Objects.requireNonNull(proc);for(var e:counts.entrySet()) proc.value(e.getKey(),e.getValue());
    }
    @Override public E detectWithOccurrences(ObjectIntPredicate<? super E> pred) {
        Objects.requireNonNull(pred);for(var e:counts.entrySet())if(pred.accept(e.getKey(),e.getValue()))return e.getKey();return null;
    }
    @Override public boolean anySatisfyWithOccurrences(ObjectIntPredicate<? super E> pred){for(var e:counts.entrySet())if(pred.accept(e.getKey(),e.getValue()))return true;return false;}
    @Override public boolean allSatisfyWithOccurrences(ObjectIntPredicate<? super E> pred){for(var e:counts.entrySet())if(!pred.accept(e.getKey(),e.getValue()))return false;return true;}
    @Override public boolean noneSatisfyWithOccurrences(ObjectIntPredicate<? super E> pred){return !anySatisfyWithOccurrences(pred);}
    @Override public boolean removeAllIterable(Iterable<?> values){boolean changed=false;for(Object v:values)changed|=removeOccurrences(v,Integer.MAX_VALUE);return changed;}
    @Override public boolean removeIf(org.eclipse.collections.api.block.predicate.Predicate<? super E> predicate){var doomed=new ArrayList<E>();for(var e:counts.keySet())if(predicate.accept(e))doomed.add(e);return removeAllIterable(doomed);}
    @Override public <P> boolean removeIfWith(org.eclipse.collections.api.block.predicate.Predicate2<? super E,? super P> predicate,P parameter){var doomed=new ArrayList<E>();for(var e:counts.keySet())if(predicate.accept(e,parameter))doomed.add(e);return removeAllIterable(doomed);}
    @Override public <V> org.eclipse.collections.api.multimap.bag.MutableBagMultimap<V,E> groupBy(org.eclipse.collections.api.block.function.Function<? super E,? extends V> mapping){var result=org.eclipse.collections.impl.multimap.bag.HashBagMultimap.<V,E>newMultimap();for(E e:this)result.put(mapping.valueOf(e),e);return result;}
    @Override public <V> org.eclipse.collections.api.multimap.bag.MutableBagMultimap<V,E> groupByEach(org.eclipse.collections.api.block.function.Function<? super E,? extends Iterable<V>> mapping){var result=org.eclipse.collections.impl.multimap.bag.HashBagMultimap.<V,E>newMultimap();for(E e:this)for(V v:mapping.valueOf(e))result.put(v,e);return result;}
    @Override public MutableBag<E> selectByOccurrences(org.eclipse.collections.api.block.predicate.primitive.IntPredicate predicate){M3EclipseBag<E> result=new M3EclipseBag<>();for(var e:counts.entrySet())if(predicate.accept(e.getValue()))result.addOccurrences(e.getKey(),e.getValue());return result;}
    @Override public org.eclipse.collections.api.map.MutableMap<E,Integer> toMapOfItemToCount(){M3LinkedHashMap<E,Integer> copy=new M3LinkedHashMap<>();copy.putAll(counts);return MapAdapter.adapt(copy);}
    @Override public void each(Procedure<? super E> proc){Objects.requireNonNull(proc);for(E e:this)proc.value(e);}
    @Override public Iterator<E> iterator(){
        // Iteration uses per-cursor occurrence coordinates, never retained per-value entries.
        Iterator<E> keys=new ArrayList<>(counts.keySet()).iterator();
        return new Iterator<>(){
            private E current,last;
            private int remaining;
            private boolean canRemove;
            @Override public boolean hasNext(){return remaining>0||keys.hasNext();}
            @Override public E next(){
                if(remaining==0){if(!keys.hasNext())throw new NoSuchElementException();current=keys.next();remaining=occurrencesOf(current);}
                remaining--;last=current;canRemove=true;return current;
            }
            @Override public void remove(){
                if(!canRemove)throw new IllegalStateException();
                M3EclipseBag.this.remove(last);canRemove=false;
            }
        };
    }
    @Override public MutableBag<E> newEmpty(){return new M3EclipseBag<>();}
}
