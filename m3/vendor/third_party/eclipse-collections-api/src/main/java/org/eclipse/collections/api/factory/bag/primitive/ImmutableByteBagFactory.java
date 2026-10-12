/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.bag.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.primitive.ImmutableByteBag;

/**
 * A factory which creates instances of type {@link ImmutableByteBag}.
 * This file was automatically generated from template file immutablePrimitiveBagFactory.stg.
 *
 * @since 4.0.
 */
public interface ImmutableByteBagFactory
{
    /**
     * @since 6.0
     */
    ImmutableByteBag empty();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteBag of();

    /**
     * Same as {@link #empty()}.
     */
    ImmutableByteBag with();

    /**
     * Same as {@link #with(byte)}.
     */
    ImmutableByteBag of(byte one);

    ImmutableByteBag with(byte one);

    /**
     * Same as {@link #with(byte[])}.
     */
    ImmutableByteBag of(byte... items);

    ImmutableByteBag with(byte... items);

    /**
     * Same as {@link #withAll(ByteIterable)}.
     */
    ImmutableByteBag ofAll(ByteIterable items);

    ImmutableByteBag withAll(ByteIterable items);

    /**
     * Same as {@link #withAll(Iterable)}.
     */
    ImmutableByteBag ofAll(Iterable<Byte> iterable);

    ImmutableByteBag withAll(Iterable<Byte> iterable);
}
