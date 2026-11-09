/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableCharListFactory;
import org.eclipse.collections.api.list.primitive.MutableCharList;
import org.eclipse.collections.impl.factory.primitive.CharLists;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharListFactoryImpl is a factory implementation which creates instances of type {@link MutableCharList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharListFactoryImpl implements MutableCharListFactory
{
    public static final MutableCharListFactory INSTANCE = new MutableCharListFactoryImpl();

    @Override
    public MutableCharList empty()
    {
        return new CharArrayList();
    }

    @Override
    public MutableCharList of()
    {
        return this.empty();
    }

    @Override
    public MutableCharList with()
    {
        return this.empty();
    }

    @Override
    public MutableCharList withInitialCapacity(int capacity)
    {
        return new CharArrayList(capacity);
    }

    @Override
    public MutableCharList of(char... items)
    {
        return this.with(items);
    }

    /**
     * Creates a new list using the passed {@code items} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    @Override
    public MutableCharList with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return CharArrayList.newListWith(items);
    }

    @Override
    public MutableCharList ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableCharList withAll(CharIterable items)
    {
        return CharArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharList ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharList withAll(Iterable<Character> iterable)
    {
        return Iterate.collectChar(iterable, Character::charValue, CharLists.mutable.empty());
    }
}
