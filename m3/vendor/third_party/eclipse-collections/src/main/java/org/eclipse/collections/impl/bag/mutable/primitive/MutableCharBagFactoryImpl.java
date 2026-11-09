/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.mutable.primitive;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableCharBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableCharBag;
import org.eclipse.collections.impl.factory.primitive.CharBags;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableCharBagFactoryImpl is a factory implementation which creates instances of type {@link MutableCharBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableCharBagFactoryImpl implements MutableCharBagFactory
{
    public static final MutableCharBagFactory INSTANCE = new MutableCharBagFactoryImpl();

    @Override
    public MutableCharBag empty()
    {
        return new CharHashBag();
    }

    @Override
    public MutableCharBag of()
    {
        return this.empty();
    }

    @Override
    public MutableCharBag with()
    {
        return this.empty();
    }

    @Override
    public MutableCharBag of(char... items)
    {
        return this.with(items);
    }

    @Override
    public MutableCharBag with(char... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return CharHashBag.newBagWith(items);
    }

    @Override
    public MutableCharBag ofAll(CharIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableCharBag withAll(CharIterable items)
    {
        return CharHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharBag ofAll(Iterable<Character> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableCharBag withAll(Iterable<Character> iterable)
    {
        return Iterate.collectChar(iterable, Character::charValue, CharBags.mutable.empty());
    }
}
