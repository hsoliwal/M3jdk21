package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.CharCursor;
import jdk.m3.collections.hppc.predicates.CharPredicate;
import java.util.Arrays;

/** Common superclass for collections. */
@jdk.m3.collections.hppc.Generated(
    date = "2026-11-04T09:22:44+0100",
    value = "AbstractKTypeCollection.java")
abstract class AbstractCharCollection implements CharCollection {
  /** Default implementation uses a predicate for removal. */
  @Override
  public int removeAll(final CharLookupContainer c) {
    return this.removeAll(c::contains);
  }

  /** Default implementation uses a predicate for retaining. */
  @Override
  public int retainAll(final CharLookupContainer c) {
    // We know c holds sub-types of char and we're not modifying c, so go unchecked.
    return this.removeAll(k -> !c.contains(k));
  }

  /**
   * Default implementation redirects to {@link #removeAll(CharPredicate)} and negates the
   * predicate.
   */
  @Override
  public int retainAll(final CharPredicate predicate) {
    return removeAll(value -> !predicate.apply(value));
  }

  /** Default implementation of copying to an array. */
  @Override
  public char[] toArray() {

    char[] array = (new char[size()]);
    int i = 0;
    for (CharCursor c : this) {
      array[i++] = c.value;
    }
    return array;
  }

  /** Convert the contents of this container to a human-friendly string. */
  @Override
  public String toString() {
    return Arrays.toString(this.toArray());
  }
}
