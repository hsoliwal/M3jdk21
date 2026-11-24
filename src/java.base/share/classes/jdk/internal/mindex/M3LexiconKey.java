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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable qualified coordinate of a canonical M3 lexicon entry.
 *
 * <p>This key describes an admitted shared atom and does not allocate, own, copy, or intern its
 * bytes. Its {@code contentId} must be assigned after exact content verification by the existing
 * canonical store; a hash or SimHash match alone is never sufficient. Namespace and qualifier
 * values are local to their assigning domain and must not be recycled while referenced.</p>
 *
 * <p>Runtime identity of a loaded class or object is deliberately not represented by this key.
 * See {@link M3OccurrenceKey} for owner- and generation-qualified occurrences.</p>
 */
public record M3LexiconKey(
        Domain domain, long namespaceId, long qualifierId, long schemaVersion, long contentId) {

    /** One VM-wide allocator shared by every M3 owner; no identity is inferred from a hash. */
    private static final AtomicLong NEXT_NAMESPACE = new AtomicLong(1L);

    /** The kind of immutable canonical content, not the kind of live object. */
    public enum Domain {
        STRING, PATH, URI, COLLECTION, AST, METADATA, CLASS, METHOD, MODULE,
        OBJECT_LAYOUT, FILE, SQL, REGEX, GRAPH, OTHER
    }

    public M3LexiconKey {
        Objects.requireNonNull(domain, "domain");
        if (namespaceId < 0 || qualifierId < 0 || schemaVersion <= 0) {
            throw new IllegalArgumentException("invalid lexicon coordinates");
        }
    }

    /** Reserve one unrecycled VM-local namespace for an admitted owner or immutable snapshot. */
    public static long newNamespace() {
        long namespace = NEXT_NAMESPACE.getAndIncrement();
        if (namespace <= 0L) {
            throw new IllegalStateException("M3 qualified namespace space exhausted");
        }
        return namespace;
    }
}
