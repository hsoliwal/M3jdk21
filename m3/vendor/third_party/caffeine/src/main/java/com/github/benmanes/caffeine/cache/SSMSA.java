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
 *   <li>ExpireAccess
 *   <li>StrongKeys (inherited)
 *   <li>StrongValues (inherited)
 *   <li>MaximumSize (inherited)
 * </ul>
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings({"MissingOverride", "NullAway"})
class SSMSA<K, V> extends SSMS<K, V> {
  static final LocalCacheFactory FACTORY = SSMSA::new;

  final Ticker ticker;

  final Expiry<K, V> expiry;

  final TimerWheel<K, V> timerWheel;

  volatile long expiresAfterAccessNanos;

  final Pacer pacer;

  SSMSA(
      Caffeine<K, V> builder, @Nullable AsyncCacheLoader<? super K, V> cacheLoader, boolean async) {
    super(builder, cacheLoader, async);
    this.ticker = builder.getTicker();
    this.expiry = builder.getExpiry(isAsync);
    this.timerWheel = builder.expiresVariable() ? new TimerWheel<K, V>() : null;
    this.expiresAfterAccessNanos = builder.getExpiresAfterAccessNanos();
    this.pacer =
        (builder.getScheduler() == Scheduler.disabledScheduler())
            ? null
            : new Pacer(builder.getScheduler());
  }

  public final Ticker expirationTicker() {
    return ticker;
  }

  protected boolean fastpath() {
    return false;
  }

  protected final boolean expiresVariable() {
    return (timerWheel != null);
  }

  public final Expiry<K, V> expiry() {
    return expiry;
  }

  protected final TimerWheel<K, V> timerWheel() {
    return timerWheel;
  }

  protected final boolean expiresAfterAccess() {
    return (timerWheel == null);
  }

  protected final long expiresAfterAccessNanos() {
    return expiresAfterAccessNanos;
  }

  protected final void setExpiresAfterAccessNanos(long expiresAfterAccessNanos) {
    this.expiresAfterAccessNanos = expiresAfterAccessNanos;
  }

  public final Pacer pacer() {
    return pacer;
  }
}
