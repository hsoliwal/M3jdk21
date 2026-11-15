/*
 * Copyright 2026 Hitesh Soliwal and Synexia contributors.
 * Licensed under the Apache License, Version 2.0.
 * You may obtain a copy at https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package jdk.m3.collections.apache.queue;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Retain the incumbent subclass multi-wrap access while converging exact-owner paths. */
class CircularFifoQueueConvergenceCompatibilityTest {
    private static final class WideQueue extends CircularFifoQueue<Object> {
        private static final long serialVersionUID = 1L;
        private boolean wide;
        WideQueue() {
            super(3);
        }
        @Override
        public int size() {
            return super.size() + (wide ? 12 : 0);
        }
    }

    @Test
    void subclassExposedIndicesRetainColdMultiWrapBehavior() {
        final WideQueue queue = new WideQueue();
        final Object first = new Object();
        final Object second = new Object();
        final Object third = new Object();
        queue.add(first);
        queue.add(second);
        queue.add(third);
        assertSame(first, queue.remove());
        queue.wide = true;
        final Object[] cycle = {second, third, null};
        for (int index = 0; index < 14; index++) {
            assertSame(cycle[index % cycle.length], queue.get(index));
        }
        assertThrows(NoSuchElementException.class, () -> queue.get(14));
    }
}
