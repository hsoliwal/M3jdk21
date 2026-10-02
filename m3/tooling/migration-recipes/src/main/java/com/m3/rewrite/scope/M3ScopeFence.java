// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.scope;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Deterministic write fence for an M3 recipe.
 *
 * <p>FILE recipes name exactly one file. Broader recipes name one or more directory roots. A
 * MULTI_MODULE recipe must name at least two roots. Paths are repository-relative, normalized to
 * forward slashes and cannot escape through {@code ..}.
 */
public record M3ScopeFence(M3EditScope scope, List<String> roots) {
  public M3ScopeFence {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(roots, "roots");
    if (roots.isEmpty()) {
      throw new IllegalArgumentException("M3_SCOPE_ROOTS_EMPTY");
    }

    List<String> normalized = roots.stream().map(M3ScopeFence::normalize).toList();
    Set<String> unique = new LinkedHashSet<>(normalized);
    if (unique.size() != normalized.size()) {
      throw new IllegalArgumentException("M3_SCOPE_ROOTS_DUPLICATE");
    }
    if ((scope == M3EditScope.FILE || scope == M3EditScope.VISIBILITY)
        && normalized.size() != 1) {
      throw new IllegalArgumentException("M3_SINGLE_FILE_SCOPE_REQUIRES_ONE_FILE");
    }
    if (scope == M3EditScope.MULTI_MODULE && normalized.size() < 2) {
      throw new IllegalArgumentException("M3_MULTI_MODULE_SCOPE_REQUIRES_MULTIPLE_ROOTS");
    }
    roots = List.copyOf(normalized);
  }

  public static M3ScopeFence file(String path) {
    return new M3ScopeFence(M3EditScope.FILE, List.of(path));
  }

  public static M3ScopeFence visibility(String path) {
    return new M3ScopeFence(M3EditScope.VISIBILITY, List.of(path));
  }

  public static M3ScopeFence packageScope(String root) {
    return new M3ScopeFence(M3EditScope.PACKAGE, List.of(root));
  }

  public static M3ScopeFence module(String root) {
    return new M3ScopeFence(M3EditScope.MODULE, List.of(root));
  }

  public static M3ScopeFence multiModule(
      String firstRoot, String secondRoot, String... additionalRoots) {
    Objects.requireNonNull(additionalRoots, "additionalRoots");
    java.util.ArrayList<String> roots = new java.util.ArrayList<>();
    roots.add(firstRoot);
    roots.add(secondRoot);
    roots.addAll(List.of(additionalRoots));
    return new M3ScopeFence(M3EditScope.MULTI_MODULE, roots);
  }

  public static M3ScopeFence library(List<String> roots) {
    return new M3ScopeFence(M3EditScope.LIBRARY, roots);
  }

  /** Returns true only when the path is inside the declared write fence. */
  public boolean allows(String candidatePath) {
    String candidate = normalize(candidatePath);
    if (scope == M3EditScope.FILE || scope == M3EditScope.VISIBILITY) {
      return roots.getFirst().equals(candidate);
    }
    return roots.stream()
        .anyMatch(root -> candidate.equals(root) || candidate.startsWith(root + "/"));
  }

  private static String normalize(String value) {
    Objects.requireNonNull(value, "path");
    String normalized = value.trim().replace('\\', '/');
    while (normalized.startsWith("./")) {
      normalized = normalized.substring(2);
    }
    while (normalized.endsWith("/") && normalized.length() > 1) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.isBlank()
        || normalized.startsWith("/")
        || normalized.matches("^[A-Za-z]:/.*")) {
      throw new IllegalArgumentException("M3_SCOPE_PATH_NOT_RELATIVE:" + value);
    }
    for (String segment : normalized.split("/")) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException("M3_SCOPE_PATH_INVALID:" + value);
      }
    }
    return normalized;
  }
}
