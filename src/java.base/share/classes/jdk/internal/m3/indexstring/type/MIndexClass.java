// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Arrays;
import java.util.Objects;

/**
 * Compact immutable handle for canonical Java class metadata.
 *
 * <p>The instance intentionally owns only a pool reference and integer handle. Names, hierarchy,
 * members, indexes and hashes live in flat arenas in {@link MIndexClassPool}.</p>
 */
public final class MIndexClass {
  private final MIndexClassPool pool;
  private final int handle;

  MIndexClass(MIndexClassPool pool, int handle) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.handle = handle;
  }

  public MIndexClassPool pool() { return pool; }
  public int handle() { return handle; }
  public int namespaceId() { return pool.namespaceId(handle); }
  public MIndexClassKind kind() { return pool.kind(handle); }
  public int modifiers() { return pool.modifiers(handle); }
  public String binaryName() { return pool.binaryName(handle); }
  public String packageName() { return pool.packageName(handle); }
  public String moduleName() { return pool.moduleName(handle); }
  public boolean hasSuperClass() { return pool.superClassHandle(handle) != 0; }
  public MIndexClass superClass() {
    int parent = pool.superClassHandle(handle);
    return parent == 0 ? null : pool.value(parent);
  }
  public int interfaceCount() { return pool.interfaceCount(handle); }
  public MIndexClass interfaceAt(int ordinal) {
    return pool.value(pool.interfaceHandle(handle, ordinal));
  }
  public int[] interfaceHandles() { return pool.interfaceHandles(handle); }
  public int memberCount() { return pool.memberCount(handle); }
  public MIndexClassMember memberAt(int ordinal) {
    return pool.member(handle, ordinal);
  }
  public long identityHash64() { return pool.identityHash64(handle); }
  public long exactHash64() { return pool.exactHash64(handle); }
  public long structuralHash64() { return pool.structuralHash64(handle); }
  public long logicHash64() { return pool.logicHash64(handle); }
  public long simHash64() { return pool.simHash64(handle); }
  public byte[] sha256() { return pool.sha256(handle); }

  @Override public boolean equals(Object other) {
    return this == other
        || other instanceof MIndexClass that && pool == that.pool && handle == that.handle;
  }

  @Override public int hashCode() {
    return 31 * System.identityHashCode(pool) + handle;
  }

  @Override public String toString() {
    return "MIndexClass[" + handle + "," + kind() + "," + binaryName()
        + ",interfaces=" + Arrays.toString(interfaceHandles()) + ",members=" + memberCount() + "]";
  }
}
