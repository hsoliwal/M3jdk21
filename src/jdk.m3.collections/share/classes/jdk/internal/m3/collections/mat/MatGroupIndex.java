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
import java.util.Objects;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatGroupIndex.java}
 * blob {@code b4633f8023712f728a8bb6590a059e25d74b3d5b} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatGroupIndex{
  private final MatIntMultiMap groups;private final int rows;private MatGroupIndex(MatIntMultiMap groups,int rows){this.groups=groups;this.rows=rows;}
  public static MatGroupIndex of(int[] keys){Objects.requireNonNull(keys,"keys");MatIntMultiMap.Builder b=MatIntMultiMap.builder();for(int row=0;row<keys.length;row++)b.put(keys[row],row);return new MatGroupIndex(b.build(),keys.length);}
  public int rows(){return rows;}public int groupCount(){return groups.keyCount();}public int[] groupKeys(){return groups.keys();}public MatIntList rows(int key){return groups.values(key);}public long primitivePayloadBytes(){return groups.primitivePayloadBytes();}
  public MatCollectionPlan plan(){return new MatCollectionPlan(MatStorageKind.GROUP_CSR,primitivePayloadBytes(),rows,groupCount(),"group key -> stable row ordinals CSR");}
}
