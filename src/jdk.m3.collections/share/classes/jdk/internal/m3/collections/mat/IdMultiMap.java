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
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/IdMultiMap.java}
 * blob {@code ce2783a57fc09b7c2e4756de5ca583ccc098ea1b} at com.synexia develop {@code 35474ccf} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class IdMultiMap<K,V>{
  private final Pool<K> kp;private final Pool<V> vp;private final IntMultiMap ids;
  private IdMultiMap(Pool<K>kp,Pool<V>vp,IntMultiMap ids){this.kp=kp;this.vp=vp;this.ids=ids;}
  public static <K,V> Builder<K,V> builder(Pool<K>kp,Pool<V>vp){return new Builder<>(kp,vp);}
  public IdList<V> values(Ref<K> key){Objects.requireNonNull(key);if(key.pool()!=kp)throw new IllegalArgumentException("different key pool");return new IdList<>(vp,ids.values(key.id()).toArray());}
  public int keyCount(){return ids.keyCount();}public int pairCount(){return ids.pairCount();}public long primitivePayloadBytes(){return ids.primitivePayloadBytes();}
  public static final class Builder<K,V>{private final Pool<K>kp;private final Pool<V>vp;private final IntMultiMap.Builder ids=IntMultiMap.builder();private Builder(Pool<K>kp,Pool<V>vp){this.kp=Objects.requireNonNull(kp);this.vp=Objects.requireNonNull(vp);}public Builder<K,V> put(Ref<K>k,Ref<V>v){if(k.pool()!=kp||v.pool()!=vp)throw new IllegalArgumentException("different pool");ids.put(k.id(),v.id());return this;}public IdMultiMap<K,V> build(){return new IdMultiMap<>(kp,vp,ids.build());}}
}
