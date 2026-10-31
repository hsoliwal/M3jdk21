/*
 * Copyright (c) 2021 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.multimap.sortedset;

import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.Function2;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.api.multimap.bag.ImmutableBagMultimap;
import jdk.m3.collections.eclipse.api.multimap.list.ImmutableListMultimap;
import jdk.m3.collections.eclipse.api.multimap.set.ImmutableSetIterableMultimap;
import jdk.m3.collections.eclipse.api.multimap.set.ImmutableSetMultimap;
import jdk.m3.collections.eclipse.api.set.sorted.ImmutableSortedSet;
import jdk.m3.collections.eclipse.api.tuple.Pair;

/**
 * @since 1.0
 */
public interface ImmutableSortedSetMultimap<K, V>
        extends SortedSetMultimap<K, V>, ImmutableSetIterableMultimap<K, V>
{
    @Override
    ImmutableSortedSet<V> get(K key);

    @Override
    ImmutableSortedSetMultimap<K, V> newEmpty();

    @Override
    ImmutableSortedSetMultimap<K, V> newWith(K key, V value);

    @Override
    ImmutableSortedSetMultimap<K, V> newWithout(Object key, Object value);

    @Override
    ImmutableSortedSetMultimap<K, V> newWithAll(K key, Iterable<? extends V> values);

    @Override
    ImmutableSortedSetMultimap<K, V> newWithoutAll(Object key);

    @Override
    ImmutableSetMultimap<V, K> flip();

    @Override
    ImmutableSortedSetMultimap<K, V> selectKeysValues(Predicate2<? super K, ? super V> predicate);

    @Override
    ImmutableSortedSetMultimap<K, V> rejectKeysValues(Predicate2<? super K, ? super V> predicate);

    @Override
    ImmutableSortedSetMultimap<K, V> selectKeysMultiValues(Predicate2<? super K, ? super RichIterable<V>> predicate);

    @Override
    ImmutableSortedSetMultimap<K, V> rejectKeysMultiValues(Predicate2<? super K, ? super RichIterable<V>> predicate);

    @Override
    <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(Function2<? super K, ? super V, Pair<K2, V2>> function);

    @Override
    <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(Function<? super K, ? extends K2> keyFunction, Function<? super V, ? extends V2> valueFunction);

    @Override
    <V2> ImmutableListMultimap<K, V2> collectValues(Function<? super V, ? extends V2> function);
}
