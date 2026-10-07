// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import java.util.Objects;

/** One immutable precompute result plus exact input/content identity and retained primitive weight. */
public record MIndexPrecomputeArtifact<T>(
    MIndexPrecomputeKey<T> key,
    T value,
    String inputSha256,
    String contentSha256,
    long retainedBytes) {

  public MIndexPrecomputeArtifact {
    Objects.requireNonNull(key, "key");
    value = key.type().cast(Objects.requireNonNull(value, "value"));
    inputSha256 = MIndexPrecomputeScope.checkedDigest(inputSha256);
    contentSha256 = MIndexPrecomputeScope.checkedDigest(contentSha256);
    if (retainedBytes < 0) throw new IllegalArgumentException("retainedBytes");
  }
}
