package jdk.m3.collections.hppc.comparators;

/** Compares two <code>int</code> values. */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeComparator.java")
public interface IntComparator {
  int compare(int a, int b);

  static <KType> IntComparator naturalOrder() {
    return Integer::compare;
  }
}
