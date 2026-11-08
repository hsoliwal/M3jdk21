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
package jdk.internal.m3.collections.mat;
import java.util.Objects;import java.util.stream.IntStream;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatSelection.java}
 * blob {@code 4dfbee90f2e0371a7ee4699af5060dd33d114afa} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatSelection{
  private final MatBits bits;public MatSelection(MatBits bits){this.bits=Objects.requireNonNull(bits);}
  public static Builder builder(int rows){return new Builder(rows);}public int rows(){return bits.size();}public int size(){return bits.cardinality();}
  public boolean contains(int row){return bits.get(row);}public long primitivePayloadBytes(){return bits.payloadBytes();}public IntStream rowsSelected(){return bits.setBits();}
  public MatSelection and(MatSelection o){req(o);return new MatSelection(bits.and(o.bits));}public MatSelection or(MatSelection o){req(o);return new MatSelection(bits.or(o.bits));}public MatSelection xor(MatSelection o){req(o);return new MatSelection(bits.xor(o.bits));}public MatSelection minus(MatSelection o){req(o);return new MatSelection(bits.andNot(o.bits));}public MatSelection not(){return new MatSelection(bits.not());}
  private void req(MatSelection o){if(o==null||rows()!=o.rows())throw new IllegalArgumentException("different row universes");}
  public static final class Builder{private final MatBits.Builder bits;private Builder(int rows){bits=MatBits.builder(rows);}public Builder add(int row){bits.set(row);return this;}public Builder add(int row,boolean v){bits.set(row,v);return this;}public MatSelection build(){return new MatSelection(bits.build());}}
}
