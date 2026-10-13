/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.block.predicate.primitive;

import java.io.Serializable;

/**
 * BooleanPredicate is a primitive Predicate which may be used to reduce the overhead
 * of autoboxing Boolean objects in places where primitive booleans could be used.
 * This file was automatically generated from template file primitivePredicate.stg.
 *
 * @since 3.0.
 */
@FunctionalInterface
public interface BooleanPredicate
        extends Serializable
{
    boolean accept(boolean value);
}
