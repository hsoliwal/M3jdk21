// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.indexstring.type;

import java.util.Objects;

/** Lightweight view over one member row in {@link MIndexClassPool}. */
public final class MIndexClassMember {
  private final MIndexClassPool pool;
  private final int index;

  MIndexClassMember(MIndexClassPool pool, int index) {
    this.pool = Objects.requireNonNull(pool, "pool");
    this.index = index;
  }

  public int index() { return index; }
  public MIndexClass owner() { return pool.value(pool.memberOwner(index)); }
  public MIndexMemberKind kind() { return pool.memberKind(index); }
  public String name() { return pool.memberName(index); }
  public String descriptor() { return pool.memberDescriptor(index); }
  public int modifiers() { return pool.memberModifiers(index); }
  public long exactHash64() { return pool.memberExactHash64(index); }
  public long structuralHash64() { return pool.memberStructuralHash64(index); }
  public long logicHash64() { return pool.memberLogicHash64(index); }
  public long simHash64() { return pool.memberSimHash64(index); }

  @Override public boolean equals(Object other) {
    return this == other
        || other instanceof MIndexClassMember that && pool == that.pool && index == that.index;
  }

  @Override public int hashCode() {
    return 31 * System.identityHashCode(pool) + index;
  }
}
