/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Shared-schema ordinal reference map; one Object[] plus presence bits, no entry objects.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalRefMap.java}
 * blob {@code e99bd234102295379eb8bf4a1b582f710e45bdf9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalRefMap<V> {
    private final OrdinalSchema schema;
    private final Object[] values;
    private final long[] present;
    public OrdinalRefMap(OrdinalSchema schema){
        this.schema=Objects.requireNonNull(schema,"schema");
        values=new Object[schema.size()];
        present=new long[(schema.size()+63)>>>6];
    }
    public boolean contains(int ordinal){ Objects.checkIndex(ordinal,values.length); return (present[ordinal>>>6]&(1L<<(ordinal&63)))!=0; }
    @SuppressWarnings("unchecked") public V get(int ordinal){ Objects.checkIndex(ordinal,values.length); return contains(ordinal)?(V)values[ordinal]:null; }
    public V get(String key){ return get(schema.requireOrdinal(key)); }
    public void put(int ordinal,V value){ Objects.checkIndex(ordinal,values.length); values[ordinal]=Objects.requireNonNull(value,"value"); present[ordinal>>>6]|=1L<<(ordinal&63); }
    public void put(String key,V value){ put(schema.requireOrdinal(key),value); }
    public void remove(int ordinal){ Objects.checkIndex(ordinal,values.length); values[ordinal]=null; present[ordinal>>>6]&=~(1L<<(ordinal&63)); }
}
