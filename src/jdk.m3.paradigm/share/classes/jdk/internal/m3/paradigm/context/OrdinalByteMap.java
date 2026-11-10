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
 * Shared-schema ordinal byte map; no per-entry nodes or boxed values.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalByteMap.java}
 * blob {@code 63ab34609ec57645c0fb0d717537ad45cce7cad0} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalByteMap {
    private final OrdinalSchema schema;
    private final byte[] values;
    private final long[] present;
    public OrdinalByteMap(OrdinalSchema schema){
        this.schema=Objects.requireNonNull(schema,"schema");
        values=new byte[schema.size()];
        present=new long[(schema.size()+63)>>>6];
    }
    public boolean contains(int ordinal){ check(ordinal); return (present[ordinal>>>6]&(1L<<(ordinal&63)))!=0; }
    public int getOrDefault(int ordinal,int fallback){ return contains(ordinal)?Byte.toUnsignedInt(values[ordinal]):fallback; }
    public int getOrDefault(String key,int fallback){ return getOrDefault(schema.requireOrdinal(key),fallback); }
    public void put(int ordinal,int value){ check(ordinal); if((value&~255)!=0) throw new IllegalArgumentException("value must be 0..255"); values[ordinal]=(byte)value; present[ordinal>>>6]|=1L<<(ordinal&63); }
    public void put(String key,int value){ put(schema.requireOrdinal(key),value); }
    public void remove(int ordinal){ check(ordinal); present[ordinal>>>6]&=~(1L<<(ordinal&63)); }
    public int capacity(){ return values.length; }
    private void check(int ordinal){ Objects.checkIndex(ordinal,values.length); }
}
