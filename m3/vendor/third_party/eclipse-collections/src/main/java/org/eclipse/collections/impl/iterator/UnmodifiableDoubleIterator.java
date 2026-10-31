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

import org.eclipse.collections.api.iterator.DoubleIterator;
import org.eclipse.collections.api.iterator.MutableDoubleIterator;

/**
 * UnmodifiableDoubleIterator is a wrapper around DoubleIterator which is unmodifiable and doesn't support remove.
 * This file was automatically generated from template file unmodifiablePrimitiveIterator.stg.
 */
public class UnmodifiableDoubleIterator implements MutableDoubleIterator
{
    private final DoubleIterator doubleIterator;

    public UnmodifiableDoubleIterator(DoubleIterator doubleIterator)
    {
        this.doubleIterator = doubleIterator;
    }

    @Override
    public boolean hasNext()
    {
        return this.doubleIterator.hasNext();
    }

    @Override
    public double next()
    {
        return this.doubleIterator.next();
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("Cannot call remove() on " + this.getClass().getSimpleName());
    }
}
