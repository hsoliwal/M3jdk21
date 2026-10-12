/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.block.procedure.checked.primitive;

import org.eclipse.collections.api.block.procedure.primitive.ByteDoubleProcedure;

/**
 * This file was automatically generated from template file checkedPrimitivePrimitiveProcedure.stg.
 *
 * @since 4.0.
 */
public abstract class CheckedByteDoubleProcedure implements ByteDoubleProcedure
{
    private static final long serialVersionUID = 1L;

    @Override
    public final void value(byte item1, double item2)
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
            throw new RuntimeException("Checked exception caught in ByteDoubleProcedure", e);
        }
    }

    @SuppressWarnings("ProhibitedExceptionDeclared")
    public abstract void safeValue(byte item1, double item2) throws Exception;
}
