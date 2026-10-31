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

import java.util.NoSuchElementException;

import org.eclipse.collections.api.iterator.BooleanIterator;

/**
 * This file was automatically generated from template file singletonPrimitiveIterator.stg.
 */
public final class SingletonBooleanIterator implements BooleanIterator
{
    private final boolean value;
    private boolean next = true;

    public SingletonBooleanIterator(boolean value)
    {
        this.value = value;
    }

    @Override
    public boolean hasNext()
    {
        return this.next;
    }

    @Override
    public boolean next()
    {
        if (this.next)
        {
            this.next = false;
            return this.value;
        }
        throw new NoSuchElementException("i=" + this.next);
    }
}
