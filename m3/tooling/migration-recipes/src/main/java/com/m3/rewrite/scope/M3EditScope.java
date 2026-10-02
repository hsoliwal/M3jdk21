// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.Objects;

/**
 * Highest authority required by a mechanical refactoring recipe.
 *
 * <p>The order is intentional: a transformation starts at FILE and is promoted only when its
 * required mutation or observable contract crosses the next boundary.
 */
public enum M3EditScope {
  FILE,
  PACKAGE,
  MODULE,
  MULTI_MODULE,
  LIBRARY_API;

  /** Returns true when this scope is broad enough to contain the required scope. */
  public boolean canContain(M3EditScope required) {
    return ordinal() >= Objects.requireNonNull(required, "required").ordinal();
  }

  /** Returns the smallest scope broad enough to contain both inputs. */
  public static M3EditScope max(M3EditScope left, M3EditScope right) {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(right, "right");
    return left.ordinal() >= right.ordinal() ? left : right;
  }
}
