package jdk.m3.collections.hppc;

import jdk.m3.collections.hppc.cursors.ShortCursor;
import jdk.m3.collections.hppc.predicates.ShortPredicate;
import jdk.m3.collections.hppc.procedures.ShortProcedure;
import java.util.Deque;
import java.util.Iterator;

/**
 * A linear collection that supports element insertion and removal at both ends.
 *
 * @see Deque
 */
@jdk.m3.collections.hppc.Generated(date = "2026-11-04T09:22:44+0100", value = "KTypeDeque.java")
public interface ShortDeque extends ShortCollection {
  /**
   * Removes the first element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeFirst(short e);

  /**
   * Removes the last element that equals <code>e</code>.
   *
   * @return The deleted element's index or <code>-1</code> if the element was not found.
   */
  public int removeLast(short e);

  /** Inserts the specified element at the front of this deque. */
  public void addFirst(short e);

  /** Inserts the specified element at the end of this deque. */
  public void addLast(short e);

  /**
   * Retrieves and removes the first element of this deque.
   *
   * @return the head (first) element of this deque.
   */
  public short removeFirst();

  /**
   * Retrieves and removes the last element of this deque.
   *
   * @return the tail of this deque.
   */
  public short removeLast();

  /**
   * Retrieves the first element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public short getFirst();

  /**
   * Retrieves the last element of this deque but does not remove it.
   *
   * @return the head of this deque.
   */
  public short getLast();

  /**
   * @return An iterator over elements in this deque in tail-to-head order.
   */
  public Iterator<ShortCursor> descendingIterator();

  /** Applies a <code>procedure</code> to all elements in tail-to-head order. */
  public <T extends ShortProcedure> T descendingForEach(T procedure);

  /**
   * Applies a <code>predicate</code> to container elements as long, as the predicate returns <code>
   * true</code>. The iteration is interrupted otherwise.
   */
  public <T extends ShortPredicate> T descendingForEach(T predicate);
}
