/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.procedure;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.api.block.procedure.Procedure;

/**
 * Implementation of {@link Procedure} that holds on to the summation of elements seen so far,
 * determined by the {@link Function}.
 *
 * This file was automatically generated from template file sumOfPrimitiveProcedure.stg.
 */
public class SumOfByteProcedure<T> implements Procedure<T>
{
    private static final long serialVersionUID = 1L;

    protected final ByteFunction<? super T> function;
    protected byte result;

    public SumOfByteProcedure(ByteFunction<? super T> function)
    {
        this.function = function;
    }

    public byte getResult()
    {
        return this.result;
    }

    @Override
    public void value(T each)
    {
        this.result += this.function.byteValueOf(each);
    }
}
