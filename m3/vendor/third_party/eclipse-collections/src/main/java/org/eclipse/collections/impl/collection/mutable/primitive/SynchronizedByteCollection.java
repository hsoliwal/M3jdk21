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

import org.eclipse.collections.api.collection.primitive.MutableByteCollection;
import org.eclipse.collections.impl.bag.mutable.primitive.ByteHashBag;

/**
 * This file was automatically generated from template file synchronizedPrimitiveCollection.stg.
 *
 * @since 4.0.
 */
public class SynchronizedByteCollection
        extends AbstractSynchronizedByteCollection
{
    private static final long serialVersionUID = 1L;

    protected SynchronizedByteCollection(MutableByteCollection collection)
    {
        this(collection, null);
    }

    protected SynchronizedByteCollection(MutableByteCollection collection, Object newLock)
    {
        super(collection, newLock);
    }

    /**
     * This method will take a MutableByteCollection and wrap it directly in a SynchronizedByteCollection.
     */
    public static SynchronizedByteCollection of(MutableByteCollection collection)
    {
        return new SynchronizedByteCollection(collection);
    }

    /**
     * This method will take a MutableByteCollection and wrap it directly in a SynchronizedByteCollection.
     * Additionally, a developer specifies which lock to use with the collection.
     */
    public static SynchronizedByteCollection of(MutableByteCollection collection, Object lock)
    {
        return new SynchronizedByteCollection(collection, lock);
    }

    /**
     * @since 9.2.
     */
    @Override
    public MutableByteCollection newEmpty()
    {
        return new ByteHashBag();
    }
}
