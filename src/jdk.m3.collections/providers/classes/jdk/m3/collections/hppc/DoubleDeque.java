package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.DoubleCursor;
import jdk.m3.collections.hppc.predicates.DoublePredicate;
import jdk.m3.collections.hppc.procedures.DoubleProcedure;
import java.util.Deque;
import java.util.Iterator;

/**
 * A linear collection that supports element insertion and removal at both ends.
 *
 * @see Deque
 */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:44+0100", value = "KTypeDeque.java")
public interface DoubleDeque extends DoubleCollection {
  /**
   * Removes the first element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeFirst(double e);

  /**
   * Removes the last element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeLast(double e);

  /** Inserts the specified element at the front of this deque. */
  public void addFirst(double e);

  /** Inserts the specified element at the end of this deque. */
  public void addLast(double e);

  /**
   * Retrieves and removes the first element of this deque.
   *
   * @return the head (first) element of this deque.
   */
  public double removeFirst();

  /**
   * Retrieves and removes the last element of this deque.
   *
   * @return the tail of this deque.
   */
  public double removeLast();

  /**
   * Retrieves the first element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public double getFirst();

  /**
   * Retrieves the last element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public double getLast();

  /**
   * @return An iterator over elements in this deque in tail-to-head order.
   */
  public Iterator<DoubleCursor> descendingIterator();

  /** Applies a <code>procedure</code> to all elements in tail-to-head order. */
  public <T extends DoubleProcedure> T descendingForEach(T procedure);

  /**
   * Applies a <code>predicate</code> to container elements as long, as the predicate returns <code>
   * true</code>. The iteration is interrupted otherwise.
   */
  public <T extends DoublePredicate> T descendingForEach(T predicate);
}
