/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.primitive;

import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;

/**
 * This file was automatically generated from template file primitiveKeysMap.stg.
 *
 * @since 6.0.
 */
public interface ShortKeysMap
{
    int size();

    boolean containsKey(short key);

    void forEachKey(ShortProcedure procedure);

    boolean isEmpty();

    boolean notEmpty();
}
