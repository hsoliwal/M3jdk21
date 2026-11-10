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
 * Compact payload for CompactContext: one shared schema, one flag word and primitive/reference lanes.
 * Rich maps are boundary views and intentionally not retained here.
 * The mutable long lane uses the existing one-word LongSlots contract (at most 64 slots).
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPackedFrame.java}
 * blob {@code aede5f649d6b79a889ad8ef9327ddfb0162f3ac5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class PackedFrame {
    private final OrdinalSchema schema;
    private long flags;
    private final OrdinalByteMap bytes;
    private final LongSlots longs;
    private final OrdinalRefMap<Object> refs;

    public PackedFrame(OrdinalSchema schema){
        this.schema=Objects.requireNonNull(schema,"schema");
        this.bytes=new OrdinalByteMap(schema);
        this.longs=new LongSlots(schema);
        this.refs=new OrdinalRefMap<>(schema);
    }
    public OrdinalSchema schema(){ return schema; }
    public long flags(){ return flags; }
    public boolean flag(int bit){ Objects.checkIndex(bit,64); return (flags&(1L<<bit))!=0; }
    public PackedFrame flag(int bit,boolean enabled){ Objects.checkIndex(bit,64); long m=1L<<bit; flags=enabled?flags|m:flags&~m; return this; }
    public OrdinalByteMap bytes(){ return bytes; }
    public LongSlots longs(){ return longs; }
    public OrdinalRefMap<Object> refs(){ return refs; }
}
