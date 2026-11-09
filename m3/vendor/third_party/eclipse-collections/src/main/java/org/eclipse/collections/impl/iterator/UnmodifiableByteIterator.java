/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.iterator;

import org.eclipse.collections.api.iterator.ByteIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;

/**
 * UnmodifiableByteIterator is a wrapper around ByteIterator which is unmodifiable and doesn't support remove.
 * This file was automatically generated from template file unmodifiablePrimitiveIterator.stg.
 */
public class UnmodifiableByteIterator implements MutableByteIterator
{
    private final ByteIterator byteIterator;

    public UnmodifiableByteIterator(ByteIterator byteIterator)
    {
        this.byteIterator = byteIterator;
    }

    @Override
    public boolean hasNext()
    {
        return this.byteIterator.hasNext();
    }

    @Override
    public byte next()
    {
        return this.byteIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }
}
