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

import org.eclipse.collections.api.collection.primitive.MutableFloatCollection;
import org.eclipse.collections.impl.bag.mutable.primitive.FloatHashBag;

/**
 * This file was automatically generated from template file synchronizedPrimitiveCollection.stg.
 *
 * @since 4.0.
 */
public class SynchronizedFloatCollection
        extends AbstractSynchronizedFloatCollection
{
    private static final long serialVersionUID = 1L;

    protected SynchronizedFloatCollection(MutableFloatCollection collection)
    {
        this(collection, null);
    }

    protected SynchronizedFloatCollection(MutableFloatCollection collection, Object newLock)
    {
        super(collection, newLock);
    }

    /**
     * This method will take a MutableFloatCollection and wrap it directly in a SynchronizedFloatCollection.
     */
    public static SynchronizedFloatCollection of(MutableFloatCollection collection)
    {
        return new SynchronizedFloatCollection(collection);
    }

    /**
     * This method will take a MutableFloatCollection and wrap it directly in a SynchronizedFloatCollection.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedFloatCollection of(MutableFloatCollection collection, Object lock)
    {
        return new SynchronizedFloatCollection(collection, lock);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableFloatCollection newEmpty()
    {
        return new FloatHashBag();
    }
}
