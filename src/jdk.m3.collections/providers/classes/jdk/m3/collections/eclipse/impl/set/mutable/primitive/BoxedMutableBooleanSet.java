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
import jdk.m3.collections.eclipse.api.iterator.MutableBooleanIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.ParallelUnsortedSetIterable;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.impl.set.mutable.AbstractMutableSet;

/**
 * A boxed view of a {@link MutableBooleanSet}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file  boxedPrimitiveSet.stg.
 *
 * @since 12.0
 */
@SuppressWarnings("deprecation")
public class BoxedMutableBooleanSet extends AbstractMutableSet<Boolean>
        implements MutableSet<Boolean>
{
    private final MutableBooleanSet delegate;

    public BoxedMutableBooleanSet(MutableBooleanSet delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public Boolean getFirst()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getLast()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void each(Procedure<? super Boolean> procedure)
    {
        this.delegate.each(procedure::value);
    }

    @Override
    public boolean add(Boolean item)
    {
        return this.delegate.add(item.booleanValue());
    }

    @Override
    public boolean contains(Object object)
    {
        return object instanceof Boolean && this.delegate.contains(((Boolean) object).booleanValue());
    }

    @Override
    public boolean remove(Object object)
    {
        return object instanceof Boolean && this.delegate.remove(((Boolean) object).booleanValue());
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public ParallelUnsortedSetIterable<Boolean> asParallel(ExecutorService executorService, int batchSize)
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
    public Iterator<Boolean> iterator()
    {
        return new BoxedPrimitiveBooleanSetIterator(this.delegate.booleanIterator());
    }

    private static final class BoxedPrimitiveBooleanSetIterator implements Iterator<Boolean>
    {
        private final MutableBooleanIterator delegate;

        private BoxedPrimitiveBooleanSetIterator(MutableBooleanIterator iterator)
        {
            this.delegate = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return this.delegate.hasNext();
        }

        @Override
        public Boolean next()
        {
            return this.delegate.next();
        }
    }
}
