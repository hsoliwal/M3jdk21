// Copyright 2023 Ben Manes. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.github.benmanes.caffeine.cache;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.ReferenceQueue;

/**
 * <em>WARNING: GENERATED CODE</em>
 *
 * <p>A cache entry that provides the following features:
 *
 * <ul>
 *   <li>RefreshWrite
 *   <li>WeakKeys (inherited)
 *   <li>WeakValues (inherited)
 *   <li>ExpireAccess (inherited)
 * </ul>
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings({"MissingOverride", "NullAway"})
class FWAR<K, V> extends FWA<K, V> {
  protected static final VarHandle WRITE_TIME;

  static {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      WRITE_TIME = lookup.findVarHandle(FWAR.class, NodeFactory.WRITE_TIME, long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  volatile long writeTime;

  FWAR() {}

  FWAR(
      K key,
      ReferenceQueue<K> keyReferenceQueue,
      V value,
      ReferenceQueue<V> valueReferenceQueue,
      int weight,
      long now) {
    super(key, keyReferenceQueue, value, valueReferenceQueue, weight, now);
    WRITE_TIME.set(this, now & ~1L);
  }

  FWAR(Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
    super(keyReference, value, valueReferenceQueue, weight, now);
    WRITE_TIME.set(this, now & ~1L);
  }

  public final long getWriteTime() {
    return (long) WRITE_TIME.getOpaque(this);
  }

  public final void setWriteTime(long writeTime) {
    WRITE_TIME.set(this, writeTime);
  }

  public final boolean casWriteTime(long expect, long update) {
    return (writeTime == expect) && WRITE_TIME.compareAndSet(this, expect, update);
  }

  public Node<K, V> newNode(
      K key,
      ReferenceQueue<K> keyReferenceQueue,
      V value,
      ReferenceQueue<V> valueReferenceQueue,
      int weight,
      long now) {
    return new FWAR<>(key, keyReferenceQueue, value, valueReferenceQueue, weight, now);
  }

  public Node<K, V> newNode(
      Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
    return new FWAR<>(keyReference, value, valueReferenceQueue, weight, now);
  }
}
