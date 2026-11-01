/*
 * Copyright (c) 2021 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.bag;

import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableBooleanBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableByteBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableCharBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableDoubleBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableFloatBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableIntBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableLongBag;
import jdk.m3.collections.eclipse.api.bag.primitive.ImmutableShortBag;
import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.Function2;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ObjectIntToObjectFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.IntPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.multimap.bag.ImmutableBagMultimap;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.api.partition.bag.PartitionImmutableBag;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.tuple.Pair;
import jdk.m3.collections.eclipse.api.tuple.primitive.ObjectIntPair;

/**
 * @since 1.0
 */
public interface ImmutableBag<T> extends UnsortedBag<T>, ImmutableBagIterable<T>
{
    @Override
    ImmutableBag<T> newWith(T element);

    @Override
    ImmutableBag<T> newWithout(T element);

    @Override
    ImmutableBag<T> newWithAll(Iterable<? extends T> elements);

    @Override
    ImmutableBag<T> newWithoutAll(Iterable<? extends T> elements);

    @Override
    ImmutableBag<T> selectByOccurrences(IntPredicate predicate);

    /**
     * @since 9.2
     */
    @Override
    default ImmutableBag<T> selectDuplicates()
    {
        return this.selectByOccurrences(occurrences -> occurrences > 1);
    }

    /**
     * @since 9.2
     */
    @Override
    default ImmutableSet<T> selectUnique()
    {
        MutableSet<T> result = Sets.mutable.empty();
        this.forEachWithOccurrences((each, occurrences) ->
        {
            if (occurrences == 1)
            {
                result.add(each);
            }
        });
        return result.toImmutable();
    }

    @Override
    ImmutableBag<T> tap(Procedure<? super T> procedure);

    @Override
    ImmutableBag<T> select(Predicate<? super T> predicate);

    @Override
    <P> ImmutableBag<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter);

    @Override
    ImmutableBag<T> reject(Predicate<? super T> predicate);

    @Override
    <P> ImmutableBag<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter);

    @Override
    PartitionImmutableBag<T> partition(Predicate<? super T> predicate);

    @Override
    <P> PartitionImmutableBag<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter);

    @Override
    <S> ImmutableBag<S> selectInstancesOf(Class<S> clazz);

    @Override
    <V> ImmutableBag<V> collect(Function<? super T, ? extends V> function);

    @Override
    ImmutableBooleanBag collectBoolean(BooleanFunction<? super T> booleanFunction);

    @Override
    ImmutableByteBag collectByte(ByteFunction<? super T> byteFunction);

    @Override
    ImmutableCharBag collectChar(CharFunction<? super T> charFunction);

    @Override
    ImmutableDoubleBag collectDouble(DoubleFunction<? super T> doubleFunction);

    @Override
    ImmutableFloatBag collectFloat(FloatFunction<? super T> floatFunction);

    @Override
    ImmutableIntBag collectInt(IntFunction<? super T> intFunction);

    @Override
    ImmutableLongBag collectLong(LongFunction<? super T> longFunction);

    @Override
    ImmutableShortBag collectShort(ShortFunction<? super T> shortFunction);

    @Override
    <P, V> ImmutableBag<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter);

    @Override
    <V> ImmutableBag<V> collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function);

    @Override
    <V> ImmutableBag<V> collectWithOccurrences(ObjectIntToObjectFunction<? super T, ? extends V> function);

    @Override
    <V> ImmutableBag<V> flatCollect(Function<? super T, ? extends Iterable<V>> function);

    /**
     * @since 9.2
     */
    @Override
    default <P, V> ImmutableBag<V> flatCollectWith(Function2<? super T, ? super P, ? extends Iterable<V>> function, P parameter)
    {
        return this.flatCollect(each -> function.apply(each, parameter));
    }

    /**
     * @since 9.0
     */
    @Override
    default <V> ImmutableBag<V> countBy(Function<? super T, ? extends V> function)
    {
        return this.collect(function);
    }

    /**
     * @since 9.0
     */
    @Override
    default <V, P> ImmutableBag<V> countByWith(Function2<? super T, ? super P, ? extends V> function, P parameter)
    {
        return this.collectWith(function, parameter);
    }

    /**
     * @since 10.0.0
     */
    @Override
    default <V> ImmutableBag<V> countByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return this.flatCollect(function);
    }

    @Override
    <V> ImmutableBagMultimap<V, T> groupBy(Function<? super T, ? extends V> function);

    @Override
    <V> ImmutableBagMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function);

    /**
     * @deprecated in 6.0. Use {@link OrderedIterable#zip(Iterable)} instead.
     */
    @Override
    @Deprecated
    <S> ImmutableBag<Pair<T, S>> zip(Iterable<S> that);

    /**
     * @deprecated in 6.0. Use {@link OrderedIterable#zipWithIndex()} instead.
     */
    @Override
    @Deprecated
    ImmutableSet<Pair<T, Integer>> zipWithIndex();

    /**
     * @since 6.0
     */
    @Override
    ImmutableList<ObjectIntPair<T>> topOccurrences(int count);

    /**
     * @since 6.0
     */
    @Override
    ImmutableList<ObjectIntPair<T>> bottomOccurrences(int count);

    /**
     * Overrides toImmutableBag in RichIterable to return this.
     *
     * @since 11.0
     */
    @Override
    default ImmutableBag<T> toImmutableBag()
    {
        return this;
    }
}
