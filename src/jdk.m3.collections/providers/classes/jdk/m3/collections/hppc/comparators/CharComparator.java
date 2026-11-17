package jdk.m3.collections.hppc.comparators;

/** Compares two <code>char</code> values. */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:42+0100", value = "KTypeComparator.java")
public interface CharComparator {
  int compare(char a, char b);

  static <KType> CharComparator naturalOrder() {
    return Character::compare;
  }
}
