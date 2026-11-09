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

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyCharIterable;
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
import org.eclipse.collections.api.block.predicate.primitive.CharObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.block.procedure.primitive.CharObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.api.collection.primitive.MutableCharCollection;
import org.eclipse.collections.api.collection.primitive.MutableDoubleCollection;
import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.api.collection.primitive.MutableIntCollection;
import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.api.collection.primitive.MutableShortCollection;
import org.eclipse.collections.api.iterator.CharIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.api.map.primitive.CharObjectMap;
import org.eclipse.collections.api.map.primitive.ImmutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.ImmutableObjectCharMap;
import org.eclipse.collections.api.map.primitive.MutableCharObjectMap;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBag;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.CharObjectPair;
import org.eclipse.collections.impl.UnmodifiableIteratorAdapter;
import org.eclipse.collections.impl.collection.mutable.UnmodifiableMutableCollection;
import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;
import org.eclipse.collections.impl.factory.primitive.ObjectCharMaps;
import org.eclipse.collections.impl.set.mutable.primitive.UnmodifiableCharSet;

/**
 * ImmutableCharObjectHashMap is the non-modifiable equivalent of {@link CharObjectHashMap}.
 * This file was automatically generated from template file immutablePrimitiveObjectHashMap.stg.
 *
 * @since 4.0.
 */
final class ImmutableCharObjectHashMap<V> extends AbstractImmutableCharObjectMap<V> implements Serializable
{
    private static final long serialVersionUID = 1L;
    private final MutableCharObjectMap<V> delegate;

    ImmutableCharObjectHashMap(CharObjectMap<? extends V> delegate)
    {
        this.delegate = new CharObjectHashMap<>(delegate);
    }

    @Override
    public V get(char key)
    {
        return this.delegate.get(key);
    }

    @Override
    public V getIfAbsent(char key, Function0<? extends V> ifAbsent)
    {
        return this.delegate.getIfAbsent(key, ifAbsent);
    }

    @Override
    public boolean containsKey(char key)
    {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return this.delegate.containsValue(value);
    }

    @Override
    public ImmutableCharObjectMap<V> tap(Procedure<? super V> procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    public void forEachValue(Procedure<? super V> procedure)
    {
        this.delegate.forEachValue(procedure);
    }

    @Override
    public void forEachKey(CharProcedure procedure)
    {
        this.delegate.forEachKey(procedure);
    }

    @Override
    public void forEachKeyValue(CharObjectProcedure<? super V> procedure)
    {
        this.delegate.forEachKeyValue(procedure);
    }

    @Override
    public ImmutableCharObjectMap<V> select(CharObjectPredicate<? super V> predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public ImmutableCharObjectMap<V> reject(CharObjectPredicate<? super V> predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public ImmutableCharObjectMap<V> toImmutable()
    {
        return this;
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean notEmpty()
    {
        return this.delegate.notEmpty();
    }

    @Override
    public V getFirst()
    {
        return this.delegate.getFirst();
    }

    @Override
    public V getLast()
    {
        return this.delegate.getLast();
    }

    @Override
    public V getOnly()
    {
        return this.delegate.getOnly();
    }

    @Override
    public boolean contains(Object object)
    {
        return this.delegate.contains(object);
    }

    @Override
    public boolean containsAllIterable(Iterable<?> source)
    {
        return this.delegate.containsAllIterable(source);
    }

    @Override
    public boolean containsAll(Collection<?> source)
    {
        return this.delegate.containsAll(source);
    }

    @Override
    public boolean containsAllArguments(Object... elements)
    {
        return this.delegate.containsAllArguments(elements);
    }

    @Override
    public ImmutableBag<V> select(Predicate<? super V> predicate)
    {
        return this.delegate.select(predicate).toImmutable();
    }

    @Override
    public <R extends Collection<V>> R select(Predicate<? super V> predicate, R target)
    {
        return this.delegate.select(predicate, target);
    }

    @Override
    public <P> ImmutableBag<V> selectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.selectWith(predicate, parameter).toImmutable();
    }

    @Override
    public <P, R extends Collection<V>> R selectWith(Predicate2<? super V, ? super P> predicate, P parameter, R target)
    {
        return this.delegate.selectWith(predicate, parameter, target);
    }

    @Override
    public ImmutableBag<V> reject(Predicate<? super V> predicate)
    {
        return this.delegate.reject(predicate).toImmutable();
    }

    @Override
    public <R extends Collection<V>> R reject(Predicate<? super V> predicate, R target)
    {
        return this.delegate.reject(predicate, target);
    }

    @Override
    public <P> ImmutableBag<V> rejectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.rejectWith(predicate, parameter).toImmutable();
    }

    @Override
    public <P, R extends Collection<V>> R rejectWith(Predicate2<? super V, ? super P> predicate, P parameter, R target)
    {
        return this.delegate.rejectWith(predicate, parameter, target);
    }

    @Override
    public PartitionImmutableBag<V> partition(Predicate<? super V> predicate)
    {
        return this.delegate.partition(predicate).toImmutable();
    }

    @Override
    public <P> PartitionImmutableBag<V> partitionWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.partitionWith(predicate, parameter).toImmutable();
    }

    @Override
    public <S> ImmutableBag<S> selectInstancesOf(Class<S> clazz)
    {
        return this.delegate.selectInstancesOf(clazz).toImmutable();
    }

    @Override
    public <VV> ImmutableBag<VV> collect(Function<? super V, ? extends VV> function)
    {
        MutableBag<VV> bag = this.delegate.collect(function);
        return bag.toImmutable();
    }

    @Override
    public ImmutableBooleanBag collectBoolean(BooleanFunction<? super V> booleanFunction)
    {
        return this.delegate.collectBoolean(booleanFunction).toImmutable();
    }

    @Override
    public <R extends MutableBooleanCollection> R collectBoolean(BooleanFunction<? super V> booleanFunction, R target)
    {
        return this.delegate.collectBoolean(booleanFunction, target);
    }

    @Override
    public ImmutableByteBag collectByte(ByteFunction<? super V> byteFunction)
    {
        return this.delegate.collectByte(byteFunction).toImmutable();
    }

    @Override
    public <R extends MutableByteCollection> R collectByte(ByteFunction<? super V> byteFunction, R target)
    {
        return this.delegate.collectByte(byteFunction, target);
    }

    @Override
    public ImmutableCharBag collectChar(CharFunction<? super V> charFunction)
    {
        return this.delegate.collectChar(charFunction).toImmutable();
    }

    @Override
    public <R extends MutableCharCollection> R collectChar(CharFunction<? super V> charFunction, R target)
    {
        return this.delegate.collectChar(charFunction, target);
    }

    @Override
    public ImmutableDoubleBag collectDouble(DoubleFunction<? super V> doubleFunction)
    {
        return this.delegate.collectDouble(doubleFunction).toImmutable();
    }

    @Override
    public <R extends MutableDoubleCollection> R collectDouble(DoubleFunction<? super V> doubleFunction, R target)
    {
        return this.delegate.collectDouble(doubleFunction, target);
    }

    @Override
    public ImmutableFloatBag collectFloat(FloatFunction<? super V> floatFunction)
    {
        return this.delegate.collectFloat(floatFunction).toImmutable();
    }

    @Override
    public <R extends MutableFloatCollection> R collectFloat(FloatFunction<? super V> floatFunction, R target)
    {
        return this.delegate.collectFloat(floatFunction, target);
    }

    @Override
    public ImmutableIntBag collectInt(IntFunction<? super V> intFunction)
    {
        return this.delegate.collectInt(intFunction).toImmutable();
    }

    @Override
    public <R extends MutableIntCollection> R collectInt(IntFunction<? super V> intFunction, R target)
    {
        return this.delegate.collectInt(intFunction, target);
    }

    @Override
    public ImmutableLongBag collectLong(LongFunction<? super V> longFunction)
    {
        return this.delegate.collectLong(longFunction).toImmutable();
    }

    @Override
    public <R extends MutableLongCollection> R collectLong(LongFunction<? super V> longFunction, R target)
    {
        return this.delegate.collectLong(longFunction, target);
    }

    @Override
    public ImmutableShortBag collectShort(ShortFunction<? super V> shortFunction)
    {
        return this.delegate.collectShort(shortFunction).toImmutable();
    }

    @Override
    public <R extends MutableShortCollection> R collectShort(ShortFunction<? super V> shortFunction, R target)
    {
        return this.delegate.collectShort(shortFunction, target);
    }

    @Override
    public <P, VV> ImmutableBag<VV> collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter)
    {
        MutableBag<VV> bag = this.delegate.collectWith(function, parameter);
        return bag.toImmutable();
    }

    @Override
    public <VV> ImmutableBag<VV> collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function)
    {
        MutableBag<VV> bag = this.delegate.collectIf(predicate, function);
        return bag.toImmutable();
    }

    @Override
    public <VV> ImmutableBag<VV> flatCollect(Function<? super V, ? extends Iterable<VV>> function)
    {
        return this.delegate.flatCollect(function).toImmutable();
    }

    @Override
    public V detect(Predicate<? super V> predicate)
    {
        return this.delegate.detect(predicate);
    }

    @Override
    public <P> V detectWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.detectWith(predicate, parameter);
    }

    @Override
    public Optional<V> detectOptional(Predicate<? super V> predicate)
    {
        return this.delegate.detectOptional(predicate);
    }

    @Override
    public <P> Optional<V> detectWithOptional(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.detectWithOptional(predicate, parameter);
    }

    @Override
    public V detectIfNone(Predicate<? super V> predicate, Function0<? extends V> function)
    {
        return this.delegate.detectIfNone(predicate, function);
    }

    @Override
    public <P> V detectWithIfNone(Predicate2<? super V, ? super P> predicate, P parameter, Function0<? extends V> function)
    {
        return this.delegate.detectWithIfNone(predicate, parameter, function);
    }

    @Override
    public int count(Predicate<? super V> predicate)
    {
        return this.delegate.count(predicate);
    }

    @Override
    public <P> int countWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.countWith(predicate, parameter);
    }

    @Override
    public boolean anySatisfy(Predicate<? super V> predicate)
    {
        return this.delegate.anySatisfy(predicate);
    }

    @Override
    public <P> boolean anySatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.anySatisfyWith(predicate, parameter);
    }

    @Override
    public boolean allSatisfy(Predicate<? super V> predicate)
    {
        return this.delegate.allSatisfy(predicate);
    }

    @Override
    public <P> boolean allSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.allSatisfyWith(predicate, parameter);
    }

    @Override
    public boolean noneSatisfy(Predicate<? super V> predicate)
    {
        return this.delegate.noneSatisfy(predicate);
    }

    @Override
    public <P> boolean noneSatisfyWith(Predicate2<? super V, ? super P> predicate, P parameter)
    {
        return this.delegate.noneSatisfyWith(predicate, parameter);
    }

    @Override
    public <IV> IV injectInto(IV injectedValue, Function2<? super IV, ? super V, ? extends IV> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public int injectInto(int injectedValue, IntObjectToIntFunction<? super V> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public long injectInto(long injectedValue, LongObjectToLongFunction<? super V> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public float injectInto(float injectedValue, FloatObjectToFloatFunction<? super V> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public double injectInto(double injectedValue, DoubleObjectToDoubleFunction<? super V> function)
    {
        return this.delegate.injectInto(injectedValue, function);
    }

    @Override
    public <R extends Collection<V>> R into(R target)
    {
        return this.delegate.into(target);
    }

    @Override
    public MutableList<V> toList()
    {
        return this.delegate.toList();
    }

    @Override
    public MutableList<V> toSortedList()
    {
        return this.delegate.toSortedList();
    }

    @Override
    public MutableList<V> toSortedList(Comparator<? super V> comparator)
    {
        return this.delegate.toSortedList(comparator);
    }

    @Override
    public MutableSet<V> toSet()
    {
        return this.delegate.toSet();
    }

    @Override
    public MutableSortedSet<V> toSortedSet()
    {
        return this.delegate.toSortedSet();
    }

    @Override
    public MutableSortedSet<V> toSortedSet(Comparator<? super V> comparator)
    {
        return this.delegate.toSortedSet(comparator);
    }

    @Override
    public MutableBag<V> toBag()
    {
        return this.delegate.toBag();
    }

    @Override
    public MutableSortedBag<V> toSortedBag()
    {
        return this.delegate.toSortedBag();
    }

    @Override
    public MutableSortedBag<V> toSortedBag(Comparator<? super V> comparator)
    {
        return this.delegate.toSortedBag(comparator);
    }

    @Override
    public <NK, NV> MutableMap<NK, NV> toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return this.delegate.toMap(keyFunction, valueFunction);
    }

    @Override
    public <NK, NV, R extends Map<NK, NV>> R toMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction, R target)
    {
        return this.delegate.toMap(keyFunction, valueFunction, target);
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return this.delegate.toSortedMap(keyFunction, valueFunction);
    }

    @Override
    public <NK, NV> MutableSortedMap<NK, NV> toSortedMap(Comparator<? super NK> comparator, Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return this.delegate.toSortedMap(comparator, keyFunction, valueFunction);
    }

    @Override
    public <NK, NV> MutableBiMap<NK, NV> toBiMap(Function<? super V, ? extends NK> keyFunction, Function<? super V, ? extends NV> valueFunction)
    {
        return this.delegate.toBiMap(keyFunction, valueFunction);
    }

    @Override
    public LazyIterable<V> asLazy()
    {
        return this.delegate.asLazy();
    }

    @Override
    public Object[] toArray()
    {
        return this.delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a)
    {
        return this.delegate.toArray(a);
    }

    @Override
    public V min(Comparator<? super V> comparator)
    {
        return this.delegate.min(comparator);
    }

    @Override
    public V max(Comparator<? super V> comparator)
    {
        return this.delegate.max(comparator);
    }

    @Override
    public V min()
    {
        return this.delegate.min();
    }

    @Override
    public V max()
    {
        return this.delegate.max();
    }

    @Override
    public long sumOfInt(IntFunction<? super V> function)
    {
        return this.delegate.sumOfInt(function);
    }

    @Override
    public double sumOfFloat(FloatFunction<? super V> function)
    {
        return this.delegate.sumOfFloat(function);
    }

    @Override
    public long sumOfLong(LongFunction<? super V> function)
    {
        return this.delegate.sumOfLong(function);
    }

    @Override
    public double sumOfDouble(DoubleFunction<? super V> function)
    {
        return this.delegate.sumOfDouble(function);
    }

    @Override
    public String makeString()
    {
        return this.delegate.makeString();
    }

    @Override
    public String makeString(String separator)
    {
        return this.delegate.makeString(separator);
    }

    @Override
    public String makeString(String start, String separator, String end)
    {
        return this.delegate.makeString(start, separator, end);
    }

    @Override
    public void appendString(Appendable appendable)
    {
        this.delegate.appendString(appendable);
    }

    @Override
    public void appendString(Appendable appendable, String separator)
    {
        this.delegate.appendString(appendable, separator);
    }

    @Override
    public void appendString(Appendable appendable, String start, String separator, String end)
    {
        this.delegate.appendString(appendable, start, separator, end);
    }

    @Override
    public <VV> ImmutableBagMultimap<VV, V> groupBy(Function<? super V, ? extends VV> function)
    {
        MutableBagMultimap<VV, V> bagMultimap = this.delegate.groupBy(function);
        return bagMultimap.toImmutable();
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupBy(Function<? super V, ? extends VV> function, R target)
    {
        return this.delegate.groupBy(function, target);
    }

    @Override
    public <VV> ImmutableBagMultimap<VV, V> groupByEach(Function<? super V, ? extends Iterable<VV>> function)
    {
        return this.delegate.groupByEach(function).toImmutable();
    }

    @Override
    public <VV, R extends MutableMultimap<VV, V>> R groupByEach(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        return this.delegate.groupByEach(function, target);
    }

    @Override
    public <VV> ImmutableMap<VV, V> groupByUniqueKey(Function<? super V, ? extends VV> function)
    {
        MutableMap<VV, V> map = this.delegate.groupByUniqueKey(function);
        return map.toImmutable();
    }

    @Override
    public <VV, R extends MutableMapIterable<VV, V>> R groupByUniqueKey(Function<? super V, ? extends VV> function, R target)
    {
        return this.delegate.groupByUniqueKey(function, target);
    }

    @Override
    public <S> ImmutableBag<Pair<V, S>> zip(Iterable<S> that)
    {
        return this.delegate.zip(that).toImmutable();
    }

    @Override
    public <S, R extends Collection<Pair<V, S>>> R zip(Iterable<S> that, R target)
    {
        return this.delegate.zip(that, target);
    }

    @Override
    public ImmutableSet<Pair<V, Integer>> zipWithIndex()
    {
        return this.delegate.zipWithIndex().toImmutable();
    }

    @Override
    public <R extends Collection<Pair<V, Integer>>> R zipWithIndex(R target)
    {
        return this.delegate.zipWithIndex(target);
    }

    @Override
    public RichIterable<RichIterable<V>> chunk(int size)
    {
        return this.delegate.chunk(size);
    }

    @Override
    public <K, VV> ImmutableMap<K, VV> aggregateBy(Function<? super V, ? extends K> groupBy, Function0<? extends VV> zeroValueFactory, Function2<? super VV, ? super V, ? extends VV> nonMutatingAggregator)
    {
        MutableMap<K, VV> map = this.delegate.aggregateBy(groupBy, zeroValueFactory, nonMutatingAggregator);
        return map.toImmutable();
    }

    @Override
    public <VV extends Comparable<? super VV>> V maxBy(Function<? super V, ? extends VV> function)
    {
        return this.delegate.maxBy(function);
    }

    @Override
    public <VV extends Comparable<? super VV>> V minBy(Function<? super V, ? extends VV> function)
    {
        return this.delegate.minBy(function);
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableSortedSet<V> toSortedSetBy(Function<? super V, ? extends VV> function)
    {
        return this.delegate.toSortedSetBy(function);
    }

    @Override
    public <VV extends Comparable<? super VV>> MutableList<V> toSortedListBy(Function<? super V, ? extends VV> function)
    {
        return this.delegate.toSortedListBy(function);
    }

    @Override
    public <VV, R extends Collection<VV>> R flatCollect(Function<? super V, ? extends Iterable<VV>> function, R target)
    {
        return this.delegate.flatCollect(function, target);
    }

    @Override
    public <VV, R extends Collection<VV>> R collectIf(Predicate<? super V> predicate, Function<? super V, ? extends VV> function, R target)
    {
        return this.delegate.collectIf(predicate, function, target);
    }

    @Override
    public <P, VV, R extends Collection<VV>> R collectWith(Function2<? super V, ? super P, ? extends VV> function, P parameter, R targetCollection)
    {
        return this.delegate.collectWith(function, parameter, targetCollection);
    }

    @Override
    public <VV, R extends Collection<VV>> R collect(Function<? super V, ? extends VV> function, R target)
    {
        return this.delegate.collect(function, target);
    }

    @Override
    public ImmutableCharObjectMap<V> newWithKeyValue(char key, V value)
    {
        CharObjectHashMap<V> map = new CharObjectHashMap<>(this);
        map.put(key, value);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharObjectMap<V> newWithoutKey(char key)
    {
        CharObjectHashMap<V> map = new CharObjectHashMap<>(this);
        map.removeKey(key);
        return map.toImmutable();
    }

    @Override
    public ImmutableCharObjectMap<V> newWithoutAllKeys(CharIterable keys)
    {
        CharObjectHashMap<V> map = new CharObjectHashMap<>(this);
        CharIterator iterator = keys.charIterator();
        while (iterator.hasNext())
        {
            map.removeKey(iterator.next());
        }
        return map.toImmutable();
    }

    @Override
    public void forEach(Procedure<? super V> procedure)
    {
        this.each(procedure);
    }

    @Override
    public void each(Procedure<? super V> procedure)
    {
        this.delegate.forEach(procedure);
    }

    @Override
    public void forEachWithIndex(ObjectIntProcedure<? super V> objectIntProcedure)
    {
        this.delegate.forEachWithIndex(objectIntProcedure);
    }

    @Override
    public <P> void forEachWith(Procedure2<? super V, ? super P> procedure, P parameter)
    {
        this.delegate.forEachWith(procedure, parameter);
    }

    @Override
    public Iterator<V> iterator()
    {
        return new UnmodifiableIteratorAdapter<>(this.delegate.iterator());
    }

    @Override
    public MutableCharSet keySet()
    {
        return UnmodifiableCharSet.of(this.delegate.keySet());
    }

    @Override
    public Collection<V> values()
    {
        return UnmodifiableMutableCollection.of(this.delegate.values());
    }

    @Override
    public LazyCharIterable keysView()
    {
        return this.delegate.keysView();
    }

    @Override
    public RichIterable<CharObjectPair<V>> keyValuesView()
    {
        return this.delegate.keyValuesView();
    }

    @Override
    public ImmutableObjectCharMap<V> flipUniqueValues()
    {
        MutableObjectCharMap<V> result = ObjectCharMaps.mutable.empty();
        this.forEachKeyValue((key, value) ->
        {
            if (result.containsKey(value))
            {
                throw new IllegalStateException("Duplicate value: " + value + " found at key: " + result.get(value) + " and key: " + key);
            }
            result.put(value, key);
        });
        return result.toImmutable();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return this.delegate.toString();
    }

    private Object writeReplace()
    {
        return new ImmutableCharObjectMapSerializationProxy<V>(this);
    }
}
