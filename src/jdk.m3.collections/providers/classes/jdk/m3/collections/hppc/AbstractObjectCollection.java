package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.ObjectCursor;
import jdk.m3.collections.hppc.predicates.ObjectPredicate;
import java.util.Arrays;

/** Common superclass for collections. */
@SuppressWarnings("unchecked")
@jdk.m3.collections.hppc.Generated(
    date = "2026-11-04T09:22:44+0100",
    value = "AbstractKTypeCollection.java")
abstract class AbstractObjectCollection<KType> implements ObjectCollection<KType> {
  /** Default implementation uses a predicate for removal. */
  @Override
  public int removeAll(final ObjectLookupContainer<? super KType> c) {
    return this.removeAll(c::contains);
  }

  /** Default implementation uses a predicate for retaining. */
  @Override
  public int retainAll(final ObjectLookupContainer<? super KType> c) {
    // We know c holds sub-types of Object and we're not modifying c, so go unchecked.
    return this.removeAll(k -> !c.contains(k));
  }

  /**
   * Default implementation redirects to {@link #removeAll(ObjectPredicate)} and negates the
   * predicate.
   */
  @Override
  public int retainAll(final ObjectPredicate<? super KType> predicate) {
    return removeAll(value -> !predicate.apply(value));
  }

  /** Default implementation of copying to an array. */
  @Override
  public Object[] toArray() {

    KType[] array = ((KType[]) new Object[size()]);
    int i = 0;
    for (ObjectCursor<KType> c : this) {
      array[i++] = c.value;
    }
    return array;
  }

  public <T> T[] toArray(Class<T> componentClass) {
    final int size = size();
    final T[] array = (T[]) java.lang.reflect.Array.newInstance(componentClass, size);
    int i = 0;
    for (ObjectCursor<KType> c : this) {
      array[i++] = (T) c.value;
    }
    return array;
  }

  /** Convert the contents of this container to a human-friendly string. */
  @Override
  public String toString() {
    return Arrays.toString(this.toArray());
  }

  protected boolean equals(Object v1, Object v2) {
    return (v1 == v2) || (v1 != null && v1.equals(v2));
  }
}
