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
import jdk.m3.collections.eclipse.api.iterator.MutableShortIterator;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.ParallelUnsortedSetIterable;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.set.mutable.AbstractMutableSet;

/**
 * A boxed view of a {@link MutableShortSet}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file  boxedPrimitiveSet.stg.
 *
 * @since 12.0
 */
@SuppressWarnings("deprecation")
public class BoxedMutableShortSet extends AbstractMutableSet<Short>
        implements MutableSet<Short>
{
    private final MutableShortSet delegate;

    public BoxedMutableShortSet(MutableShortSet delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public Short getFirst()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Short getLast()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void each(Procedure<? super Short> procedure)
    {
        this.delegate.each(procedure::value);
    }

    @Override
    public boolean add(Short item)
    {
        return this.delegate.add(item.shortValue());
    }

    @Override
    public boolean contains(Object object)
    {
        return object instanceof Short && this.delegate.contains(((Short) object).shortValue());
    }

    @Override
    public boolean remove(Object object)
    {
        return object instanceof Short && this.delegate.remove(((Short) object).shortValue());
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public ParallelUnsortedSetIterable<Short> asParallel(ExecutorService executorService, int batchSize)
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
    public Iterator<Short> iterator()
    {
        return new BoxedPrimitiveShortSetIterator(this.delegate.shortIterator());
    }

    private static final class BoxedPrimitiveShortSetIterator implements Iterator<Short>
    {
        private final MutableShortIterator delegate;

        private BoxedPrimitiveShortSetIterator(MutableShortIterator iterator)
        {
            this.delegate = iterator;
        }

        @Override
        public boolean hasNext()
        {
            return this.delegate.hasNext();
        }

        @Override
        public Short next()
        {
            return this.delegate.next();
        }
    }
}
