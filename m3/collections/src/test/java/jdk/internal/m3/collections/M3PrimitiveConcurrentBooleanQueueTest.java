// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
final class M3PrimitiveConcurrentBooleanQueueTest {
 @Test void mpmcBooleanRoundTrip(){var q=new M3PrimitiveConcurrentQueues.BooleanQueue(8,M3PrimitiveConcurrentQueues.Topology.MPMC);assertTrue(q.offer(true));assertTrue(q.offer(false));assertTrue(q.remove());assertFalse(q.remove());}
 @Test void spscBooleanRoundTrip(){var q=new M3PrimitiveConcurrentQueues.BooleanQueue(8,M3PrimitiveConcurrentQueues.Topology.SPSC);assertTrue(q.offer(false));assertTrue(q.offer(true));assertFalse(q.remove());assertTrue(q.remove());}
}
