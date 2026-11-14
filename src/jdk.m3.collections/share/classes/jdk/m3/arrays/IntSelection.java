// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;
import jdk.m3.arrays.SelectionBits;import java.util.Objects;import java.util.stream.IntStream;
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/64652588f0d4c23b1f6e0411090b2a796325ae0e/synexia-mat-collections/src/main/java/com/synexia/matcollections/MatSelection.java blob 4dfbee90f2e0; recipe m3/arrays/recipe/posting-index-20261106/relocate.py v2
public final class IntSelection{
  private final SelectionBits bits;public IntSelection(SelectionBits bits){this.bits=Objects.requireNonNull(bits);}
  public static Builder builder(int rows){return new Builder(rows);}public int rows(){return bits.size();}public int size(){return bits.cardinality();}
  public boolean contains(int row){return bits.get(row);}public long primitivePayloadBytes(){return bits.payloadBytes();}public IntStream rowsSelected(){return bits.setBits();}
  public IntSelection and(IntSelection o){req(o);return new IntSelection(bits.and(o.bits));}public IntSelection or(IntSelection o){req(o);return new IntSelection(bits.or(o.bits));}public IntSelection xor(IntSelection o){req(o);return new IntSelection(bits.xor(o.bits));}public IntSelection minus(IntSelection o){req(o);return new IntSelection(bits.andNot(o.bits));}public IntSelection not(){return new IntSelection(bits.not());}
  private void req(IntSelection o){if(o==null||rows()!=o.rows())throw new IllegalArgumentException("different row universes");}
  public static final class Builder{private final SelectionBits.Builder bits;private Builder(int rows){bits=SelectionBits.builder(rows);}public Builder add(int row){bits.set(row);return this;}public Builder add(int row,boolean v){bits.set(row,v);return this;}public IntSelection build(){return new IntSelection(bits.build());}}
}
