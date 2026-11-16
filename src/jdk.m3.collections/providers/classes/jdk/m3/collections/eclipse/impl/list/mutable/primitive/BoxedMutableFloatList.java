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
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.impl.list.mutable.AbstractMutableList;
import jdk.m3.collections.eclipse.impl.utility.Iterate;

/**
 * A boxed view of a {@link MutableFloatList}.
 *
 * This is for scenarios where optimization is not a consideration and therefore no further methods will be optimized.
 * <p>
 * This file was automatically generated from template file boxedPrimitiveList.stg.
 *
 * @since 12.0
 */
public class BoxedMutableFloatList extends AbstractMutableList<Float>
        implements MutableList<Float>, RandomAccess
{
    private final MutableFloatList delegate;

    public BoxedMutableFloatList(MutableFloatList delegate)
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public int size()
    {
        return this.delegate.size();
    }

    @Override
    public boolean add(Float item)
    {
        return this.delegate.add(item.floatValue());
    }

    @Override
    public boolean addAll(int index, Collection<? extends Float> c)
    {
        float[] target = new float[c.size()];
        Iterate.forEachWithIndex(c, (each, parameter) -> target[parameter] = each.floatValue());
        return this.delegate.addAllAtIndex(index, target);
    }

    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    @Override
    public Float get(int index)
    {
        return this.delegate.get(index);
    }

    @Override
    public Float set(int index, Float element)
    {
        return this.delegate.set(index, element.floatValue());
    }

    @Override
    public void add(int index, Float element)
    {
        this.delegate.addAtIndex(index, element.floatValue());
    }

    @Override
    public Float remove(int index)
    {
        return this.delegate.removeAtIndex(index);
    }

    @Override
    public int indexOf(Object o)
    {
        return o instanceof Float ? this.delegate.indexOf((Float) o) : -1;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return o instanceof Float ? this.delegate.lastIndexOf((Float) o) : -1;
    }

    @Override
    public MutableList<Float> subList(int fromIndex, int toIndex)
    {
        return this.delegate.subList(fromIndex, toIndex).boxed();
    }
}
