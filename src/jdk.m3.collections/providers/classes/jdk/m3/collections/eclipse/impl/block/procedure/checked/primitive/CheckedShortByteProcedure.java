/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.block.procedure.checked.primitive;

import jdk.m3.collections.eclipse.api.block.procedure.primitive.ShortByteProcedure;

/**
 * This file was automatically generated from template file checkedPrimitivePrimitiveProcedure.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class CheckedShortByteProcedure implements ShortByteProcedure
{
    private static final long serialVersionUID = 1L;

    @Override
    public final void value(short item1, byte item2)
    {
        try
        {
            this.safeValue(item1, item2);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Checked exception caught in ShortByteProcedure", e);
        }
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    public abstract void safeValue(short item1, byte item2) throws Exception;
}
