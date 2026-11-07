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
import java.util.Objects;
/**
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatIdMultiMap.java}
 * blob {@code ce2783a57fc09b7c2e4756de5ca583ccc098ea1b} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatIdMultiMap<K,V>{
  private final M3MatPool<K> kp;private final M3MatPool<V> vp;private final M3MatIntMultiMap ids;
  private M3MatIdMultiMap(M3MatPool<K>kp,M3MatPool<V>vp,M3MatIntMultiMap ids){this.kp=kp;this.vp=vp;this.ids=ids;}
  public static <K,V> Builder<K,V> builder(M3MatPool<K>kp,M3MatPool<V>vp){return new Builder<>(kp,vp);}
  public M3MatIdList<V> values(M3MatRef<K> key){Objects.requireNonNull(key);if(key.pool()!=kp)throw new IllegalArgumentException("different key pool");return new M3MatIdList<>(vp,ids.values(key.id()).toArray());}
  public int keyCount(){return ids.keyCount();}public int pairCount(){return ids.pairCount();}public long primitivePayloadBytes(){return ids.primitivePayloadBytes();}
  public static final class Builder<K,V>{private final M3MatPool<K>kp;private final M3MatPool<V>vp;private final M3MatIntMultiMap.Builder ids=M3MatIntMultiMap.builder();private Builder(M3MatPool<K>kp,M3MatPool<V>vp){this.kp=Objects.requireNonNull(kp);this.vp=Objects.requireNonNull(vp);}public Builder<K,V> put(M3MatRef<K>k,M3MatRef<V>v){if(k.pool()!=kp||v.pool()!=vp)throw new IllegalArgumentException("different pool");ids.put(k.id(),v.id());return this;}public M3MatIdMultiMap<K,V> build(){return new M3MatIdMultiMap<>(kp,vp,ids.build());}}
}
