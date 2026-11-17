// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Comparator;
import java.util.Objects;

/**
 * Admission-only member description.
 *
 * <p>The pool copies this descriptor into primitive columns and does not retain this object.</p>
 */
public record MIndexClassMemberSpec(
    MIndexMemberKind kind, String name, String descriptor, int modifiers) {

  static final Comparator<MIndexClassMemberSpec> CANONICAL_ORDER =
      Comparator.comparingInt((MIndexClassMemberSpec value) -> value.kind().ordinal())
          .thenComparing(MIndexClassMemberSpec::name)
          .thenComparing(MIndexClassMemberSpec::descriptor)
          .thenComparingInt(MIndexClassMemberSpec::modifiers);

  public MIndexClassMemberSpec {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(descriptor, "descriptor");
    if (name.isEmpty()) throw new IllegalArgumentException("empty member name");
    if (descriptor.isEmpty()) throw new IllegalArgumentException("empty member descriptor");
  }

  public static MIndexClassMemberSpec field(String name, String descriptor, int modifiers) {
    return new MIndexClassMemberSpec(MIndexMemberKind.FIELD, name, descriptor, modifiers);
  }

  public static MIndexClassMemberSpec method(String name, String descriptor, int modifiers) {
    return new MIndexClassMemberSpec(MIndexMemberKind.METHOD, name, descriptor, modifiers);
  }

  public static MIndexClassMemberSpec constructor(String descriptor, int modifiers) {
    return new MIndexClassMemberSpec(MIndexMemberKind.CONSTRUCTOR, "<init>", descriptor, modifiers);
  }

  public static MIndexClassMemberSpec recordComponent(String name, String descriptor) {
    return new MIndexClassMemberSpec(MIndexMemberKind.RECORD_COMPONENT, name, descriptor, 0);
  }
}
