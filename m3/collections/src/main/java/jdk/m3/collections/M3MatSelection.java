/*******************************************************************************
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
package jdk.m3.collections;
import java.util.Objects;import java.util.stream.IntStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSelection.java}
 * blob {@code 4dfbee90f2e0371a7ee4699af5060dd33d114afa} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatSelection{
  private final M3MatBits bits;public M3MatSelection(M3MatBits bits){this.bits=Objects.requireNonNull(bits);}
  public static Builder builder(int rows){return new Builder(rows);}public int rows(){return bits.size();}public int size(){return bits.cardinality();}
  public boolean contains(int row){return bits.get(row);}public long primitivePayloadBytes(){return bits.payloadBytes();}public IntStream rowsSelected(){return bits.setBits();}
  public M3MatSelection and(M3MatSelection o){req(o);return new M3MatSelection(bits.and(o.bits));}public M3MatSelection or(M3MatSelection o){req(o);return new M3MatSelection(bits.or(o.bits));}public M3MatSelection xor(M3MatSelection o){req(o);return new M3MatSelection(bits.xor(o.bits));}public M3MatSelection minus(M3MatSelection o){req(o);return new M3MatSelection(bits.andNot(o.bits));}public M3MatSelection not(){return new M3MatSelection(bits.not());}
  private void req(M3MatSelection o){if(o==null||rows()!=o.rows())throw new IllegalArgumentException("different row universes");}
  public static final class Builder{private final M3MatBits.Builder bits;private Builder(int rows){bits=M3MatBits.builder(rows);}public Builder add(int row){bits.set(row);return this;}public Builder add(int row,boolean v){bits.set(row,v);return this;}public M3MatSelection build(){return new M3MatSelection(bits.build());}}
}
