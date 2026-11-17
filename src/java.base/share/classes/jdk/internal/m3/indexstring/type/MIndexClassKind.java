// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Objects;

/** Java reference-type kind retained as one byte in {@link MIndexClassPool}. */
public enum MIndexClassKind {
  CLASS,
  INTERFACE,
  ENUM,
  ANNOTATION,
  RECORD;

  public static MIndexClassKind fromClass(Class<?> type) {
    Objects.requireNonNull(type, "type");
    if (type.isPrimitive() || type.isArray()) {
      throw new IllegalArgumentException("MIndexClass models declared reference types only: " + type);
    }
    if (type.isAnnotation()) return ANNOTATION;
    if (type.isEnum()) return ENUM;
    if (type.isRecord()) return RECORD;
    if (type.isInterface()) return INTERFACE;
    return CLASS;
  }
}
