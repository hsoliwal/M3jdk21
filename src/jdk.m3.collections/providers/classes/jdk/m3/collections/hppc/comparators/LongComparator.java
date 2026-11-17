package jdk.m3.collections.hppc.comparators;

/** Compares two <code>long</code> values. */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeComparator.java")
public interface LongComparator {
  int compare(long a, long b);

  static <KType> LongComparator naturalOrder() {
    return Long::compare;
  }
}
