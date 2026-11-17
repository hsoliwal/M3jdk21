package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.DoubleCursor;
import jdk.m3.collections.hppc.predicates.DoublePredicate;
import java.util.Arrays;

/** Common superclass for collections. */
@jdk.m3.collections.hppc.Generated(
    date = "2026-11-04T09:22:44+0100",
    value = "AbstractKTypeCollection.java")
abstract class AbstractDoubleCollection implements DoubleCollection {
  /** Default implementation uses a predicate for removal. */
  @Override
  public int removeAll(final DoubleLookupContainer c) {
    return this.removeAll(c::contains);
  }

  /** Default implementation uses a predicate for retaining. */
  @Override
  public int retainAll(final DoubleLookupContainer c) {
    // We know c holds sub-types of double and we're not modifying c, so go unchecked.
    return this.removeAll(k -> !c.contains(k));
  }

  /**
   * Default implementation redirects to {@link #removeAll(DoublePredicate)} and negates the
   * predicate.
   */
  @Override
  public int retainAll(final DoublePredicate predicate) {
    return removeAll(value -> !predicate.apply(value));
  }

  /** Default implementation of copying to an array. */
  @Override
  public double[] toArray() {

    double[] array = (new double[size()]);
    int i = 0;
    for (DoubleCursor c : this) {
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
