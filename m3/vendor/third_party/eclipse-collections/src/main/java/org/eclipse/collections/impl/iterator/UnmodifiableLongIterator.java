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

import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableLongIterator;

/**
 * UnmodifiableLongIterator is a wrapper around LongIterator which is unmodifiable and doesn't support remove.
 * This file was automatically generated from template file unmodifiablePrimitiveIterator.stg.
 */
public class UnmodifiableLongIterator implements MutableLongIterator
{
    private final LongIterator longIterator;

    public UnmodifiableLongIterator(LongIterator longIterator)
    {
        this.longIterator = longIterator;
    }

    @Override
    public boolean hasNext()
    {
        return this.longIterator.hasNext();
    }

    @Override
    public long next()
    {
        return this.longIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }
}
