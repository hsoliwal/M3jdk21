// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
final class PrimitiveConcurrentBooleanQueueTest {
 @Test void mpmcBooleanRoundTrip(){var q=new PrimitiveConcurrentQueues.BooleanQueue(8,PrimitiveConcurrentQueues.Topology.MPMC);assertTrue(q.offer(true));assertTrue(q.offer(false));assertTrue(q.remove());assertFalse(q.remove());}
 @Test void spscBooleanRoundTrip(){var q=new PrimitiveConcurrentQueues.BooleanQueue(8,PrimitiveConcurrentQueues.Topology.SPSC);assertTrue(q.offer(false));assertTrue(q.offer(true));assertFalse(q.remove());assertTrue(q.remove());}
}
