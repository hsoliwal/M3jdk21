// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Arrays;
import java.util.Objects;

/**
 * Admission-only class description.
 *
 * <p>All arrays and member descriptors are defensively copied. {@link MIndexClassPool} consumes
 * this object and retains only primitive columns plus canonical indexed text.</p>
 */
public final class MIndexClassSpec {
  private final int namespaceId;
  private final MIndexClassKind kind;
  private final String binaryName;
  private final String packageName;
  private final String moduleName;
  private final int modifiers;
  private final int superClassHandle;
  private final int[] interfaceHandles;
  private final MIndexClassMemberSpec[] members;

  public MIndexClassSpec(
      int namespaceId,
      MIndexClassKind kind,
      String binaryName,
      String packageName,
      String moduleName,
      int modifiers,
      int superClassHandle,
      int[] interfaceHandles,
      MIndexClassMemberSpec[] members) {
    if (namespaceId < 0) throw new IllegalArgumentException("negative namespaceId");
    this.namespaceId = namespaceId;
    this.kind = Objects.requireNonNull(kind, "kind");
    this.binaryName = requireText(binaryName, "binaryName", false);
    this.packageName = requireText(packageName, "packageName", true);
    this.moduleName = requireText(moduleName, "moduleName", true);
    if (superClassHandle < 0) throw new IllegalArgumentException("negative superClassHandle");
    this.modifiers = modifiers;
    this.superClassHandle = superClassHandle;
    this.interfaceHandles = Objects.requireNonNull(interfaceHandles, "interfaceHandles").clone();
    for (int handle : this.interfaceHandles) {
      if (handle <= 0) throw new IllegalArgumentException("interface handle must be positive");
    }
    for (int i = 0; i < this.interfaceHandles.length; i++) {
      for (int j = i + 1; j < this.interfaceHandles.length; j++) {
        if (this.interfaceHandles[i] == this.interfaceHandles[j]) {
          throw new IllegalArgumentException("duplicate interface handle " + this.interfaceHandles[i]);
        }
      }
    }
    this.members = Objects.requireNonNull(members, "members").clone();
    for (int i = 0; i < this.members.length; i++) {
      this.members[i] = Objects.requireNonNull(this.members[i], "member");
    }
    Arrays.sort(this.members, MIndexClassMemberSpec.CANONICAL_ORDER);
    for (int i = 1; i < this.members.length; i++) {
      if (this.members[i - 1].equals(this.members[i])) {
        throw new IllegalArgumentException("duplicate member spec " + this.members[i]);
      }
    }
  }

  private static String requireText(String value, String name, boolean emptyAllowed) {
    Objects.requireNonNull(value, name);
    if (!emptyAllowed && value.isEmpty()) throw new IllegalArgumentException("empty " + name);
    return value;
  }

  public int namespaceId() { return namespaceId; }
  public MIndexClassKind kind() { return kind; }
  public String binaryName() { return binaryName; }
  public String packageName() { return packageName; }
  public String moduleName() { return moduleName; }
  public int modifiers() { return modifiers; }
  public int superClassHandle() { return superClassHandle; }
  public int interfaceCount() { return interfaceHandles.length; }
  public int interfaceHandle(int ordinal) {
    return interfaceHandles[Objects.checkIndex(ordinal, interfaceHandles.length)];
  }
  public int[] interfaceHandles() { return interfaceHandles.clone(); }
  public int memberCount() { return members.length; }
  public MIndexClassMemberSpec member(int ordinal) {
    return members[Objects.checkIndex(ordinal, members.length)];
  }
  public MIndexClassMemberSpec[] members() { return members.clone(); }
}
