/*
 * Copyright (c) 2026 The Bank of New York Mellon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.list.mutable.primitive;

import java.util.Collection;
import java.util.Objects;
import java.util.RandomAccess;

import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.impl.list.mutable.AbstractMutableList;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * A boxed view of a {@link MutableLongList}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file boxedPrimitiveList.stg.
 *
 * @since 12.0
 */
public class BoxedMutableLongList extends AbstractMutableList<Long>
        implements MutableList<Long>, RandomAccess
{
    private final MutableLongList delegate;

    public BoxedMutableLongList(MutableLongList delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean add(Long item)
    {
        return this.delegate.add(item.longValue());
    }

    @Override
    public boolean addAll(int index, Collection<? extends Long> c)
    {
        long[] target = new long[c.size()];
        Iterate.forEachWithIndex(c, (each, parameter) -> target[parameter] = each.longValue());
        return this.delegate.addAllAtIndex(index, target);
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public Long get(int index)
    {
        return this.delegate.get(index);
    }

    @Override
    public Long set(int index, Long element)
    {
        return this.delegate.set(index, element.longValue());
    }

    @Override
    public void add(int index, Long element)
    {
        this.delegate.addAtIndex(index, element.longValue());
    }

    @Override
    public Long remove(int index)
    {
        return this.delegate.removeAtIndex(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return o instanceof Long ? this.delegate.indexOf((Long) o) : -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return o instanceof Long ? this.delegate.lastIndexOf((Long) o) : -1;
    }

    @Override
    public MutableList<Long> subList(int fromIndex, int toIndex)
    {
        return this.delegate.subList(fromIndex, toIndex).boxed();
    }
}
