package jdk.m3.collections.hppc.comparators;

/** Compares two <code>short</code> values. */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeComparator.java")
public interface ShortComparator {
  int compare(short a, short b);

  static <KType> ShortComparator naturalOrder() {
    return Short::compare;
  }
}
