/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.list.primitive;

import org.eclipse.collections.api.DoubleIterable;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import java.util.stream.DoubleStream;

/**
 * A factory which creates instances of type {@link MutableDoubleList}.
 * This file was automatically generated from template file mutablePrimitiveListFactory.stg.
 *
 * @since 6.0.
 */
public interface MutableDoubleListFactory
{
    MutableDoubleList empty();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleList of();

    /**
     * Same as {@link #empty()}.
     */
    MutableDoubleList with();

    /**
     * Same as {@link #empty()}. but takes in an initial capacity
     * @since 10.3
     */
    default MutableDoubleList withInitialCapacity(int capacity)
    {
        throw new UnsupportedOperationException("Default method to ensure backwards compatibility");
    }

    /**
     * Same as {@link #with(double[])}.
     */
    MutableDoubleList of(double... items);

    /**
     * Creates a new list using the passed {@code items} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    MutableDoubleList with(double... items);

    /**
     * Creates a new list by first copying the array passed in.
     */
    default MutableDoubleList wrapCopy(double... array)
    {
        double[] newArray = new double[array.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return this.with(newArray);
    }

    /**
     * Same as {@link #withAll(DoubleIterable)}.
     */
    MutableDoubleList ofAll(DoubleIterable items);

    MutableDoubleList withAll(DoubleIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    MutableDoubleList ofAll(Iterable<Double> iterable);

    MutableDoubleList withAll(Iterable<Double> iterable);

    /**
     * @since 9.0
     */
    MutableDoubleList ofAll(DoubleStream items);

    /**
     * @since 9.0
     */
    MutableDoubleList withAll(DoubleStream items);
}
