/* *****************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.m3.collections.guava.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import jdk.m3.collections.guava.collect.ImmutableList;

/**
 * A stable-order mutable value graph from the public builder, driven in lockstep with a model:
 * a node-ordered map of verbatim pre-patch {@link DirectedGraphConnectionsReference} instances
 * updated exactly as {@code StandardMutableValueGraph} updates its connections.
 */
final class GraphTwin {

    private static final int PERCENT = 100;
    private static final int PUT_LIMIT = 55;
    private static final int REMOVE_EDGE_LIMIT = 80;
    private static final int ADD_NODE_LIMIT = 85;
    private static final int HUB_LIMIT = 87;

    private final MutableValueGraph<LaneKey, Object> graph = ValueGraphBuilder.directed()
            .allowsSelfLoops(true).incidentEdgeOrder(ElementOrder.<LaneKey>stable()).build();
    private final Map<LaneKey, DirectedGraphConnectionsReference<LaneKey, Object>> model =
            new LinkedHashMap<>();
    private final int universe;

    GraphTwin(int universe) {
        this.universe = universe;
    }

    void randomOp(Random random, String at) {
        LaneKey u = new LaneKey(random.nextInt(universe), random.nextInt());
        LaneKey v = new LaneKey(random.nextInt(universe), random.nextInt());
        int roll = random.nextInt(PERCENT);
        if (roll < PUT_LIMIT) {
            put(u, v, at + " put " + u + " " + v);
        } else if (roll < REMOVE_EDGE_LIMIT) {
            removeEdge(u, v, at + " removeEdge " + u + " " + v);
        } else if (roll < ADD_NODE_LIMIT) {
            assertEquals(!model.containsKey(u) && connections(u) != null, graph.addNode(u), at);
        } else if (roll < HUB_LIMIT) {
            hub(u, at + " hub " + u);
        } else {
            assertEquals(modelRemoveNode(u), graph.removeNode(u), at + " removeNode " + u);
        }
        compareAll(at);
    }

    private void put(LaneKey u, LaneKey v, String at) {
        Object value = new Object();
        LaneOutcomes.assertSameOutcome(modelPut(u, v, value), graph.putEdgeValue(u, v, value), at);
    }

    private void removeEdge(LaneKey u, LaneKey v, String at) {
        LaneOutcomes.assertSameOutcome(modelRemoveEdge(u, v), graph.removeEdge(u, v), at);
    }

    /** Links {@code hub} both ways to every node, self-loop included, then removes it. */
    private void hub(LaneKey hub, String at) {
        IntStream.range(0, universe).forEach(id -> {
            put(hub, new LaneKey(id, -id), at);
            put(new LaneKey(id, id), hub, at);
        });
        compareAll(at);
        assertEquals(modelRemoveNode(hub), graph.removeNode(hub), at + " removeNode");
    }

    private DirectedGraphConnectionsReference<LaneKey, Object> connections(LaneKey node) {
        return model.computeIfAbsent(node, key -> DirectedGraphConnectionsReference.of(
                ElementOrder.<LaneKey>stable()));
    }

    private Object modelPut(LaneKey u, LaneKey v, Object value) {
        Object previous = connections(u).addSuccessor(v, value);
        connections(v).addPredecessor(u, value);
        return previous;
    }

    private Object modelRemoveEdge(LaneKey u, LaneKey v) {
        DirectedGraphConnectionsReference<LaneKey, Object> cu = model.get(u);
        DirectedGraphConnectionsReference<LaneKey, Object> cv = model.get(v);
        if (cu == null || cv == null) {
            return null;
        }
        Object previous = cu.removeSuccessor(v);
        if (previous != null) {
            cv.removePredecessor(u);
        }
        return previous;
    }

    /** StandardMutableValueGraph.removeNode, step for step, on the model. */
    private boolean modelRemoveNode(LaneKey node) {
        DirectedGraphConnectionsReference<LaneKey, Object> c = model.get(node);
        if (c == null) {
            return false;
        }
        if (c.removeSuccessor(node) != null) {
            c.removePredecessor(node);
        }
        unlink(node, c);
        return model.remove(node) != null;
    }

    /** Successors first, then predecessors, each from a copy, as removeNode does. */
    private void unlink(LaneKey node, DirectedGraphConnectionsReference<LaneKey, Object> c) {
        ImmutableList.copyOf(c.successors()).forEach(successor -> {
            model.get(successor).removePredecessor(node);
            c.removeSuccessor(successor);
        });
        ImmutableList.copyOf(c.predecessors()).forEach(predecessor -> {
            model.get(predecessor).removeSuccessor(node);
            c.removePredecessor(predecessor);
        });
    }

    private void compareAll(String at) {
        LaneOutcomes.assertSameSequence(model.keySet().iterator(), graph.nodes().iterator(),
                at + " nodes");
        model.forEach((node, c) -> compareNode(node, c, at + " node " + node));
        LaneOutcomes.assertSameSequence(modelEdges(), graph.edges().iterator(), at + " edges");
        assertEquals(model.values().stream().mapToInt(c -> c.successors().size()).sum(),
                graph.edges().size(), at + " edge count");
    }

    private void compareNode(LaneKey node, GraphConnections<LaneKey, Object> c, String at) {
        LaneOutcomes.assertSameSequence(c.successors().iterator(),
                graph.successors(node).iterator(), at + " successors");
        LaneOutcomes.assertSameSequence(c.predecessors().iterator(),
                graph.predecessors(node).iterator(), at + " predecessors");
        LaneOutcomes.assertSameSequence(c.adjacentNodes().iterator(),
                graph.adjacentNodes(node).iterator(), at + " adjacent");
        LaneOutcomes.assertSameSequence(c.incidentEdgeIterator(node),
                graph.incidentEdges(node).iterator(), at + " incident");
    }

    private Iterator<EndpointPair<LaneKey>> modelEdges() {
        return model.entrySet().stream()
                .flatMap(e -> e.getValue().successors().stream()
                        .map(successor -> EndpointPair.ordered(e.getKey(), successor)))
                .iterator();
    }

    /**
     * ImmutableValueGraph.copyOf of the graph against the reference ofImmutable over the model's
     * incident edges: the copy keeps the order of the patched graph.
     */
    void compareImmutableCopy(String at) {
        ImmutableValueGraph<LaneKey, Object> copy = ImmutableValueGraph.copyOf(graph);
        model.forEach((node, c) -> {
            GraphConnections<LaneKey, Object> expected = DirectedGraphConnectionsReference
                    .ofImmutable(node, () -> c.incidentEdgeIterator(node), c::value);
            String where = at + " copy " + node;
            LaneOutcomes.assertSameSequence(expected.incidentEdgeIterator(node),
                    copy.incidentEdges(node).iterator(), where + " incident");
            LaneOutcomes.assertSameSequence(expected.successors().iterator(),
                    copy.successors(node).iterator(), where + " successors");
            LaneOutcomes.assertSameSequence(expected.predecessors().iterator(),
                    copy.predecessors(node).iterator(), where + " predecessors");
        });
    }
}
