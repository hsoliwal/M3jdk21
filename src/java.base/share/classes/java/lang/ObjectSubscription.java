/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package java.lang;

/**
 * The lifetime of an asynchronous object subscription. A live subscription
 * retains its required observation owner. Closing releases this retention and
 * prevents further capture or delivery admission. An already admitted callback
 * may complete.
 *
 * @since 21
 */
public final class ObjectSubscription implements AutoCloseable {
    static final ObjectSubscription CLOSED = new ObjectSubscription(null, -1);

    volatile M3ObjectRuntime.State state;
    final int index;

    ObjectSubscription(M3ObjectRuntime.State state, int index) {
        this.state = state;
        this.index = index;
    }

    /** Returns whether this subscription is closed or inactive. */
    public boolean isClosed() {
        return state == null;
    }

    /** Idempotently ends this subscription. Does not wait for an admitted callback. */
    @Override
    public void close() {
        M3ObjectRuntime.close(this);
    }
}
