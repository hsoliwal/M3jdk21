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

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * <em>WARNING: GENERATED CODE</em>
 *
 * <p>A cache that provides the following features:
 *
 * <ul>
 *   <li>ExpireWrite
 *   <li>StrongKeys (inherited)
 *   <li>StrongValues (inherited)
 *   <li>MaximumWeight (inherited)
 * </ul>
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings({"MissingOverride", "NullAway"})
class SSMWW<K, V> extends SSMW<K, V> {
  static final LocalCacheFactory FACTORY = SSMWW::new;

  final Ticker ticker;

  final WriteOrderDeque<Node<K, V>> writeOrderDeque;

  volatile long expiresAfterWriteNanos;

  final Pacer pacer;

  SSMWW(
      Caffeine<K, V> builder, @Nullable AsyncCacheLoader<? super K, V> cacheLoader, boolean async) {
    super(builder, cacheLoader, async);
    this.ticker = builder.getTicker();
    this.writeOrderDeque = new WriteOrderDeque<Node<K, V>>();
    this.expiresAfterWriteNanos = builder.getExpiresAfterWriteNanos();
    this.pacer =
        (builder.getScheduler() == Scheduler.disabledScheduler())
            ? null
            : new Pacer(builder.getScheduler());
  }

  public final Ticker expirationTicker() {
    return ticker;
  }

  protected final WriteOrderDeque<Node<K, V>> writeOrderDeque() {
    return writeOrderDeque;
  }

  protected final boolean expiresAfterWrite() {
    return true;
  }

  protected final long expiresAfterWriteNanos() {
    return expiresAfterWriteNanos;
  }

  protected final void setExpiresAfterWriteNanos(long expiresAfterWriteNanos) {
    this.expiresAfterWriteNanos = expiresAfterWriteNanos;
  }

  public final Pacer pacer() {
    return pacer;
  }
}
