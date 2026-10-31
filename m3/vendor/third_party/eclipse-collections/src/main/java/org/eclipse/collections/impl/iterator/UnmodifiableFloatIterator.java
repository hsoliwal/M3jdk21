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

import org.eclipse.collections.api.iterator.FloatIterator;
import org.eclipse.collections.api.iterator.MutableFloatIterator;

/**
 * UnmodifiableFloatIterator is a wrapper around FloatIterator which is unmodifiable and doesn't support remove.
 * This file was automatically generated from template file unmodifiablePrimitiveIterator.stg.
 */
public class UnmodifiableFloatIterator implements MutableFloatIterator
{
    private final FloatIterator floatIterator;

    public UnmodifiableFloatIterator(FloatIterator floatIterator)
    {
        this.floatIterator = floatIterator;
    }

    @Override
    public boolean hasNext()
    {
        return this.floatIterator.hasNext();
    }

    @Override
    public float next()
    {
        return this.floatIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }
}
