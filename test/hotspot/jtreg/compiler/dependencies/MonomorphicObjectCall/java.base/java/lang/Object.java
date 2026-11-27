/*
 * Copyright (c) 1994, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import jdk.internal.vm.annotation.IntrinsicCandidate;
import java.security.SHA256Adapter;

/**
 * Slightly modified version of java.lang.Object that replaces
 * finalize() by finalizeObject() to avoid overriding in subclasses.
 */
public class Object {

    @IntrinsicCandidate
    public Object() {}

    /**
     * Adapt this object to a requested Java type or M3JDK object capability.
     * Assignable types return this receiver without allocation or conversion.
     * An exact request for a JDK-internal object capability type returns its lazy
     * capability; unsupported types return null. Creating that capability does not
     * read fields, index an object graph, or start observation delivery.
     *
     * <p>Capability-bearing subclasses may override this method. No resident
     * state is added to Object, and a nonmatching type is never cast unsafely.
     *
     * @param clazz the requested type
     * @param <T> requested result type
     * @return this instance when assignable, its JDK-internal capability for that
     *         exact type, otherwise null
     * @throws NullPointerException if clazz is null
     */
    public <T> T toAdapter(Class<T> clazz) {
        if (clazz == null) {
            throw new NullPointerException("clazz");
        }
        if (clazz.isInstance(this)) {
            return clazz.cast(this);
        }
        if (clazz == M3Object.class) {
            return clazz.cast(M3Object.of(this));
        }
        return clazz == SHA256Adapter.class
                ? clazz.cast(SHA256Adapter.of(this)) : null;
    }

    /**
     * Adapts this object by delegating to {@link #toAdapter(Class)}.
     *
     * <p>This additional entry delegates virtually to {@code toAdapter}, so
     * subclasses that override that method keep their established semantics.
     * The selected implementation determines the result and exceptions; this
     * method performs no additional argument validation. This method is also
     * overridable and adds no per-object state.
     *
     * @param clazz the requested adapter type
     * @param <T> the requested result type
     * @return the selected adapter result, which may be {@code null}
     * @throws NullPointerException if the selected implementation rejects a null
     *         {@code clazz}; the implementation in {@code Object} rejects null
     */
    public <T> T toAdpater(Class<T> clazz) {
        return toAdapter(clazz);
    }

    @IntrinsicCandidate
    public final native Class<?> getClass();

    @IntrinsicCandidate
    public native int hashCode();

    public boolean equals(Object obj) {
        return (this == obj);
    }

    @IntrinsicCandidate
    protected native Object clone() throws CloneNotSupportedException;

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }

    @IntrinsicCandidate
    public final native void notify();

    @IntrinsicCandidate
    public final native void notifyAll();

    private final native void wait0(long timeout) throws InterruptedException;

    public final void wait(long timeout) throws InterruptedException {
        wait0(timeout);
    }

    public final void wait(long timeout, int nanos) throws InterruptedException {
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && timeout == 0)) {
            timeout++;
        }

        wait0(timeout);
    }

    public final void wait() throws InterruptedException {
        wait0(0);
    }

    /**
     * Replaces original finalize() method and is therefore not
     * overridden by any subclasses of Object.
     * @throws Throwable
     */
    // protected void finalize() throws Throwable { }
    public void finalizeObject() throws Throwable { }
}
