package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.FloatCursor;
import jdk.m3.collections.hppc.predicates.FloatPredicate;
import jdk.m3.collections.hppc.procedures.FloatProcedure;
import java.util.Deque;
import java.util.Iterator;

/**
 * A linear collection that supports element insertion and removal at both ends.
 *
 * @see Deque
 */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:44+0100", value = "KTypeDeque.java")
public interface FloatDeque extends FloatCollection {
  /**
   * Removes the first element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeFirst(float e);

  /**
   * Removes the last element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeLast(float e);

  /** Inserts the specified element at the front of this deque. */
  public void addFirst(float e);

  /** Inserts the specified element at the end of this deque. */
  public void addLast(float e);

  /**
   * Retrieves and removes the first element of this deque.
   *
   * @return the head (first) element of this deque.
   */
  public float removeFirst();

  /**
   * Retrieves and removes the last element of this deque.
   *
   * @return the tail of this deque.
   */
  public float removeLast();

  /**
   * Retrieves the first element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public float getFirst();

  /**
   * Retrieves the last element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public float getLast();

  /**
   * @return An iterator over elements in this deque in tail-to-head order.
   */
  public Iterator<FloatCursor> descendingIterator();

  /** Applies a <code>procedure</code> to all elements in tail-to-head order. */
  public <T extends FloatProcedure> T descendingForEach(T procedure);

  /**
   * Applies a <code>predicate</code> to container elements as long, as the predicate returns <code>
   * true</code>. The iteration is interrupted otherwise.
   */
  public <T extends FloatPredicate> T descendingForEach(T predicate);
}
