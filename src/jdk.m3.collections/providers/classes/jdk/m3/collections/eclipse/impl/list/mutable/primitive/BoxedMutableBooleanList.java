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
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.impl.list.mutable.AbstractMutableList;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * A boxed view of a {@link MutableBooleanList}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file boxedPrimitiveList.stg.
 *
 * @since 12.0
 */
public class BoxedMutableBooleanList extends AbstractMutableList<Boolean>
        implements MutableList<Boolean>, RandomAccess
{
    private final MutableBooleanList delegate;

    public BoxedMutableBooleanList(MutableBooleanList delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean add(Boolean item)
    {
        return this.delegate.add(item.booleanValue());
    }

    @Override
    public boolean addAll(int index, Collection<? extends Boolean> c)
    {
        boolean[] target = new boolean[c.size()];
        Iterate.forEachWithIndex(c, (each, parameter) -> target[parameter] = each.booleanValue());
        return this.delegate.addAllAtIndex(index, target);
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public Boolean get(int index)
    {
        return this.delegate.get(index);
    }

    @Override
    public Boolean set(int index, Boolean element)
    {
        return this.delegate.set(index, element.booleanValue());
    }

    @Override
    public void add(int index, Boolean element)
    {
        this.delegate.addAtIndex(index, element.booleanValue());
    }

    @Override
    public Boolean remove(int index)
    {
        return this.delegate.removeAtIndex(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return o instanceof Boolean ? this.delegate.indexOf((Boolean) o) : -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return o instanceof Boolean ? this.delegate.lastIndexOf((Boolean) o) : -1;
    }

    @Override
    public MutableList<Boolean> subList(int fromIndex, int toIndex)
    {
        return this.delegate.subList(fromIndex, toIndex).boxed();
    }
}
