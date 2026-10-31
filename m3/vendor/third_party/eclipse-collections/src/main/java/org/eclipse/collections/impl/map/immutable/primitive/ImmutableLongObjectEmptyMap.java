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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.LazyLongIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
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
import org.eclipse.collections.api.block.procedure.primitive.LongObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
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
import org.eclipse.collections.api.map.primitive.ImmutableObjectDoubleMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectLongMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBag;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.bag.sorted.mutable.TreeBag;
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
import org.eclipse.collections.impl.factory.primitive.ShortBags;
import org.eclipse.collections.impl.factory.primitive.ObjectDoubleMaps;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.lazy.LazyIterableAdapter;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.partition.bag.PartitionHashBag;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableLongSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.primitive.LazyLongIterate;

/**
 * ImmutableLongObjectEmptyMap is an optimization for {@link ImmutableLongObjectMap} of size 0.
 * This file was automatically generated from template file immutablePrimitiveObjectEmptyMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableLongObjectEmptyMap<V> implements ImmutableLongObjectMap<V>, Serializable
{
    static final ImmutableLongObjectMap<?> INSTANCE = new ImmutableLongObjectEmptyMap<>();

    private static final long serialVersionUID = 1L;

    private Object readResolve()
    {
        return INSTANCE;
    }

    @Override
    public V get(long key)
    {
        return null;
    }

    @Override
    public V getIfAbsent(long key, Function0<? extends V> ifAbsent)
    {
        return ifAbsent.value();
    }

    @Override
    public boolean containsKey(long key)
    {
        return false;
    }

    @Override
    public boolean containsValue(Object value)
    {
        return false;
    }

    @Override
    public ImmutableLongObjectMap<V> tap(Procedure<? super V> procedure)
    {
        return this;
    }

    @Override
    public void forEachValue(Procedure<? super V> procedure)
    {
    }

    @Override
    public void forEachKey(LongProcedure procedure)
    {
    }

    @Override
    public void forEachKeyValue(LongObjectProcedure<? super V> procedure)
    {
    }

    @Override
    public ImmutableLongObjectMap<V> select(LongObjectPredicate<? super V> predicate)
    {
        return this;
    }

    @Override
    public ImmutableLongObjectMap<V> reject(LongObjectPredicate<? super V> predicate)
    {
        return this;
    }

    @Override
    public ImmutableLongObjectMap<V> toImmutable()
    {
        return this;
    }

    @Override
    public int size()
    {
        return 0;
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public boolean notEmpty()
    {
        return false;
    }

    @Override
    public V getFirst()
    {
        return null;
    }

    @Override
    public V getLast()
    {
        return null;
    }

    @Override
    public V getOnly()
    {
        throw new IllegalStateException("Size must be 1 but was " + this.size());
    }

    @Override
    public boolean contains(Object object)
    {
        return false;
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        return !source.iterator().hasNext();
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        return source.isEmpty();
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        return elements.length == 0;
    }

    @Override
    public ImmutableBag<V> select(Predicate<? super V> predicate)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <P> ImmutableBag<V> selectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return Bags.immutable.empty();
    }

    @Override
    public ImmutableBag<V> reject(Predicate<? super V> predicate)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <P> ImmutableBag<V> rejectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <R extends Collection<V>> R select(Predicate<? super V> predicate, R target)
    {
        return target;
    }

    @Override
    public <P, R extends Collection<V>> R selectWith(Predicate2<? super V, ? super P> predicate, P parameter, R target)
    {
        return target;
    }

    @Override
    public <R extends Collection<V>> R reject(Predicate<? super V> predicate, R target)
    {
        return target;
    }

    @Override
    public <P, R extends Collection<V>> R rejectWith(Predicate2<? super V, ? super P> predicate, P parameter, R target)
    {
        return target;
    }

    @Override
    public PartitionImmutableBag<V> partition(Predicate<? super V> predicate)
    {
        return new PartitionHashBag<V>().toImmutable();
    }

    @Override
    public <P> PartitionImmutableBag<V> partitionWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return new PartitionHashBag<V>().toImmutable();
    }

    @Override
    public <S> ImmutableBag<S> selectInstancesOf(Class<S> clazz)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <VV> ImmutableBag<VV> collect(Function<? super V, ? extends VV> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <P, VV> ImmutableBag<VV> collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <VV> ImmutableBag<VV> collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public ImmutableBooleanBag collectBoolean(BooleanFunction<? super V> booleanFunction)
    {
        return BooleanBags.immutable.empty();
    }

    @Override
    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super V> booleanFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableByteBag collectByte(ByteFunction<? super V> byteFunction)
    {
        return ByteBags.immutable.empty();
    }

    @Override
    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super V> byteFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableCharBag collectChar(CharFunction<? super V> charFunction)
    {
        return CharBags.immutable.empty();
    }

    @Override
    public <R extends MutableCharCollection> R collectChar(CharFunction<? super V> charFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableDoubleBag collectDouble(DoubleFunction<? super V> doubleFunction)
    {
        return DoubleBags.immutable.empty();
    }

    @Override
    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super V> doubleFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableFloatBag collectFloat(FloatFunction<? super V> floatFunction)
    {
        return FloatBags.immutable.empty();
    }

    @Override
    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super V> floatFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableIntBag collectInt(IntFunction<? super V> intFunction)
    {
        return IntBags.immutable.empty();
    }

    @Override
    public <R extends MutableIntCollection> R collectInt(IntFunction<? super V> intFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableLongBag collectLong(LongFunction<? super V> longFunction)
    {
        return LongBags.immutable.empty();
    }

    @Override
    public <R extends MutableLongCollection> R collectLong(LongFunction<? super V> longFunction, R target)
    {
        return target;
    }

    @Override
    public ImmutableShortBag collectShort(ShortFunction<? super V> shortFunction)
    {
        return ShortBags.immutable.empty();
    }

    @Override
    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super V> shortFunction, R target)
    {
        return target;
    }

    @Override
    public <VV> ImmutableBag<VV> flatCollect(Function<? super V, ? extends Iterable<VV>> function)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <R extends MutableByteCollection> R flatCollectByte(
            Function<? super V, ? extends ByteIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableCharCollection> R flatCollectChar(
            Function<? super V, ? extends CharIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableIntCollection> R flatCollectInt(
            Function<? super V, ? extends IntIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableShortCollection> R flatCollectShort(
            Function<? super V, ? extends ShortIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableDoubleCollection> R flatCollectDouble(
            Function<? super V, ? extends DoubleIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableFloatCollection> R flatCollectFloat(
            Function<? super V, ? extends FloatIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableLongCollection> R flatCollectLong(
            Function<? super V, ? extends LongIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public <R extends MutableBooleanCollection> R flatCollectBoolean(
            Function<? super V, ? extends BooleanIterable> function,
            R target)
    {
        return target;
    }

    @Override
    public V detect(Predicate<? super V> predicate)
    {
        return null;
    }

    @Override
    public <P> V detectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return null;
    }

    @Override
    public Optional<V> detectOptional(Predicate<? super V> predicate)
    {
        return Optional.empty();
    }

    @Override
    public <P> Optional<V> detectWithOptional(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return Optional.empty();
    }

    @Override
    public V detectIfNone(Predicate<? super V> predicate, Function0<? extends V> function)
    {
        return function.value();
    }

    @Override
    public <P> V detectWithIfNone(Predicate2<? super V, ? super P> predicate, P parameter, Function0<? extends V> function)
    {
        return function.value();
    }

    @Override
    public int count(Predicate<? super V> predicate)
    {
        return 0;
    }

    @Override
    public <P> int countWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return 0;
    }

    @Override
    public boolean anySatisfy(Predicate<? super V> predicate)
    {
        return false;
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return false;
    }

    @Override
    public boolean allSatisfy(Predicate<? super V> predicate)
    {
        return true;
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return true;
    }

    @Override
    public boolean noneSatisfy(Predicate<? super V> predicate)
    {
        return true;
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return true;
    }

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super V, ? extends IV> function)
    {
        return injectedValue;
    }

    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super V> function)
    {
        return injectedValue;
    }

    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super V> function)
    {
        return injectedValue;
    }

    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super V> function)
    {
        return injectedValue;
    }

    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super V> function)
    {
        return injectedValue;
    }

    @Override
    public <R extends Collection<V>> R into(R target)
    {
        return target;
    }

    @Override
    public MutableList<V> toList()
    {
        return Lists.mutable.with();
    }

    @Override
    public MutableList<V> toSortedList()
    {
        return Lists.mutable.with();
    }

    @Override
    public MutableList<V> toSortedList(Comparator<? super V> comparator)
    {
        return Lists.mutable.with();
    }

    @Override
    public MutableSet<V> toSet()
    {
        return Sets.mutable.with();
    }

    @Override
    public MutableSortedSet<V> toSortedSet()
    {
        return SortedSets.mutable.with();
    }

    @Override
    public MutableSortedSet<V> toSortedSet(Comparator<? super V> comparator)
    {
        return SortedSets.mutable.with();
    }

    @Override
    public MutableBag<V> toBag()
    {
        return Bags.mutable.with();
    }

    @Override
    public MutableSortedBag<V> toSortedBag()
    {
        return TreeBag.newBag();
    }

    @Override
    public MutableSortedBag<V> toSortedBag(Comparator<? super V> comparator)
    {
        return TreeBag.newBag(comparator);
    }

    @Override
    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return Maps.mutable.with();
    }

    @Override
    public <NK, NV, R extends Map<NK, NV>> R toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction, R target)
    {
        return target;
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with();
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with();
    }

    @Override
    public <KK extends Comparable<? super KK>, NK, NV> MutableSortedMap<NK, NV> toSortedMapBy(Function<? super NK, KK> sortBy, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return SortedMaps.mutable.with();
    }

    @Override
    public <NK, NV> MutableBiMap<NK, NV> toBiMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return BiMaps.mutable.empty();
    }

    @Override
    public LazyIterable<V> asLazy()
    {
        return new LazyIterableAdapter<>(Lists.immutable.<V>empty());
    }

    @Override
    public Object[] toArray()
    {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return (T[]) new Object[0];
    }

    @Override
    public V min(Comparator<? super V> comparator)
    {
        throw new NoSuchElementException();
    }

    @Override
    public V max(Comparator<? super V> comparator)
    {
        throw new NoSuchElementException();
    }

    @Override
    public V min()
    {
        throw new NoSuchElementException();
    }

    @Override
    public V max()
    {
        throw new NoSuchElementException();
    }

    @Override
    public long sumOfInt(IntFunction<? super V> function)
    {
        return 0L;
    }

    @Override
    public double sumOfFloat(FloatFunction<? super V> function)
    {
        return 0.0;
    }

    @Override
    public long sumOfLong(LongFunction<? super V> function)
    {
        return 0L;
    }

    @Override
    public double sumOfDouble(DoubleFunction<? super V> function)
    {
        return 0.0;
    }

    @Override
    public <V1> ImmutableObjectLongMap<V1> sumByInt(Function<? super V, ? extends V1> groupBy, IntFunction<? super V> function)
    {
        return ObjectLongMaps.immutable.empty();
    }

    @Override
    public <V1> ImmutableObjectDoubleMap<V1> sumByFloat(Function<? super V, ? extends V1> groupBy, FloatFunction<? super V> function)
    {
        return ObjectDoubleMaps.immutable.empty();
    }

    @Override
    public <V1> ImmutableObjectLongMap<V1> sumByLong(Function<? super V, ? extends V1> groupBy, LongFunction<? super V> function)
    {
        return ObjectLongMaps.immutable.empty();
    }

    @Override
    public <V1> ImmutableObjectDoubleMap<V1> sumByDouble(Function<? super V, ? extends V1> groupBy, DoubleFunction<? super V> function)
    {
        return ObjectDoubleMaps.immutable.empty();
    }

    @Override
    public MutableLongSet keySet()
    {
        return UnmodifiableLongSet.of(LongHashSet.newSetWith());
    }

    @Override
    public Collection<V> values()
    {
        return Lists.immutable.<V>of().castToList();
    }

    @Override
    public LazyLongIterable keysView()
    {
        return LazyLongIterate.empty();
    }

    @Override
    public RichIterable<LongObjectPair<V>> keyValuesView()
    {
        return LazyIterate.empty();
    }

    @Override
    public ImmutableObjectLongMap<V> flipUniqueValues()
    {
        return ObjectLongMaps.immutable.empty();
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
        return map.isEmpty();
    }

    @Override
    public int hashCode()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return "{}";
    }

    @Override
    public String makeString()
    {
        return "";
    }

    @Override
    public String makeString(String separator)
    {
        return "";
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return start + end;
    }

    @Override
    public void appendString(Appendable appendable)
    {
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        try
        {
            appendable.append(start);
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
        return Multimaps.immutable.bag.empty();
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupBy(Function<? super V, ? extends VV> function, R target)
    {
        return target;
    }

    @Override
    public <VV> ImmutableBagMultimap<VV, V> groupByEach(Function<? super V, ? extends Iterable<VV>> function)
    {
        return Multimaps.immutable.bag.empty();
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupByEach(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        return target;
    }

    @Override
    public <VV> ImmutableMap<VV, V> groupByUniqueKey(Function<? super V, ? extends VV> function)
    {
        return Maps.immutable.empty();
    }

    @Override
    public <VV, R extends MutableMapIterable<VV, V>> R groupByUniqueKey(Function<? super V, ? extends VV> function, R target)
    {
        return target;
    }

    @Override
    public <S> ImmutableBag<Pair<V, S>> zip(Iterable<S> that)
    {
        return Bags.immutable.empty();
    }

    @Override
    public <S, R extends Collection<Pair<V, S>>> R zip(Iterable<S> that, R target)
    {
        return target;
    }

    @Override
    public ImmutableSet<Pair<V, Integer>> zipWithIndex()
    {
        return Sets.immutable.empty();
    }

    @Override
    public <R extends Collection<Pair<V, Integer>>> R zipWithIndex(R target)
    {
        return target;
    }

    @Override
    public RichIterable<RichIterable<V>> chunk(int size)
    {
        return Lists.immutable.of();
    }

    @Override
    public <VV extends Comparable<? super VV>> V maxBy(Function<? super V, ? extends VV> function)
    {
        throw new NoSuchElementException();
    }

    @Override
    public <VV extends Comparable<? super VV>> V minBy(Function<? super V, ? extends VV> function)
    {
        throw new NoSuchElementException();
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableSortedSet<V> toSortedSetBy(Function<? super V, ? extends VV> function)
    {
        return SortedSets.mutable.of();
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableList<V> toSortedListBy(Function<? super V, ? extends VV> function)
    {
        return Lists.mutable.of();
    }

    @Override
    public <VV, R extends Collection<VV>> R flatCollect(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        return target;
    }

    @Override
    public <VV, R extends Collection<VV>> R collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function, R target)
    {
        return target;
    }

    @Override
    public <P, VV, R extends Collection<VV>> R collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter, R targetCollection)
    {
        return targetCollection;
    }

    @Override
    public <VV, R extends Collection<VV>> R collect(Function<? super V, ? extends VV> function, R target)
    {
        return target;
    }

    @Override
    public ImmutableLongObjectMap<V> newWithKeyValue(long key, V value)
    {
        LongObjectHashMap<V> map = new LongObjectHashMap<>();
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableLongObjectMap<V> newWithoutKey(long key)
    {
        return this;
    }

    @Override
    public ImmutableLongObjectMap<V> newWithoutAllKeys(LongIterable keys)
    {
        return this;
    }

    @Override
    public void forEach(Procedure<? super V> procedure)
    {
    }

    @Override
    public void each(Procedure<? super V> procedure)
    {
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super V> objectIntProcedure)
    {
    }

    @Override
    public <P> void forEachWith(Procedure2<? super V, ? super P> procedure, P parameter)
    {
    }

    @Override
    public Iterator<V> iterator()
    {
        return new InternalIterator();
    }

    private class InternalIterator implements Iterator<V>
    {
        @Override
        public boolean hasNext()
        {
            return false;
        }

        @Override
        public V next()
        {
            throw new NoSuchElementException();
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
        }
    }
}
