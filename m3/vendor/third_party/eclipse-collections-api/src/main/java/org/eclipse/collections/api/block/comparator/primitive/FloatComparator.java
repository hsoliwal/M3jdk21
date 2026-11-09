/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.block.comparator.primitive;

import java.io.Serializable;

/**
 * FloatComparator is a primitive Comparator that takes two arguments of type float and
 * otherwise follows the contract of {@code java.util.Comparator}.
 * This comparator can be used to sort primitive collections, which support indirect sorting,
 * if a sort order other thant the natural one of the collection elements is required.
 *
 * This file was automatically generated from template file primitiveComparator.stg.
 *
 */
@FunctionalInterface
public interface FloatComparator
        extends Serializable
{
    int compare(float value1, float value2);
}
