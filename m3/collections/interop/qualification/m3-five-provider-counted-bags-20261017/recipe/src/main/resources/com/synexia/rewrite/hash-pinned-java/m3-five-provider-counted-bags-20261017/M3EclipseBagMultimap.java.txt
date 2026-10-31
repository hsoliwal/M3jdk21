/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;
import com.m3.collections.M3LinkedHashMap;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.multimap.bag.AbstractMutableBagMultimap;

/** Eclipse BagMultimap: M3 key slots and a primitive-counted M3 bag per key. */
public final class M3EclipseBagMultimap<K,V> extends AbstractMutableBagMultimap<K,V> {
    public M3EclipseBagMultimap(){super();}
    @Override protected MutableMap<K, MutableBag<V>> createMap(){return MapAdapter.adapt(new M3LinkedHashMap<>());}
    @Override protected MutableMap<K, MutableBag<V>> createMapWithKeyCount(int keyCount){if(keyCount<0)throw new IllegalArgumentException();return createMap();}
    @Override protected MutableBag<V> createCollection(){return new M3EclipseBag<>();}
    @Override public M3EclipseBagMultimap<K,V> newEmpty(){return new M3EclipseBagMultimap<>();}
    @Override public MutableBagMultimap<V,K> flip(){M3EclipseBagMultimap<V,K> result=new M3EclipseBagMultimap<>();this.forEachKeyValue((k,v)->result.put(v,k));return result;}
    @Override public M3EclipseBagMultimap<K,V> selectKeysValues(Predicate2<? super K,? super V> predicate){M3EclipseBagMultimap<K,V> result=newEmpty();this.forEachKeyValue((k,v)->{if(predicate.accept(k,v))result.put(k,v);});return result;}
    @Override public M3EclipseBagMultimap<K,V> rejectKeysValues(Predicate2<? super K,? super V> predicate){M3EclipseBagMultimap<K,V> result=newEmpty();this.forEachKeyValue((k,v)->{if(!predicate.accept(k,v))result.put(k,v);});return result;}
    @Override public M3EclipseBagMultimap<K,V> selectKeysMultiValues(Predicate2<? super K,? super RichIterable<V>> predicate){M3EclipseBagMultimap<K,V> result=newEmpty();this.forEachKeyMutableBag((k,bag)->{if(predicate.accept(k,bag))result.putAll(k,bag);});return result;}
    @Override public M3EclipseBagMultimap<K,V> rejectKeysMultiValues(Predicate2<? super K,? super RichIterable<V>> predicate){M3EclipseBagMultimap<K,V> result=newEmpty();this.forEachKeyMutableBag((k,bag)->{if(!predicate.accept(k,bag))result.putAll(k,bag);});return result;}
    @Override public <V2> M3EclipseBagMultimap<K,V2> collectValues(Function<? super V,? extends V2> transform){M3EclipseBagMultimap<K,V2> result=new M3EclipseBagMultimap<>();this.forEachKeyValue((k,v)->result.put(k,transform.valueOf(v)));return result;}
}
