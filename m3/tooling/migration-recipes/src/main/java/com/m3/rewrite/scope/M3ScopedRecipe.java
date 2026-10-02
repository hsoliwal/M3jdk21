// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

/**
 * M3 recipe contract: every mutating recipe declares the highest edit scope it requires.
 *
 * <p>A recipe must not write outside this fence. Scope promotion is explicit; a FILE recipe never
 * acquires PACKAGE, MODULE, MULTI_MODULE or LIBRARY_API authority implicitly.
 */
@FunctionalInterface
public interface M3ScopedRecipe {
  M3ScopeFence editScope();

  /** Fails closed when a candidate write escapes the declared recipe scope. */
  default void requireWritable(String path) {
    if (!editScope().allows(path)) {
      throw new IllegalStateException("M3_RECIPE_SCOPE_VIOLATION:" + path);
    }
  }
}
