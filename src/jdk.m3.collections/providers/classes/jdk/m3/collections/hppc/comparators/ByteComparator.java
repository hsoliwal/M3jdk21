package jdk.m3.collections.hppc.comparators;

/** Compares two <code>byte</code> values. */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeComparator.java")
public interface ByteComparator {
  int compare(byte a, byte b);

  static <KType> ByteComparator naturalOrder() {
    return Byte::compare;
  }
}
