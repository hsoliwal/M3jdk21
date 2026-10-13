/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.immutable.primitive;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.ImmutableBooleanBag;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;
import org.eclipse.collections.api.bag.primitive.ImmutableCharBag;
import org.eclipse.collections.api.bag.primitive.ImmutableDoubleBag;
import org.eclipse.collections.api.bag.primitive.ImmutableFloatBag;
import org.eclipse.collections.api.bag.primitive.ImmutableIntBag;
import org.eclipse.collections.api.bag.primitive.ImmutableLongBag;
import org.eclipse.collections.api.bag.primitive.ImmutableShortBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.impl.bag.sorted.mutable.TreeBag;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.BooleanFunction;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.function.primitive.CharFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleFunction;
import org.eclipse.collections.api.block.function.primitive.DoubleObjectToDoubleFunction;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatObjectToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.function.primitive.LongFunction;
import org.eclipse.collections.api.block.function.primitive.LongObjectToLongFunction;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.api.map.primitive.LongObjectMap;
import org.eclipse.collections.api.map.primitive.ImmutableLongObjectMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBag;
import org.eclipse.collections.api.partition.bag.PartitionMutableBag;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.UnmodifiableIteratorAdapter;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.block.procedure.PartitionProcedure;
import org.eclipse.collections.impl.block.procedure.PartitionPredicate2Procedure;
import org.eclipse.collections.impl.factory.Bags;
import org.eclipse.collections.impl.factory.BiMaps;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.SortedMaps;
import org.eclipse.collections.impl.factory.SortedSets;
import org.eclipse.collections.impl.factory.primitive.BooleanBags;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.factory.primitive.DoubleBags;
import org.eclipse.collections.impl.factory.primitive.FloatBags;
import org.eclipse.collections.impl.factory.primitive.IntBags;
import org.eclipse.collections.impl.factory.primitive.LongBags;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.eclipse.collections.impl.lazy.LazyIterableAdapter;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.multimap.bag.HashBagMultimap;
import org.eclipse.collections.impl.partition.bag.PartitionHashBag;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableLongSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;

/**
 * ImmutableLongObjectSingletonMap is an optimization for {@link ImmutableLongObjectMap} of size 1.
 * This file was automatically generated from template file immutablePrimitiveObjectSingletonMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongObjectSingletonMap<V> extends AbstractImmutableLongObjectMap<V> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final long key1;
    private final V value1;

    ImmutableLongObjectSingletonMap(long key1, V value1)
    {
        this.key1 = key1;
        this.value1 = value1;
    }

    @Override
    public V get(long key)
    {
        return this.key1 == key ? this.value1 : null;
    }

    @Override
    public V getIfAbsent(long key, Function0<? extends V> ifAbsent)
    {
        return this.key1 == key ? this.value1 : ifAbsent.value();
    }

    @Override
    public boolean containsKey(long key)
    {
        return this.key1 == key;
    }

    @Override
    public boolean containsValue(Object value)
    {
        return nullSafeEquals(this.value1, value);
    }

    private static boolean nullSafeEquals(Object value, Object other)
    {
        if (value == null)
        {
            if (other == null)
            {
                return true;
            }
        }
        else if (other == value || value.equals(other))
        {
            return true;
        }
        return false;
    }

    @Override
    public ImmutableLongObjectMap<V> tap(Procedure<? super V> procedure)
    {
        procedure.value(this.value1);
        return this;
    }

    @Override
    public void forEachValue(Procedure<? super V> procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
        procedure.value(this.key1);
    }

    @Override
    public void forEachKeyValue(LongObjectProcedure<? super V> procedure)
    {
        procedure.value(this.key1, this.value1);
    }

    @Override
    public ImmutableLongObjectMap<V> select(LongObjectPredicate<? super V> predicate)
    {
        return predicate.accept(this.key1, this.value1) ? LongObjectHashMap.newWithKeysValues(this.key1, this.value1).toImmutable()
                : (ImmutableLongObjectMap<V>) ImmutableLongObjectEmptyMap.INSTANCE;
    }

    @Override
    public ImmutableLongObjectMap<V> reject(LongObjectPredicate<? super V> predicate)
    {
        return predicate.accept(this.key1, this.value1) ? (ImmutableLongObjectMap<V>) ImmutableLongObjectEmptyMap.INSTANCE
                : LongObjectHashMap.newWithKeysValues(this.key1, this.value1).toImmutable();
    }

    @Override
    public ImmutableLongObjectMap<V> toImmutable()
    {
        return this;
    }

    @Override
    public int size()
    {
        return 1;
    }

    @Override
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(LongHashSet.newSetWith(this.key1));
    }

    @Override
    public Collection<V> values()
    {
        return Lists.immutable.of(this.value1).castToList();
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LongLists.immutable.of(this.key1).asLazy();
    }

    @Override
    public RichIterable<LongObjectPair<V>> keyValuesView()
    {
        return Lists.immutable.of(PrimitiveTuples.pair(this.key1, this.value1)).asLazy();
    }

    @Override
    public ImmutableObjectLongMap<V> flipUniqueValues()
    {
        return ObjectLongMaps.immutable.with(this.value1, this.key1);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof LongObjectMap))
        {
            return false;
        }
        LongObjectMap<V> map = (LongObjectMap<V>) obj;
        if (map.size() != 1)
        {
            return false;
        }
        return map.containsKey(this.key1) && nullSafeEquals(this.value1, map.get(this.key1));
    }

    @Override
    public int hashCode()
    {
        return (int) (this.key1 ^ this.key1 >>> 32) ^ (this.value1 == null ? 0 : this.value1.hashCode());
    }

    @Override
    public String toString()
    {
        return "{" + this.key1 + "=" + this.value1 + "}";
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public boolean notEmpty()
    {
        return true;
    }

    @Override
    public V getFirst()
    {
        return this.value1;
    }

    @Override
    public V getLast()
    {
        return this.value1;
    }

    @Override
    public V getOnly()
    {
        return this.value1;
    }

    @Override
    public boolean contains(Object object)
    {
        return this.containsValue(object);
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        for (Iterator<?> iterator = source.iterator(); iterator.hasNext(); )
        {
            if (!nullSafeEquals(this.value1, iterator.next()))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        return this.containsAllIterable(source);
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        for (Object item : elements)
        {
            if (!this.contains(item))
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public ImmutableBag<V> select(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1) ? Bags.immutable.with(this.value1) : Bags.immutable.<V>with();
    }

    @Override
    public <R extends Collection<V>> R select(Predicate<? super V> predicate, R target)
    {
        if (predicate.accept(this.value1))
        {
            target.add(this.value1);
        }
        return target;
    }

    @Override
    public <P> ImmutableBag<V> selectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter) ? Bags.immutable.with(this.value1) : Bags.immutable.<V>with();
    }

    @Override
    public <P, R extends Collection<V>> R selectWith(Predicate2<? super V, ? super P> predicate, P parameter, R targetCollection)
    {
        if (predicate.accept(this.value1, parameter))
        {
            targetCollection.add(this.value1);
        }
        return targetCollection;
    }

    @Override
    public ImmutableBag<V> reject(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1) ? Bags.immutable.<V>with() : Bags.immutable.with(this.value1);
    }

    @Override
    public <R extends Collection<V>> R reject(Predicate<? super V> predicate, R target)
    {
        if (!predicate.accept(this.value1))
        {
            target.add(this.value1);
        }
        return target;
    }

    @Override
    public <P> ImmutableBag<V> rejectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter) ? Bags.immutable.<V>with() : Bags.immutable.with(this.value1);
    }

    @Override
    public <P, R extends Collection<V>> R rejectWith(Predicate2<? super V, ? super P> predicate, P parameter, R targetCollection)
    {
        if (!predicate.accept(this.value1, parameter))
        {
            targetCollection.add(this.value1);
        }
        return targetCollection;
    }

    @Override
    public PartitionImmutableBag<V> partition(Predicate<? super V> predicate)
    {
        PartitionMutableBag<V> partitionMutableBag = new PartitionHashBag<>();
        this.forEach(new PartitionProcedure<V>(predicate, partitionMutableBag));
        return partitionMutableBag.toImmutable();
    }

    @Override
    public <P> PartitionImmutableBag<V> partitionWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        PartitionMutableBag<V> partitionMutableBag = new PartitionHashBag<>();
        this.forEach(new PartitionPredicate2Procedure<V, P>(predicate, parameter, partitionMutableBag));
        return partitionMutableBag.toImmutable();
    }

    @Override
    public <S> ImmutableBag<S> selectInstancesOf(Class<S> clazz)
    {
        return clazz.isInstance(this.value1) ? Bags.immutable.with(clazz.cast(this.value1)) : Bags.immutable.<S>with();
    }

    @Override
    public <VV> ImmutableBag<VV> collect(Function<? super V, ? extends VV> function)
    {
        return Bags.immutable.with(function.valueOf(this.value1));
    }

    @Override
    public ImmutableBooleanBag collectBoolean(BooleanFunction<? super V> booleanFunction)
    {
        return BooleanBags.immutable.with(booleanFunction.booleanValueOf(this.value1));
    }

    @Override
    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super V> booleanFunction, R target)
    {
        target.add(booleanFunction.booleanValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableByteBag collectByte(ByteFunction<? super V> byteFunction)
    {
        return ByteBags.immutable.with(byteFunction.byteValueOf(this.value1));
    }

    @Override
    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super V> byteFunction, R target)
    {
        target.add(byteFunction.byteValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableCharBag collectChar(CharFunction<? super V> charFunction)
    {
        return CharBags.immutable.with(charFunction.charValueOf(this.value1));
    }

    @Override
    public <R extends MutableCharCollection> R collectChar(CharFunction<? super V> charFunction, R target)
    {
        target.add(charFunction.charValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableDoubleBag collectDouble(DoubleFunction<? super V> doubleFunction)
    {
        return DoubleBags.immutable.with(doubleFunction.doubleValueOf(this.value1));
    }

    @Override
    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super V> doubleFunction, R target)
    {
        target.add(doubleFunction.doubleValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableFloatBag collectFloat(FloatFunction<? super V> floatFunction)
    {
        return FloatBags.immutable.with(floatFunction.floatValueOf(this.value1));
    }

    @Override
    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super V> floatFunction, R target)
    {
        target.add(floatFunction.floatValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableIntBag collectInt(IntFunction<? super V> intFunction)
    {
        return IntBags.immutable.with(intFunction.intValueOf(this.value1));
    }

    @Override
    public <R extends MutableIntCollection> R collectInt(IntFunction<? super V> intFunction, R target)
    {
        target.add(intFunction.intValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableLongBag collectLong(LongFunction<? super V> longFunction)
    {
        return LongBags.immutable.with(longFunction.longValueOf(this.value1));
    }

    @Override
    public <R extends MutableLongCollection> R collectLong(LongFunction<? super V> longFunction, R target)
    {
        target.add(longFunction.longValueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableShortBag collectShort(ShortFunction<? super V> shortFunction)
    {
        return ShortBags.immutable.with(shortFunction.shortValueOf(this.value1));
    }

    @Override
    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super V> shortFunction, R target)
    {
        target.add(shortFunction.shortValueOf(this.value1));
        return target;
    }

    @Override
    public <VV> ImmutableBag<VV> collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function)
    {
        return predicate.accept(this.value1) ? Bags.immutable.with(function.valueOf(this.value1)) : Bags.immutable.<VV>with();
    }

    @Override
    public <VV> ImmutableBag<VV> flatCollect(Function<? super V, ? extends Iterable<VV>> function)
    {
        return Bags.immutable.withAll(function.valueOf(this.value1));
    }

    @Override
    public V detect(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1) ? this.value1 : null;
    }

    @Override
    public <P> V detectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter) ? this.value1 : null;
    }

    @Override
    public Optional<V> detectOptional(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1) ? Optional.of(this.value1) : Optional.empty();
    }

    @Override
    public <P> Optional<V> detectWithOptional(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter) ? Optional.of(this.value1) : Optional.empty();
    }

    @Override
    public V detectIfNone(Predicate<? super V> predicate, Function0<? extends V> function)
    {
        return predicate.accept(this.value1) ? this.value1 : function.value();
    }

    @Override
    public <P> V detectWithIfNone(Predicate2<? super V, ? super P> predicate, P parameter, Function0<? extends V> function)
    {
        return predicate.accept(this.value1, parameter) ? this.value1 : function.value();
    }

    @Override
    public int count(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1) ? 1 : 0;
    }

    @Override
    public <P> int countWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter) ? 1 : 0;
    }

    @Override
    public boolean anySatisfy(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter);
    }

    @Override
    public boolean allSatisfy(Predicate<? super V> predicate)
    {
        return predicate.accept(this.value1);
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return predicate.accept(this.value1, parameter);
    }

    @Override
    public boolean noneSatisfy(Predicate<? super V> predicate)
    {
        return !predicate.accept(this.value1);
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return !predicate.accept(this.value1, parameter);
    }

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super V, ? extends IV> function)
    {
        return function.value(injectedValue, this.value1);
    }

    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super V> function)
    {
        return function.intValueOf(injectedValue, this.value1);
    }

    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super V> function)
    {
        return function.longValueOf(injectedValue, this.value1);
    }

    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super V> function)
    {
        return function.floatValueOf(injectedValue, this.value1);
    }

    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super V> function)
    {
        return function.doubleValueOf(injectedValue, this.value1);
    }

    @Override
    public <R extends Collection<V>> R into(R target)
    {
        return Iterate.addAllTo(this, target);
    }

    @Override
    public MutableList<V> toList()
    {
        return Lists.mutable.with(this.value1);
    }

    @Override
    public MutableList<V> toSortedList()
    {
        return Lists.mutable.with(this.value1);
    }

    @Override
    public MutableList<V> toSortedList(Comparator<? super V> comparator)
    {
        return Lists.mutable.with(this.value1);
    }

    @Override
    public MutableSet<V> toSet()
    {
        return Sets.mutable.with(this.value1);
    }

    @Override
    public MutableSortedSet<V> toSortedSet()
    {
        return SortedSets.mutable.with(this.value1);
    }

    @Override
    public MutableSortedSet<V> toSortedSet(Comparator<? super V> comparator)
    {
        return SortedSets.mutable.with(this.value1);
    }

    @Override
    public MutableBag<V> toBag()
    {
        return Bags.mutable.with(this.value1);
    }

    @Override
    public MutableSortedBag<V> toSortedBag()
    {
        return TreeBag.newBagWith(this.value1);
    }

    @Override
    public MutableSortedBag<V> toSortedBag(Comparator<? super V> comparator)
    {
        return TreeBag.newBagWith(comparator, this.value1);
    }

    @Override
    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return Maps.mutable.with(keyFunction.valueOf(this.value1), valueFunction.valueOf(this.value1));
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with(keyFunction.valueOf(this.value1), valueFunction.valueOf(this.value1));
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with(keyFunction.valueOf(this.value1), valueFunction.valueOf(this.value1));
    }

    @Override
    public <KK extends Comparable<? super KK>, NK, NV> MutableSortedMap<NK, NV> toSortedMapBy(Function<? super NK, KK> sortBy, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with(keyFunction.valueOf(this.value1), valueFunction.valueOf(this.value1));
    }

    @Override
    public <NK, NV> MutableBiMap<NK, NV> toBiMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return BiMaps.mutable.with(keyFunction.valueOf(this.value1), valueFunction.valueOf(this.value1));
    }

    @Override
    public LazyIterable<V> asLazy()
    {
        return new LazyIterableAdapter<>(Lists.mutable.with(this.value1));
    }

    @Override
    public Object[] toArray()
    {
        return new Object[]{this.value1};
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return (T[]) new Object[]{this.value1};
    }

    @Override
    public V min(Comparator<? super V> comparator)
    {
        return this.value1;
    }

    @Override
    public V max(Comparator<? super V> comparator)
    {
        return this.value1;
    }

    @Override
    public V min()
    {
        return this.value1;
    }

    @Override
    public V max()
    {
        return this.value1;
    }

    @Override
    public long sumOfInt(IntFunction<? super V> function)
    {
        return function.intValueOf(this.value1);
    }

    @Override
    public double sumOfFloat(FloatFunction<? super V> function)
    {
        return function.floatValueOf(this.value1);
    }

    @Override
    public long sumOfLong(LongFunction<? super V> function)
    {
        return function.longValueOf(this.value1);
    }

    @Override
    public double sumOfDouble(DoubleFunction<? super V> function)
    {
        return function.doubleValueOf(this.value1);
    }

    @Override
    public String makeString()
    {
        return this.makeString(", ");
    }

    @Override
    public String makeString(String separator)
    {
        return this.makeString("", separator, "");
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        Appendable stringBuilder = new StringBuilder();
        this.appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.appendString(appendable, ", ");
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.appendString(appendable, "", separator, "");
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
            appendable.append(String.valueOf(this.value1));
            appendable.append(end);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <VV> ImmutableBagMultimap<VV, V> groupBy(Function<? super V, ? extends VV> function)
    {
        return Multimaps.immutable.bag.with(function.valueOf(this.value1), this.value1);
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupBy(Function<? super V, ? extends VV> function, R target)
    {
        target.put(function.valueOf(this.value1), this.value1);
        return target;
    }

    @Override
    public <VV> ImmutableBagMultimap<VV, V> groupByEach(Function<? super V, ? extends Iterable<VV>> function)
    {
        return this.groupByEach(function, HashBagMultimap.<VV, V>newMultimap()).toImmutable();
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupByEach(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        Iterable<VV> iterable = function.valueOf(this.value1);
        for (VV key : iterable)
        {
            target.put(key, this.value1);
        }
        return target;
    }

    @Override
    public <VV> ImmutableMap<VV, V> groupByUniqueKey(Function<? super V, ? extends VV> function)
    {
        return Maps.immutable.with(function.valueOf(this.value1), this.value1);
    }

    @Override
    public <VV, R extends MutableMapIterable<VV, V>> R groupByUniqueKey(Function<? super V, ? extends VV> function, R target)
    {
        if (target.put(function.valueOf(this.value1), this.value1) != null)
        {
            throw new IllegalStateException("Key " + function.valueOf(this.value1) + " already exists in map!");
        }
        return target;
    }

    @Override
    public <S> ImmutableBag<Pair<V, S>> zip(Iterable<S> that)
    {
        return this.zip(that, HashBag.<Pair<V, S>>newBag()).toImmutable();
    }

    @Override
    public <S, R extends Collection<Pair<V, S>>> R zip(Iterable<S> that, R target)
    {
        return IterableIterate.zip(this, that, target);
    }

    @Override
    public ImmutableSet<Pair<V, Integer>> zipWithIndex()
    {
        return this.zipWithIndex(UnifiedSet.<Pair<V, Integer>>newSet()).toImmutable();
    }

    @Override
    public <R extends Collection<Pair<V, Integer>>> R zipWithIndex(R target)
    {
        return IterableIterate.zipWithIndex(this, target);
    }

    @Override
    public RichIterable<RichIterable<V>> chunk(int size)
    {
        if (size <= 0)
        {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + size);
        }
        MutableList<RichIterable<V>> result = Lists.mutable.of();
        result.add(Lists.mutable.with(this.value1));
        return result;
    }

    @Override
    public <VV extends Comparable<? super VV>> V maxBy(Function<? super V, ? extends VV> function)
    {
        return this.value1;
    }

    @Override
    public <VV extends Comparable<? super VV>> V minBy(Function<? super V, ? extends VV> function)
    {
        return this.value1;
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableSortedSet<V> toSortedSetBy(Function<? super V, ? extends VV> function)
    {
        return SortedSets.mutable.with(this.value1);
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableList<V> toSortedListBy(Function<? super V, ? extends VV> function)
    {
        return Lists.mutable.of(this.value1);
    }

    @Override
    public <VV, R extends Collection<VV>> R flatCollect(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        Iterate.addAllTo(function.valueOf(this.value1), target);
        return target;
    }

    @Override
    public <VV, R extends Collection<VV>> R collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function, R target)
    {
        if (predicate.accept(this.value1))
        {
            target.add(function.valueOf(this.value1));
        }
        return target;
    }

    @Override
    public <P, VV> ImmutableBag<VV> collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter)
    {
        return Bags.immutable.with(function.value(this.value1, parameter));
    }

    @Override
    public <P, VV, R extends Collection<VV>> R collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter, R targetCollection)
    {
        targetCollection.add(function.value(this.value1, parameter));
        return targetCollection;
    }

    @Override
    public <VV, R extends Collection<VV>> R collect(Function<? super V, ? extends VV> function, R target)
    {
        target.add(function.valueOf(this.value1));
        return target;
    }

    @Override
    public ImmutableLongObjectMap<V> newWithKeyValue(long key, V value)
    {
        return LongObjectHashMap.newWithKeysValues(this.key1, this.value1, key, value).toImmutable();
    }

    @Override
    public ImmutableLongObjectMap<V> newWithoutKey(long key)
    {
        return this.key1 == key ? (ImmutableLongObjectMap<V>) ImmutableLongObjectEmptyMap.INSTANCE : this;
    }

    @Override
    public ImmutableLongObjectMap<V> newWithoutAllKeys(LongIterable keys)
    {
        return keys.contains(this.key1) ? (ImmutableLongObjectMap<V>) ImmutableLongObjectEmptyMap.INSTANCE : this;
    }

    @Override
    public void forEach(Procedure<? super V> procedure)
    {
        this.each(procedure);
    }

    @Override
    public void each(Procedure<? super V> procedure)
    {
        procedure.value(this.value1);
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super V> objectIntProcedure)
    {
        objectIntProcedure.value(this.value1, 0);
    }

    @Override
    public <P> void forEachWith(Procedure2<? super V, ? super P> procedure, P parameter)
    {
        procedure.value(this.value1, parameter);
    }

    @Override
    public Iterator<V> iterator()
    {
        return new UnmodifiableIteratorAdapter<>(LongObjectHashMap.newWithKeysValues(this.key1, this.value1).iterator());
    }

    private Object writeReplace()
    {
        return new ImmutableLongObjectMapSerializationProxy<V>(this);
    }
}
