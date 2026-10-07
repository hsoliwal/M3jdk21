// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.util.List;

/**
 * One deterministic pass in the precompute dependency DAG.
 *
 * <p>A pass may only read declared dependencies through the execution context. Any external source
 * identity that can change the result must already be represented by {@link MIndexPrecomputeScope}.
 * The implementation revision invalidates cache entries when pass mechanics change.</p>
 */
public interface MIndexPrecomputePass<T> {
  MIndexPrecomputeKey<T> output();

  default List<MIndexPrecomputeKey<?>> dependencies() {
    return List.of();
  }

  /** Stable implementation/mechanics revision, not a timestamp. */
  String implementationRevision();

  T compute(MIndexPrecomputeContext context);

  long retainedBytes(T value);

  String contentSha256(T value);
}
