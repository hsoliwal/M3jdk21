// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

/**
 * Convenience contract for the massively parallel first pass: exactly one source file is writable.
 *
 * <p>Implementations may inspect broader read-only context when their algorithm needs it, but a
 * FILE-scoped mutation has exactly one writable target and cannot promote itself implicitly.
 */
@FunctionalInterface
public interface M3FileScopedRecipe extends M3ScopedRecipe {
  String targetPath();

  @Override
  default M3ScopeFence editScope() {
    return M3ScopeFence.file(targetPath());
  }
}
