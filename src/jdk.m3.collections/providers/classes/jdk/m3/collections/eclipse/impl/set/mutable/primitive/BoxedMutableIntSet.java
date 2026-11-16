/*
 * Copyright (c) 2026 The Bank of New York Mellon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.mutable.primitive;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.api.iterator.MutableIntIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.ParallelUnsortedSetIterable;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.impl.set.mutable.AbstractMutableSet;

/**
 * A boxed view of a {@link MutableIntSet}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file  boxedPrimitiveSet.stg.
 *
 * @since 12.0
 */
@SuppressWarnings("deprecation")
public class BoxedMutableIntSet extends AbstractMutableSet<Integer>
        implements MutableSet<Integer>
{
    private final MutableIntSet delegate;

    public BoxedMutableIntSet(MutableIntSet delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public Integer getFirst()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getLast()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void each(Procedure<? super Integer> procedure)
    {
        this.delegate.each(procedure::value);
    }

    @Override
    public boolean add(Integer item)
    {
        return this.delegate.add(item.intValue());
    }

    @Override
    public boolean contains(Object object)
    {
        return object instanceof Integer && this.delegate.contains(((Integer) object).intValue());
    }

    @Override
    public boolean remove(Object object)
    {
        return object instanceof Integer && this.delegate.remove(((Integer) object).intValue());
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public ParallelUnsortedSetIterable<Integer> asParallel(ExecutorService executorService, int batchSize)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }

        if (!(object instanceof Set))
        {
            return false;
        }

        Set<?> other = (Set<?>) object;
        return this.size() == other.size() && this.containsAll(other);
    }

    @Override
    public Iterator<Integer> iterator()
    {
        return new BoxedPrimitiveIntSetIterator(this.delegate.intIterator());
    }

    private static final class BoxedPrimitiveIntSetIterator implements Iterator<Integer>
    {
        private final MutableIntIterator delegate;

        private BoxedPrimitiveIntSetIterator(MutableIntIterator iterator)
        {
            this.delegate = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return this.delegate.hasNext();
        }

        @Override
        public Integer next()
        {
            return this.delegate.next();
        }
    }
}
