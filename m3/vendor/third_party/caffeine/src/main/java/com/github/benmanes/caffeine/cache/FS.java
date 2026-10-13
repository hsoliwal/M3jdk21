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

import com.github.benmanes.caffeine.cache.References.LookupKeyReference;
import com.github.benmanes.caffeine.cache.References.WeakKeyReference;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;

/**
 * <em>WARNING: GENERATED CODE</em>
 *
 * <p>A cache entry that provides the following features:
 *
 * <ul>
 *   <li>WeakKeys
 *   <li>StrongValues
 * </ul>
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings({"MissingOverride", "NullAway", "PMD.UnusedFormalParameter"})
class FS<K, V> extends Node<K, V> implements NodeFactory<K, V> {
  protected static final VarHandle KEY;

  protected static final VarHandle VALUE;

  static {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    try {
      KEY = lookup.findVarHandle(FS.class, NodeFactory.KEY, WeakKeyReference.class);
      VALUE = lookup.findVarHandle(FS.class, NodeFactory.VALUE, Object.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  volatile WeakKeyReference<K> key;

  volatile V value;

  FS() {}

  FS(
      K key,
      ReferenceQueue<K> keyReferenceQueue,
      V value,
      ReferenceQueue<V> valueReferenceQueue,
      int weight,
      long now) {
    this(new WeakKeyReference<K>(key, keyReferenceQueue), value, valueReferenceQueue, weight, now);
  }

  FS(Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
    KEY.set(this, keyReference);
    VALUE.set(this, value);
  }

  public final K getKey() {
    return ((Reference<K>) KEY.get(this)).get();
  }

  public final Object getKeyReference() {
    return KEY.get(this);
  }

  public final V getValue() {
    return (V) VALUE.get(this);
  }

  public final Object getValueReference() {
    return VALUE.get(this);
  }

  public final void setValue(V value, ReferenceQueue<V> referenceQueue) {
    VALUE.setRelease(this, value);
  }

  public final boolean containsValue(Object value) {
    return Objects.equals(value, getValue());
  }

  public Node<K, V> newNode(
      K key,
      ReferenceQueue<K> keyReferenceQueue,
      V value,
      ReferenceQueue<V> valueReferenceQueue,
      int weight,
      long now) {
    return new FS<>(key, keyReferenceQueue, value, valueReferenceQueue, weight, now);
  }

  public Node<K, V> newNode(
      Object keyReference, V value, ReferenceQueue<V> valueReferenceQueue, int weight, long now) {
    return new FS<>(keyReference, value, valueReferenceQueue, weight, now);
  }

  public Object newLookupKey(Object key) {
    return new LookupKeyReference<>(key);
  }

  public Object newReferenceKey(K key, ReferenceQueue<K> referenceQueue) {
    return new WeakKeyReference<K>(key, referenceQueue);
  }

  public final boolean isAlive() {
    Object key = getKeyReference();
    return (key != RETIRED_WEAK_KEY) && (key != DEAD_WEAK_KEY);
  }

  public final boolean isRetired() {
    return (getKeyReference() == RETIRED_WEAK_KEY);
  }

  public final void retire() {
    key.clear();
    KEY.set(this, RETIRED_WEAK_KEY);
  }

  public final boolean isDead() {
    return (getKeyReference() == DEAD_WEAK_KEY);
  }

  public final void die() {
    key.clear();
    VALUE.set(this, null);
    KEY.set(this, DEAD_WEAK_KEY);
  }
}
