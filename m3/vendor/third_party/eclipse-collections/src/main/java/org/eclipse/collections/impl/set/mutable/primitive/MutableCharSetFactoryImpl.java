/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.mutable.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.set.primitive.MutableCharSetFactory;
import org.eclipse.collections.api.set.primitive.MutableCharSet;
import org.eclipse.collections.impl.factory.primitive.CharSets;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharSetFactoryImpl is a factory implementation which creates instances of type {@link MutableCharSet}.
 * This file was automatically generated from template file mutablePrimitiveSetFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharSetFactoryImpl implements MutableCharSetFactory
{
    public static final MutableCharSetFactory INSTANCE = new MutableCharSetFactoryImpl();

    @Override
    public MutableCharSet empty()
    {
        return new CharHashSet();
    }

    @Override
    public MutableCharSet of()
    {
        return this.empty();
    }

    @Override
    public MutableCharSet with()
    {
        return this.empty();
    }

    @Override
    public MutableCharSet withInitialCapacity(int capacity)
    {
        return new CharHashSet(capacity);
    }

    @Override
    public MutableCharSet of(char... items)
    {
        return this.with(items);
    }

    @Override
    public MutableCharSet with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.empty();
        }
        return CharHashSet.newSetWith(items);
    }

    @Override
    public MutableCharSet ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableCharSet withAll(CharIterable items)
    {
        return CharHashSet.newSet(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharSet ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharSet withAll(Iterable<Character> iterable)
    {
        return Iterate.collectChar(iterable, Character::charValue, CharSets.mutable.empty());
    }
}
