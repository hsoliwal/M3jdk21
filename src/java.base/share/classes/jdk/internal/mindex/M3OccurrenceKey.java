/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package jdk.internal.mindex;

import java.util.Objects;

/**
 * Immutable semantic occurrence qualified by a source owner, generation and local coordinate.
 *
 * <p>Two occurrences may share a {@link M3LexiconKey} without becoming the same runtime type,
 * binding, source node or Java object. Owners must allocate IDs without reuse for as long as
 * callers can observe the occurrence. Invalidation creates a new generation; old immutable
 * snapshots remain valid only for their exact generation.</p>
 */
public record M3OccurrenceKey(
        M3LexiconKey lexiconEntry, long ownerId, long generation, long occurrenceId) {

    public M3OccurrenceKey {
        Objects.requireNonNull(lexiconEntry, "lexiconEntry");
        if (ownerId < 0 || generation < 0 || occurrenceId < 0) {
            throw new IllegalArgumentException("invalid occurrence coordinates");
        }
    }
}
