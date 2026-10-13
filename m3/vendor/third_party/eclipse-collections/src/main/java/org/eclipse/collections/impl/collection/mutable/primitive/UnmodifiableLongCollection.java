/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.collection.mutable.primitive;

import org.eclipse.collections.api.collection.primitive.MutableLongCollection;
import org.eclipse.collections.impl.bag.mutable.primitive.LongHashBag;

/**
 * This file was automatically generated from template file unmodifiablePrimitiveCollection.stg.
 *
 * @since 4.0.
 */
public class UnmodifiableLongCollection
        extends AbstractUnmodifiableLongCollection
{
    private static final long serialVersionUID = 1L;

    protected UnmodifiableLongCollection(MutableLongCollection collection)
    {
        super(collection);
    }

    /**
     * This method will take a MutableLongCollection and wrap it directly in a UnmodifiableLongCollection.
     */
    public static UnmodifiableLongCollection of(MutableLongCollection collection)
    {
        if (collection == null)
        {
            throw new IllegalArgumentException("cannot create a UnmodifiableLongCollection for null");
        }
        return new UnmodifiableLongCollection(collection);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableLongCollection newEmpty()
    {
        return new LongHashBag();
    }
}
