/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jdk.m3.collections.guava.graph;

import static jdk.m3.collections.guava.base.Preconditions.checkArgument;
import static jdk.m3.collections.guava.base.Preconditions.checkNotNull;
import static jdk.m3.collections.guava.base.Preconditions.checkState;
import static jdk.m3.collections.guava.collect.Iterators.concat;
import static jdk.m3.collections.guava.collect.Iterators.transform;
import static jdk.m3.collections.guava.graph.GraphConstants.INNER_CAPACITY;
import static jdk.m3.collections.guava.graph.GraphConstants.INNER_LOAD_FACTOR;
import static jdk.m3.collections.guava.graph.Graphs.checkNonNegative;
import static jdk.m3.collections.guava.graph.Graphs.checkPositive;
import static java.util.Collections.unmodifiableSet;

import jdk.m3.collections.guava.base.Function;
import jdk.m3.collections.guava.collect.AbstractIterator;
import jdk.m3.collections.guava.collect.ImmutableList;
import jdk.m3.collections.guava.collect.UnmodifiableIterator;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of {@link GraphConnections} for directed graphs.
 *
 * @author James Sexton
 * @author Jens Nyman
 * @param <N> Node parameter type
 * @param <V> Value parameter type
 */
@SuppressWarnings("cast")
final class DirectedGraphConnections<N, V> implements GraphConnections<N, V> {
  /**
   * A wrapper class to indicate a node is both a predecessor and successor while still providing
   * the successor value.
   */
  private static final class PredAndSucc {
    private final Object successorValue;

    PredAndSucc(Object successorValue) {
      this.successorValue = successorValue;
    }
  }

  /**
   * A value class representing single connection between the origin node and another node.
   *
   * <p>There can be two types of connections (predecessor and successor), which is represented by
   * the two implementations.
   */
  private abstract static class NodeConnection<N> {
    final N node;

    /** Slot in the owning {@code ConnectionLane}; unused by ImmutableList-backed connections. */
    int slot;

    NodeConnection(N node) {
      this.node = checkNotNull(node);
    }

    static final class Pred<N> extends NodeConnection<N> {
      Pred(N node) {
        super(node);
      }

      @Override
      public boolean equals(Object that) {
        if (that instanceof Pred) {
          return this.node.equals(((Pred<?>) that).node);
        } else {
          return false;
        }
      }

      @Override
      public int hashCode() {
        // Adding the class hashCode to avoid a clash with Succ instances.
        return Pred.class.hashCode() + node.hashCode();
      }
    }

    static final class Succ<N> extends NodeConnection<N> {
      Succ(N node) {
        super(node);
      }

      @Override
      public boolean equals(Object that) {
        if (that instanceof Succ) {
          return this.node.equals(((Succ<?>) that).node);
        } else {
          return false;
        }
      }

      @Override
      public int hashCode() {
        // Adding the class hashCode to avoid a clash with Pred instances.
        return Succ.class.hashCode() + node.hashCode();
      }
    }
  }

  /**
   * The {@code orderedNodeConnections} of a mutable graph with a stable incident edge order: an
   * insertion-ordered slot array with {@code EMPTY} tombstones, plus a self-keyed index that finds
   * a stored connection, and so its slot, in O(1) expected time. A removal writes a tombstone; one
   * in-place compaction pass runs once tombstones outnumber live connections, so removal is O(1)
   * amortised and iteration stays O(size). The iterator keeps the {@code ArrayList} iterator
   * contract the list had: {@code hasNext()} is {@code cursor != size()}, and {@code next()} throws
   * {@link ConcurrentModificationException} once the lane was added to or removed from after the
   * iterator was created. A slot never holds null: a removed or unused slot holds {@code EMPTY},
   * which never leaves the lane.
   */
  private static final class ConnectionLane<N> extends AbstractCollection<NodeConnection<N>> {
    private static final Object[] NO_SLOTS = {};

    /** Marks a removed or unused slot; compared with {@code ==}, never returned. */
    private static final Object EMPTY = new Object();

    private final Map<NodeConnection<N>, NodeConnection<N>> index =
        new HashMap<>(INNER_CAPACITY * 2, INNER_LOAD_FACTOR);
    private Object[] slots = NO_SLOTS; // [0, end): connections in insertion order, EMPTY = removed
    private int end;
    private int live;
    private int modCount; // +1 per add and per successful remove, as ArrayList.modCount

    @Override
    public int size() {
      return live;
    }

    @Override
    public boolean add(NodeConnection<N> connection) {
      modCount++;
      if (end == slots.length) {
        slots = Arrays.copyOf(slots, Math.max(INNER_CAPACITY * 2, end + (end >> 1)));
        Arrays.fill(slots, end, slots.length, EMPTY);
      }
      index.put(connection, connection);
      connection.slot = end;
      slots[end++] = connection;
      live++;
      return true;
    }

    @Override
    public boolean remove(Object connection) {
      NodeConnection<N> removed = index.remove(connection);
      if (removed == null) {
        return false;
      }
      modCount++;
      slots[removed.slot] = EMPTY;
      live--;
      if (end - live > live) {
        compact();
      }
      return true;
    }

    /** One pass: slide live connections left in order, renumber their slots, clear the tail. */
    private void compact() {
      int write = 0;
      for (int read = 0; read < end; read++) {
        Object connection = slots[read];
        if (connection != EMPTY) {
          ((NodeConnection<?>) connection).slot = write;
          slots[write++] = connection;
        }
      }
      Arrays.fill(slots, write, end, EMPTY);
      end = write;
    }

    @Override
    public Iterator<NodeConnection<N>> iterator() {
      return new LaneIterator();
    }

    /** Fail-fast cursor over the live slots with the {@code ArrayList} iterator semantics. */
    private final class LaneIterator implements Iterator<NodeConnection<N>> {
      private final int expectedModCount = modCount;
      private int cursor; // logical index of the next connection, as in ArrayList's iterator
      private int position; // first slot not yet scanned

      @Override
      public boolean hasNext() {
        return cursor != live;
      }

      @SuppressWarnings("unchecked")
      @Override
      public NodeConnection<N> next() {
        if (modCount != expectedModCount) {
          throw new ConcurrentModificationException();
        }
        if (cursor >= live) {
          throw new NoSuchElementException();
        }
        Object connection;
        do {
          connection = slots[position++];
        } while (connection == EMPTY);
        cursor++;
        return (NodeConnection<N>) connection;
      }
    }
  }

  private static final Object PRED = new Object();

  // Every value in this map must either be an instance of PredAndSucc with a successorValue of
  // type V, PRED (representing predecessor), or an instance of type V (representing successor).
  private final Map<N, Object> adjacentNodeValues;

  /**
   * All node connections in this graph, in edge insertion order.
   *
   * <p>Note: This field and {@link #adjacentNodeValues} cannot be combined into a single
   * LinkedHashMap because one target node may be mapped to both a predecessor and a successor. A
   * LinkedHashMap combines two such edges into a single node-value pair, even though the edges may
   * not have been inserted consecutively.
   */
  private final Collection<NodeConnection<N>> orderedNodeConnections;

  private int predecessorCount;
  private int successorCount;

  private DirectedGraphConnections(
      Map<N, Object> adjacentNodeValues,
      Collection<NodeConnection<N>> orderedNodeConnections,
      int predecessorCount,
      int successorCount) {
    this.adjacentNodeValues = checkNotNull(adjacentNodeValues);
    this.orderedNodeConnections = orderedNodeConnections;
    this.predecessorCount = checkNonNegative(predecessorCount);
    this.successorCount = checkNonNegative(successorCount);
    checkState(
        predecessorCount <= adjacentNodeValues.size()
            && successorCount <= adjacentNodeValues.size());
  }

  static <N, V> DirectedGraphConnections<N, V> of(ElementOrder<N> incidentEdgeOrder) {
    // We store predecessors and successors in the same map, so double the initial capacity.
    int initialCapacity = INNER_CAPACITY * 2;

    Collection<NodeConnection<N>> orderedNodeConnections =
        switch (incidentEdgeOrder.type()) {
          case UNORDERED -> null;
          case STABLE -> new ConnectionLane<>();
          default -> throw new AssertionError(incidentEdgeOrder.type());
        };

    return new DirectedGraphConnections<>(
        /* adjacentNodeValues= */ new HashMap<N, Object>(initialCapacity, INNER_LOAD_FACTOR),
        orderedNodeConnections,
        /* predecessorCount= */ 0,
        /* successorCount= */ 0);
  }

  static <N, V> DirectedGraphConnections<N, V> ofImmutable(
      N thisNode, Iterable<EndpointPair<N>> incidentEdges, Function<N, V> successorNodeToValueFn) {
    checkNotNull(thisNode);
    checkNotNull(successorNodeToValueFn);

    Map<N, Object> adjacentNodeValues = new HashMap<>();
    ImmutableList.Builder<NodeConnection<N>> orderedNodeConnectionsBuilder =
        ImmutableList.builder();
    int predecessorCount = 0;
    int successorCount = 0;

    for (EndpointPair<N> incidentEdge : incidentEdges) {
      if (incidentEdge.nodeU().equals(thisNode) && incidentEdge.nodeV().equals(thisNode)) {
        // incidentEdge is a self-loop

        adjacentNodeValues.put(thisNode, new PredAndSucc(successorNodeToValueFn.apply(thisNode)));

        orderedNodeConnectionsBuilder.add(new NodeConnection.Pred<>(thisNode));
        orderedNodeConnectionsBuilder.add(new NodeConnection.Succ<>(thisNode));
        predecessorCount++;
        successorCount++;
      } else if (incidentEdge.nodeV().equals(thisNode)) { // incidentEdge is an inEdge
        N predecessor = incidentEdge.nodeU();

        Object existingValue = adjacentNodeValues.put(predecessor, PRED);
        if (existingValue != null) {
          adjacentNodeValues.put(predecessor, new PredAndSucc(existingValue));
        }

        orderedNodeConnectionsBuilder.add(new NodeConnection.Pred<>(predecessor));
        predecessorCount++;
      } else { // incidentEdge is an outEdge
        checkArgument(incidentEdge.nodeU().equals(thisNode));

        N successor = incidentEdge.nodeV();
        V value = successorNodeToValueFn.apply(successor);

        Object existingValue = adjacentNodeValues.put(successor, value);
        if (existingValue != null) {
          checkArgument(existingValue == PRED);
          adjacentNodeValues.put(successor, new PredAndSucc(value));
        }

        orderedNodeConnectionsBuilder.add(new NodeConnection.Succ<>(successor));
        successorCount++;
      }
    }

    return new DirectedGraphConnections<>(
        adjacentNodeValues,
        orderedNodeConnectionsBuilder.build(),
        predecessorCount,
        successorCount);
  }

  @Override
  public Set<N> adjacentNodes() {
    if (orderedNodeConnections == null) {
      return unmodifiableSet(adjacentNodeValues.keySet());
    } else {
      return new AbstractSet<N>() {
        @Override
        public UnmodifiableIterator<N> iterator() {
          Iterator<NodeConnection<N>> nodeConnections = orderedNodeConnections.iterator();
          Set<N> seenNodes = new HashSet<>();
          return new AbstractIterator<N>() {
            @Override
            protected N computeNext() {
              while (nodeConnections.hasNext()) {
                NodeConnection<N> nodeConnection = nodeConnections.next();
                boolean added = seenNodes.add(nodeConnection.node);
                if (added) {
                  return nodeConnection.node;
                }
              }
              return endOfData();
            }
          };
        }

        @Override
        public int size() {
          return adjacentNodeValues.size();
        }

        @Override
        public boolean contains(Object obj) {
          return adjacentNodeValues.containsKey(obj);
        }
      };
    }
  }

  @Override
  public Set<N> predecessors() {
    return new AbstractSet<N>() {
      @Override
      public UnmodifiableIterator<N> iterator() {
        if (orderedNodeConnections == null) {
          Iterator<Entry<N, Object>> entries = adjacentNodeValues.entrySet().iterator();
          return new AbstractIterator<N>() {
            @Override
            protected N computeNext() {
              while (entries.hasNext()) {
                Entry<N, Object> entry = entries.next();
                if (isPredecessor(entry.getValue())) {
                  return entry.getKey();
                }
              }
              return endOfData();
            }
          };
        } else {
          Iterator<NodeConnection<N>> nodeConnections = orderedNodeConnections.iterator();
          return new AbstractIterator<N>() {
            @Override
            protected N computeNext() {
              while (nodeConnections.hasNext()) {
                NodeConnection<N> nodeConnection = nodeConnections.next();
                if (nodeConnection instanceof NodeConnection.Pred) {
                  return nodeConnection.node;
                }
              }
              return endOfData();
            }
          };
        }
      }

      @Override
      public int size() {
        return predecessorCount;
      }

      @Override
      public boolean contains(Object obj) {
        return isPredecessor(adjacentNodeValues.get(obj));
      }
    };
  }

  @Override
  public Set<N> successors() {
    return new AbstractSet<N>() {
      @Override
      public UnmodifiableIterator<N> iterator() {
        if (orderedNodeConnections == null) {
          Iterator<Entry<N, Object>> entries = adjacentNodeValues.entrySet().iterator();
          return new AbstractIterator<N>() {
            @Override
            protected N computeNext() {
              while (entries.hasNext()) {
                Entry<N, Object> entry = entries.next();
                if (isSuccessor(entry.getValue())) {
                  return entry.getKey();
                }
              }
              return endOfData();
            }
          };
        } else {
          Iterator<NodeConnection<N>> nodeConnections = orderedNodeConnections.iterator();
          return new AbstractIterator<N>() {
            @Override
            protected N computeNext() {
              while (nodeConnections.hasNext()) {
                NodeConnection<N> nodeConnection = nodeConnections.next();
                if (nodeConnection instanceof NodeConnection.Succ) {
                  return nodeConnection.node;
                }
              }
              return endOfData();
            }
          };
        }
      }

      @Override
      public int size() {
        return successorCount;
      }

      @Override
      public boolean contains(Object obj) {
        return isSuccessor(adjacentNodeValues.get(obj));
      }
    };
  }

  @Override
  public Iterator<EndpointPair<N>> incidentEdgeIterator(N thisNode) {
    checkNotNull(thisNode);

    Iterator<EndpointPair<N>> resultWithDoubleSelfLoop;
    if (orderedNodeConnections == null) {
      resultWithDoubleSelfLoop =
          concat(
              transform(
                  predecessors().iterator(),
                  (N predecessor) -> EndpointPair.ordered(predecessor, thisNode)),
              transform(
                  successors().iterator(),
                  (N successor) -> EndpointPair.ordered(thisNode, successor)));
    } else {
      resultWithDoubleSelfLoop =
          transform(
              orderedNodeConnections.iterator(),
              (NodeConnection<N> connection) -> {
                if (connection instanceof NodeConnection.Succ) {
                  return EndpointPair.ordered(thisNode, connection.node);
                } else {
                  return EndpointPair.ordered(connection.node, thisNode);
                }
              });
    }

    AtomicBoolean alreadySeenSelfLoop = new AtomicBoolean(false);
    return new AbstractIterator<EndpointPair<N>>() {
      @Override
      protected EndpointPair<N> computeNext() {
        while (resultWithDoubleSelfLoop.hasNext()) {
          EndpointPair<N> edge = resultWithDoubleSelfLoop.next();
          if (edge.nodeU().equals(edge.nodeV())) {
            if (!alreadySeenSelfLoop.getAndSet(true)) {
              return edge;
            }
          } else {
            return edge;
          }
        }
        return endOfData();
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public V value(N node) {
    checkNotNull(node);
    Object value = adjacentNodeValues.get(node);
    if (value == PRED) {
      return null;
    }
    if (value instanceof PredAndSucc) {
      return (V) ((PredAndSucc) value).successorValue;
    }
    return (V) value;
  }

  @Override
  public void removePredecessor(N node) {
    checkNotNull(node);

    Object previousValue = adjacentNodeValues.get(node);
    boolean removedPredecessor;

    if (previousValue == PRED) {
      adjacentNodeValues.remove(node);
      removedPredecessor = true;
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put((N) node, ((PredAndSucc) previousValue).successorValue);
      removedPredecessor = true;
    } else {
      removedPredecessor = false;
    }

    if (removedPredecessor) {
      checkNonNegative(--predecessorCount);

      if (orderedNodeConnections != null) {
        orderedNodeConnections.remove(new NodeConnection.Pred<>(node));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V removeSuccessor(Object node) {
    checkNotNull(node);
    Object previousValue = adjacentNodeValues.get(node);
    Object removedValue;

    if (previousValue == null || previousValue == PRED) {
      removedValue = null;
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put((N) node, PRED);
      removedValue = ((PredAndSucc) previousValue).successorValue;
    } else { // successor
      adjacentNodeValues.remove(node);
      removedValue = previousValue;
    }

    if (removedValue != null) {
      checkNonNegative(--successorCount);

      if (orderedNodeConnections != null) {
        orderedNodeConnections.remove(new NodeConnection.Succ<>((N) node));
      }
    }

    /*
     * TODO(cpovirk): `return (V) removedValue` once our checker permits that.
     *
     * (We promoted a class of warnings into errors because sometimes they indicate real problems.
     * But now we need to "undo" some instance of spurious errors, as discussed in
     * https://github.com/jspecify/checker-framework/issues/8.)
     */
    return removedValue == null ? null : (V) removedValue;
  }

  @Override
  public void addPredecessor(N node, V unused) {
    checkNotNull(node);
    checkNotNull(unused);
    Object previousValue = adjacentNodeValues.put(node, PRED);
    boolean addedPredecessor;

    if (previousValue == null) {
      addedPredecessor = true;
    } else if (previousValue instanceof PredAndSucc) {
      // Restore previous PredAndSucc object.
      adjacentNodeValues.put(node, previousValue);
      addedPredecessor = false;
    } else if (previousValue != PRED) { // successor
      // Do NOT use method parameter value 'unused'. In directed graphs, successors store the value.
      adjacentNodeValues.put(node, new PredAndSucc(previousValue));
      addedPredecessor = true;
    } else {
      addedPredecessor = false;
    }

    if (addedPredecessor) {
      checkPositive(++predecessorCount);

      if (orderedNodeConnections != null) {
        orderedNodeConnections.add(new NodeConnection.Pred<>(node));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public V addSuccessor(N node, V value) {
    checkNotNull(node);
    checkNotNull(value);
    Object previousValue = adjacentNodeValues.put(node, value);
    Object previousSuccessor;

    if (previousValue == null) {
      previousSuccessor = null;
    } else if (previousValue instanceof PredAndSucc) {
      adjacentNodeValues.put(node, new PredAndSucc(value));
      previousSuccessor = ((PredAndSucc) previousValue).successorValue;
    } else if (previousValue == PRED) {
      adjacentNodeValues.put(node, new PredAndSucc(value));
      previousSuccessor = null;
    } else { // successor
      previousSuccessor = previousValue;
    }

    if (previousSuccessor == null) {
      checkPositive(++successorCount);

      if (orderedNodeConnections != null) {
        orderedNodeConnections.add(new NodeConnection.Succ<>(node));
      }
    }

    // See the comment on the similar cast in removeSuccessor.
    return previousSuccessor == null ? null : (V) previousSuccessor;
  }

  private static boolean isPredecessor(Object value) {
    return (value == PRED) || (value instanceof PredAndSucc);
  }

  private static boolean isSuccessor(Object value) {
    return (value != PRED) && (value != null);
  }
}
