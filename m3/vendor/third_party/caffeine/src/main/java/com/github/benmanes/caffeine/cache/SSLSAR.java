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
 *   <li>RefreshWrite
 *   <li>StrongKeys (inherited)
 *   <li>StrongValues (inherited)
 *   <li>ExpireAccess (inherited)
 *   <li>Listening (inherited)
 *   <li>Stats (inherited)
 * </ul>
 *
 * @author ben.manes@gmail.com (Ben Manes)
 */
@SuppressWarnings("MissingOverride")
final class SSLSAR<K, V> extends SSLSA<K, V> {
  static final LocalCacheFactory FACTORY = SSLSAR::new;

  volatile long refreshAfterWriteNanos;

  SSLSAR(
      Caffeine<K, V> builder, @Nullable AsyncCacheLoader<? super K, V> cacheLoader, boolean async) {
    super(builder, cacheLoader, async);
    this.refreshAfterWriteNanos = builder.getRefreshAfterWriteNanos();
  }

  protected boolean refreshAfterWrite() {
    return true;
  }

  protected long refreshAfterWriteNanos() {
    return refreshAfterWriteNanos;
  }

  protected void setRefreshAfterWriteNanos(long refreshAfterWriteNanos) {
    this.refreshAfterWriteNanos = refreshAfterWriteNanos;
  }
}
