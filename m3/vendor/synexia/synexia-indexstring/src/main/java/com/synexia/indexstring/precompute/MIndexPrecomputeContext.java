// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.precompute;

import com.synexia.job.IProgressMonitor;
import java.util.Objects;

/** Read-only view exposed to one executing precompute pass. */
public final class MIndexPrecomputeContext {
  private final MIndexPrecomputeScope scope;
  private final MIndexPrecomputeLimits limits;
  private final MIndexPrecomputePlan plan;
  private final MIndexPrecomputeArtifact<?>[] artifacts;
  private final int passRow;
  private final IProgressMonitor monitor;

  MIndexPrecomputeContext(
      MIndexPrecomputeScope scope,
      MIndexPrecomputeLimits limits,
      MIndexPrecomputePlan plan,
      MIndexPrecomputeArtifact<?>[] artifacts,
      int passRow,
      IProgressMonitor monitor) {
    this.scope = Objects.requireNonNull(scope, "scope");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.plan = Objects.requireNonNull(plan, "plan");
    this.artifacts = Objects.requireNonNull(artifacts, "artifacts");
    this.passRow = Objects.checkIndex(passRow, plan.size());
    this.monitor = monitor == null ? IProgressMonitor.noop() : monitor;
  }

  public MIndexPrecomputeScope scope() {
    return scope;
  }

  public MIndexPrecomputeLimits limits() {
    return limits;
  }

  public IProgressMonitor monitor() {
    return monitor;
  }

  public boolean contains(MIndexPrecomputeKey<?> key) {
    int row = plan.rowOf(Objects.requireNonNull(key, "key"));
    return row >= 0 && allowed(row) && artifacts[row] != null;
  }

  public <T> T require(MIndexPrecomputeKey<T> key) {
    return artifact(key).value();
  }

  public <T> MIndexPrecomputeArtifact<T> artifact(MIndexPrecomputeKey<T> key) {
    int row = plan.rowOf(Objects.requireNonNull(key, "key"));
    if (row < 0 || !allowed(row)) {
      throw new IllegalArgumentException(
          "pass " + plan.keyAt(passRow) + " did not declare dependency " + key);
    }
    MIndexPrecomputeArtifact<?> artifact = artifacts[row];
    if (artifact == null) {
      throw new IllegalStateException("dependency has not been computed: " + key);
    }
    return new MIndexPrecomputeArtifact<>(
        key,
        key.type().cast(artifact.value()),
        artifact.inputSha256(),
        artifact.contentSha256(),
        artifact.retainedBytes());
  }

  private boolean allowed(int row) {
    for (int at = plan.dependencyStart(passRow); at < plan.dependencyEnd(passRow); at++) {
      if (plan.dependencyRowAt(at) == row) return true;
    }
    return false;
  }
}
