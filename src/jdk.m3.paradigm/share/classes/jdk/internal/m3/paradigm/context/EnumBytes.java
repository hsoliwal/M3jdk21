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
 * EnumMap-like ordinal byte lanes with a separate presence mask so zero remains a value.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextEnumBytes.java}
 * blob {@code 2f678bb60aa672f34cb248a7620fb3d8e82ebd3f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class EnumBytes<E extends Enum<E>> {
    private final Class<E> type;
    private final E[] universe;
    private final byte[] values;
    private long present;
    public EnumBytes(Class<E> type){
        this.type=Objects.requireNonNull(type,"type");
        universe=type.getEnumConstants();
        if(universe==null || universe.length>64) throw new IllegalArgumentException("enum universe must be <=64");
        values=new byte[universe.length];
    }
    public boolean containsKey(E key){ return (present & mask(key))!=0; }
    public int getOrDefault(E key,int fallback){ int o=ordinal(key); return containsKey(key)?Byte.toUnsignedInt(values[o]):fallback; }
    public EnumBytes<E> put(E key,int value){
        if((value & ~255)!=0) throw new IllegalArgumentException("value must be 0..255");
        int o=ordinal(key); values[o]=(byte)value; present |= 1L<<o; return this;
    }
    public EnumBytes<E> remove(E key){ int o=ordinal(key); present &= ~(1L<<o); return this; }
    public long presenceBits(){ return present; }
    private int ordinal(E key){ Objects.requireNonNull(key,"key"); if(key.getDeclaringClass()!=type) throw new IllegalArgumentException("wrong enum type"); return key.ordinal(); }
    private long mask(E key){ return 1L<<ordinal(key); }
}
