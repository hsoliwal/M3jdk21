/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.sorted.immutable;

import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;

import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.RichIterable;
import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.function.Function2;
import jdk.m3.collections.eclipse.api.block.function.primitive.BooleanFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ByteFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongFunction;
import jdk.m3.collections.eclipse.api.block.function.primitive.ShortFunction;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate2;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.collection.MutableCollection;
import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.SortedSets;
import jdk.m3.collections.eclipse.api.factory.primitive.BooleanLists;
import jdk.m3.collections.eclipse.api.factory.primitive.ByteLists;
import jdk.m3.collections.eclipse.api.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.api.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.api.factory.primitive.FloatLists;
import jdk.m3.collections.eclipse.api.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.api.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.api.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.api.list.ImmutableList;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableCharList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableIntList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.ImmutableShortList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.multimap.sortedset.ImmutableSortedSetMultimap;
import jdk.m3.collections.eclipse.api.ordered.OrderedIterable;
import jdk.m3.collections.eclipse.api.partition.set.sorted.PartitionImmutableSortedSet;
import jdk.m3.collections.eclipse.api.partition.set.sorted.PartitionMutableSortedSet;
import jdk.m3.collections.eclipse.api.set.SetIterable;
import jdk.m3.collections.eclipse.api.set.sorted.ImmutableSortedSet;
import jdk.m3.collections.eclipse.api.set.sorted.MutableSortedSet;
import jdk.m3.collections.eclipse.api.set.sorted.ParallelSortedSetIterable;
import jdk.m3.collections.eclipse.api.set.sorted.SortedSetIterable;
import jdk.m3.collections.eclipse.api.stack.MutableStack;
import jdk.m3.collections.eclipse.api.tuple.Pair;
import jdk.m3.collections.eclipse.impl.block.factory.Comparators;
import jdk.m3.collections.eclipse.impl.block.factory.Functions;
import jdk.m3.collections.eclipse.impl.block.factory.Predicates;
import jdk.m3.collections.eclipse.impl.block.procedure.CollectIfProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.CollectProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.FlatCollectProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.PartitionPredicate2Procedure;
import jdk.m3.collections.eclipse.impl.block.procedure.PartitionProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.RejectProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.SelectInstancesOfProcedure;
import jdk.m3.collections.eclipse.impl.block.procedure.SelectProcedure;
import jdk.m3.collections.eclipse.impl.collection.immutable.AbstractImmutableCollection;
import jdk.m3.collections.eclipse.impl.lazy.parallel.set.sorted.NonParallelSortedSetIterable;
import jdk.m3.collections.eclipse.impl.list.mutable.FastList;
import jdk.m3.collections.eclipse.impl.multimap.set.sorted.TreeSortedSetMultimap;
import jdk.m3.collections.eclipse.impl.partition.set.sorted.PartitionTreeSortedSet;
import jdk.m3.collections.eclipse.impl.set.sorted.mutable.TreeSortedSet;
import jdk.m3.collections.eclipse.impl.stack.mutable.ArrayStack;
import jdk.m3.collections.eclipse.impl.utility.Iterate;
import jdk.m3.collections.eclipse.impl.utility.OrderedIterate;
import jdk.m3.collections.eclipse.impl.utility.internal.IterableIterate;
import jdk.m3.collections.eclipse.impl.utility.internal.SetIterables;
import jdk.m3.collections.eclipse.impl.utility.internal.SortedSetIterables;

/**
 * This class is the parent class for all ImmutableSortedSets. All implementations of ImmutableSortedSet must implement the SortedSet
 * interface so an TreeSet.equals(anImmutableSortedSet) can return true when the contents are the same.
 */
abstract class AbstractImmutableSortedSet<T> extends AbstractImmutableCollection<T>
        implements ImmutableSortedSet<T>, NavigableSet<T>
{
    @Override
    public SortedSet<T> castToSortedSet()
    {
        return this;
    }

    @Override
    public NavigableSet<T> castToNavigableSet()
    {
        return this;
    }

    @Override
    public ImmutableSortedSet<T> newWith(T element)
    {
        if (!this.contains(element))
        {
            return SortedSets.mutable.withAll(this).with(element).toImmutable();
        }
        return this;
    }

    @Override
    public ImmutableSortedSet<T> newWithout(T element)
    {
        if (this.contains(element))
        {
            MutableSortedSet<T> result = TreeSortedSet.newSet(this);
            result.remove(element);
            return result.toImmutable();
        }
        return this;
    }

    @Override
    public ImmutableSortedSet<T> newWithAll(Iterable<? extends T> elements)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this);
        result.addAllIterable(elements);
        return result.toImmutable();
    }

    @Override
    public ImmutableSortedSet<T> newWithoutAll(Iterable<? extends T> elements)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this);
        this.removeAllFrom(elements, result);
        return result.toImmutable();
    }

    @Override
    public T getFirst()
    {
        return this.first();
    }

    @Override
    public T getLast()
    {
        return this.last();
    }

    @Override
    protected MutableCollection<T> newMutable(int size)
    {
        return TreeSortedSet.newSet(this.comparator());
    }

    @Override
    public ImmutableBooleanList collectBoolean(BooleanFunction<? super T> booleanFunction)
    {
        MutableBooleanList result = BooleanLists.mutable.withInitialCapacity(this.size());
        return this.collectBoolean(booleanFunction, result).toImmutable();
    }

    @Override
    public ImmutableByteList collectByte(ByteFunction<? super T> byteFunction)
    {
        MutableByteList result = ByteLists.mutable.withInitialCapacity(this.size());
        return this.collectByte(byteFunction, result).toImmutable();
    }

    @Override
    public ImmutableCharList collectChar(CharFunction<? super T> charFunction)
    {
        MutableCharList result = CharLists.mutable.withInitialCapacity(this.size());
        return this.collectChar(charFunction, result).toImmutable();
    }

    @Override
    public ImmutableDoubleList collectDouble(DoubleFunction<? super T> doubleFunction)
    {
        MutableDoubleList result = DoubleLists.mutable.withInitialCapacity(this.size());
        return this.collectDouble(doubleFunction, result).toImmutable();
    }

    @Override
    public ImmutableFloatList collectFloat(FloatFunction<? super T> floatFunction)
    {
        MutableFloatList result = FloatLists.mutable.withInitialCapacity(this.size());
        return this.collectFloat(floatFunction, result).toImmutable();
    }

    @Override
    public ImmutableIntList collectInt(IntFunction<? super T> intFunction)
    {
        MutableIntList result = IntLists.mutable.withInitialCapacity(this.size());
        return this.collectInt(intFunction, result).toImmutable();
    }

    @Override
    public ImmutableLongList collectLong(LongFunction<? super T> longFunction)
    {
        MutableLongList result = LongLists.mutable.withInitialCapacity(this.size());
        return this.collectLong(longFunction, result).toImmutable();
    }

    @Override
    public ImmutableShortList collectShort(ShortFunction<? super T> shortFunction)
    {
        MutableShortList result = ShortLists.mutable.withInitialCapacity(this.size());
        return this.collectShort(shortFunction, result).toImmutable();
    }

    @Override
    public ImmutableSortedSet<T> tap(Procedure<? super T> procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    public <S> boolean corresponds(OrderedIterable<S> other, Predicate2<? super T, ? super S> predicate)
    {
        return OrderedIterate.corresponds(this, other, predicate);
    }

    @Override
    public ImmutableSortedSet<T> select(Predicate<? super T> predicate)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this.comparator());
        this.forEach(new SelectProcedure<>(predicate, result));
        return result.toImmutable();
    }

    @Override
    public <P> ImmutableSortedSet<T> selectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return this.select(Predicates.bind(predicate, parameter));
    }

    @Override
    public ImmutableSortedSet<T> reject(Predicate<? super T> predicate)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this.comparator());
        this.forEach(new RejectProcedure<>(predicate, result));
        return result.toImmutable();
    }

    @Override
    public <P> ImmutableSortedSet<T> rejectWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        return this.reject(Predicates.bind(predicate, parameter));
    }

    @Override
    public PartitionImmutableSortedSet<T> partition(Predicate<? super T> predicate)
    {
        PartitionMutableSortedSet<T> partitionTreeSortedSet = new PartitionTreeSortedSet<>(this.comparator());
        this.forEach(new PartitionProcedure<>(predicate, partitionTreeSortedSet));
        return partitionTreeSortedSet.toImmutable();
    }

    @Override
    public <P> PartitionImmutableSortedSet<T> partitionWith(Predicate2<? super T, ? super P> predicate, P parameter)
    {
        PartitionMutableSortedSet<T> partitionTreeSortedSet = new PartitionTreeSortedSet<>(this.comparator());
        this.forEach(new PartitionPredicate2Procedure<>(predicate, parameter, partitionTreeSortedSet));
        return partitionTreeSortedSet.toImmutable();
    }

    @Override
    public PartitionImmutableSortedSet<T> partitionWhile(Predicate<? super T> predicate)
    {
        PartitionTreeSortedSet<T> result = new PartitionTreeSortedSet<>(this.comparator());
        return IterableIterate.partitionWhile(this, predicate, result).toImmutable();
    }

    @Override
    public <S> ImmutableSortedSet<S> selectInstancesOf(Class<S> clazz)
    {
        MutableSortedSet<S> result = TreeSortedSet.newSet((Comparator<? super S>) this.comparator());
        this.forEach(new SelectInstancesOfProcedure<>(clazz, result));
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableList<V> collect(Function<? super T, ? extends V> function)
    {
        MutableList<V> result = Lists.mutable.empty();
        this.forEach(new CollectProcedure<>(function, result));
        return result.toImmutable();
    }

    @Override
    public <P, V> ImmutableList<V> collectWith(Function2<? super T, ? super P, ? extends V> function, P parameter)
    {
        return this.collect(Functions.bind(function, parameter));
    }

    @Override
    public <V> ImmutableList<V> collectIf(Predicate<? super T> predicate, Function<? super T, ? extends V> function)
    {
        MutableList<V> result = Lists.mutable.empty();
        this.forEach(new CollectIfProcedure<>(result, function, predicate));
        return result.toImmutable();
    }

    @Override
    public <V> ImmutableList<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
    {
        MutableList<V> result = Lists.mutable.empty();
        this.forEach(new FlatCollectProcedure<>(function, result));
        return result.toImmutable();
    }

    @Override
    public int detectIndex(Predicate<? super T> predicate)
    {
        return Iterate.detectIndex(this, predicate);
    }

    @Override
    public <V> ImmutableSortedSetMultimap<V, T> groupBy(Function<? super T, ? extends V> function)
    {
        return this.groupBy(function, TreeSortedSetMultimap.<V, T>newMultimap(this.comparator())).toImmutable();
    }

    @Override
    public <V> ImmutableSortedSetMultimap<V, T> groupByEach(Function<? super T, ? extends Iterable<V>> function)
    {
        return this.groupByEach(function, TreeSortedSetMultimap.newMultimap(this.comparator())).toImmutable();
    }

    @Override
    public <S> ImmutableList<Pair<T, S>> zip(Iterable<S> that)
    {
        if (that instanceof Collection || that instanceof RichIterable)
        {
            int thatSize = Iterate.sizeOf(that);
            MutableList<Pair<T, S>> target = FastList.newList(Math.min(this.size(), thatSize));
            return Iterate.zip(this, that, target).toImmutable();
        }
        return Iterate.zip(this, that, FastList.newList()).toImmutable();
    }

    @Override
    public ImmutableSortedSet<Pair<T, Integer>> zipWithIndex()
    {
        Comparator<? super T> comparator = this.comparator();
        if (comparator == null)
        {
            MutableSortedSet<Pair<T, Integer>> pairs = TreeSortedSet.newSet(Comparators.byFunction(Functions.firstOfPair(), Comparators.naturalOrder()));
            return Iterate.zipWithIndex(this, pairs).toImmutable();
        }
        return Iterate.zipWithIndex(this, TreeSortedSet.newSet(Comparators.byFirstOfPair(comparator))).toImmutable();
    }

    @Override
    public ImmutableSortedSet<T> distinct()
    {
        return this;
    }

    @Override
    public ImmutableSortedSet<T> takeWhile(Predicate<? super T> predicate)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this.comparator());
        return IterableIterate.takeWhile(this, predicate, result).toImmutable();
    }

    @Override
    public ImmutableSortedSet<T> dropWhile(Predicate<? super T> predicate)
    {
        MutableSortedSet<T> result = TreeSortedSet.newSet(this.comparator());
        return IterableIterate.dropWhile(this, predicate, result).toImmutable();
    }

    @Override
    public MutableStack<T> toStack()
    {
        return ArrayStack.newStack(this);
    }

    @Override
    public ImmutableSortedSet<T> union(SetIterable<? extends T> set)
    {
        return SetIterables.unionInto(this, set, TreeSortedSet.newSet(this.comparator())).toImmutable();
    }

    @Override
    public <R extends Set<T>> R unionInto(SetIterable<? extends T> set, R targetSet)
    {
        return SetIterables.unionInto(this, set, targetSet);
    }

    @Override
    public ImmutableSortedSet<T> intersect(SetIterable<? extends T> set)
    {
        return SetIterables.intersectInto(this, set, TreeSortedSet.newSet(this.comparator())).toImmutable();
    }

    @Override
    public <R extends Set<T>> R intersectInto(SetIterable<? extends T> set, R targetSet)
    {
        return SetIterables.intersectInto(this, set, targetSet);
    }

    @Override
    public ImmutableSortedSet<T> difference(SetIterable<? extends T> subtrahendSet)
    {
        return SetIterables.differenceInto(this, subtrahendSet, TreeSortedSet.newSet(this.comparator())).toImmutable();
    }

    @Override
    public <R extends Set<T>> R differenceInto(SetIterable<? extends T> subtrahendSet, R targetSet)
    {
        return SetIterables.differenceInto(this, subtrahendSet, targetSet);
    }

    @Override
    public ImmutableSortedSet<T> symmetricDifference(SetIterable<? extends T> setB)
    {
        return SetIterables.symmetricDifferenceInto(this, setB, TreeSortedSet.newSet(this.comparator())).toImmutable();
    }

    @Override
    public <R extends Set<T>> R symmetricDifferenceInto(SetIterable<? extends T> set, R targetSet)
    {
        return SetIterables.symmetricDifferenceInto(this, set, targetSet);
    }

    @Override
    public boolean isSubsetOf(SetIterable<? extends T> candidateSuperset)
    {
        return SetIterables.isSubsetOf(this, candidateSuperset);
    }

    @Override
    public boolean isProperSubsetOf(SetIterable<? extends T> candidateSuperset)
    {
        return SetIterables.isProperSubsetOf(this, candidateSuperset);
    }

    @Override
    public ImmutableSortedSet<SortedSetIterable<T>> powerSet()
    {
        return (ImmutableSortedSet<SortedSetIterable<T>>) (ImmutableSortedSet<?>) SortedSetIterables.immutablePowerSet(this);
    }

    @Override
    public <B> LazyIterable<Pair<T, B>> cartesianProduct(SetIterable<B> set)
    {
        return SetIterables.cartesianProduct(this, set);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement)
    {
        return this.subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement)
    {
        return this.headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement)
    {
        return this.tailSet(fromElement, true);
    }

    @Override
    public ImmutableSortedSet<T> toImmutable()
    {
        return this;
    }

    @Override
    public ParallelSortedSetIterable<T> asParallel(ExecutorService executorService, int batchSize)
    {
        Objects.requireNonNull(executorService);
        if (batchSize < 1)
        {
            throw new IllegalArgumentException("batchSize must be greater than zero, but was: " + batchSize);
        }
        return new NonParallelSortedSetIterable<>(this);
    }

    @Override
    public abstract AbstractImmutableSortedSet<T> descendingSet();

    @Override
    public abstract AbstractImmutableSortedSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive);

    @Override
    public abstract AbstractImmutableSortedSet<T> headSet(T toElement, boolean inclusive);

    @Override
    public abstract AbstractImmutableSortedSet<T> tailSet(T fromElement, boolean inclusive);

    @Override
    public ImmutableSortedSet<T> toReversed()
    {
        return this.descendingSet();
    }

    @Override
    public T pollFirst()
    {
        throw new UnsupportedOperationException("Cannot call pollFirst() on " + this.getClass().getSimpleName());
    }

    @Override
    public T pollLast()
    {
        throw new UnsupportedOperationException("Cannot call pollLast() on " + this.getClass().getSimpleName());
    }
}
