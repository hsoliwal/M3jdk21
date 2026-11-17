package jdk.m3.collections.hppc.cursors;

/** A cursor over entries of an associative container (long keys and byte values). */
@SuppressWarnings("missing-explicit-ctor")
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeVTypeCursor.java")
public final class LongByteCursor {
  /**
   * The current key and value's index in the container this cursor belongs to. The meaning of this
   * index is defined by the container (usually it will be an index in the underlying storage
   * buffer).
   */
  public int index;

  /** The current key. */
  public long key;

  /** The current value. */
  public byte value;

  @Override
  public String toString() {
    return "[cursor, index: " + index + ", key: " + key + ", value: " + value + "]";
  }
}
