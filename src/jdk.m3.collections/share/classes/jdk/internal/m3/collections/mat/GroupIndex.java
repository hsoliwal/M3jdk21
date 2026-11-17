/* *****************************************************************************
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
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/GroupIndex.java}
 * blob {@code b4633f8023712f728a8bb6590a059e25d74b3d5b} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class GroupIndex{
  private final IntMultiMap groups;private final int rows;private GroupIndex(IntMultiMap groups,int rows){this.groups=groups;this.rows=rows;}
  public static GroupIndex of(int[] keys){Objects.requireNonNull(keys,"keys");IntMultiMap.Builder b=IntMultiMap.builder();for(int row=0;row<keys.length;row++)b.put(keys[row],row);return new GroupIndex(b.build(),keys.length);}
  public int rows(){return rows;}public int groupCount(){return groups.keyCount();}public int[] groupKeys(){return groups.keys();}public IntList rows(int key){return groups.values(key);}public long primitivePayloadBytes(){return groups.primitivePayloadBytes();}
  public CollectionPlan plan(){return new CollectionPlan(StorageKind.GROUP_CSR,primitivePayloadBytes(),rows,groupCount(),"group key -> stable row ordinals CSR");}
}
