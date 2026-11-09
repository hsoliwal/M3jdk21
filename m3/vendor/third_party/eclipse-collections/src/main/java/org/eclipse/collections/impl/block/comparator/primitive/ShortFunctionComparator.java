/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.comparator.primitive;

import org.eclipse.collections.api.block.SerializableComparator;
import org.eclipse.collections.api.block.function.primitive.ShortFunction;

/**
 * A Comparator which takes a ShortFunction to compare a primitive short value retrieved from an object.
 * This file was automatically generated from template file primitiveFunctionComparator.stg.
 */
public class ShortFunctionComparator<T>
        implements SerializableComparator<T>
{
    private static final long serialVersionUID = 1L;

    private final ShortFunction<T> function;

    public ShortFunctionComparator(ShortFunction<T> function)
    {
        this.function = function;
    }

    @Override
    public int compare(T o1, T o2)
    {
        short one = this.function.shortValueOf(o1);
        short two = this.function.shortValueOf(o2);
        return one - two;
    }
}
