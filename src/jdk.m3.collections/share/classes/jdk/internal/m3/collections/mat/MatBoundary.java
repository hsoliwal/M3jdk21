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
import java.util.ArrayList;import java.util.LinkedHashMap;import java.util.LinkedHashSet;import java.util.List;import java.util.Map;import java.util.Set;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatBoundary.java}
 * blob {@code 746715994bb9ca2f7c4cbdbe7693436698d58ffa} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatBoundary{
  private MatBoundary(){}
  public static List<Integer> toList(MatIntList v){ArrayList<Integer>r=new ArrayList<>(v.size());v.intStream().forEach(r::add);return List.copyOf(r);}
  public static Set<Integer> toSet(MatAdaptiveIntSet v){LinkedHashSet<Integer>r=new LinkedHashSet<>();v.intStream().forEach(r::add);return Set.copyOf(r);}
  public static Map<Integer,Integer> toMap(MatAdaptiveIntIntMap v){LinkedHashMap<Integer,Integer>r=new LinkedHashMap<>();for(int k:v.keys())r.put(k,v.get(k));return Map.copyOf(r);}
}
