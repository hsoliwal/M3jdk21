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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * A lazy capability for an existing object's selected fields and cooperative
 * asynchronous change notifications.
 *
 * <p>The object remains the payload owner. Creating this capability does not
 * inspect its fields, traverse its graph, or create observation state. A field
 * is a receiver and an access plan; its VarHandle is resolved on first use.
 * Repeated adaptation returns the same capability while that capability is
 * reachable. Keep the capability or subscription reachable for the required
 * observation lifetime.</p>
 *
 * <p>Observation is enabled by the startup property
 * {@code jdk.m3.objectEvents.enabled=true}, and defaults to disabled. All user
 * callbacks run on framework background dispatch. Notifications are coalesced
 * invalidations, not an audit log or deep snapshots. Changes made through this
 * API and explicit cooperating producers are covered; arbitrary field, array,
 * Unsafe, VarHandle or JNI writes are not automatically intercepted.</p>
 *
 * @since 21
 */
public final class M3Object {
    final Object source;
    volatile M3ObjectRuntime.State state;

    M3Object(Object source) {
        this.source = source;
    }

    /**
     * Returns the identity-bound lazy capability for {@code source}.
     *
     * @param source the original payload owner
     * @return its live capability
     * @throws NullPointerException if source is null
     */
    public static M3Object of(Object source) {
        return M3ObjectRuntime.of(Objects.requireNonNull(source, "source"));
    }

    /** Returns the original payload owner. */
    public Object source() {
        return source;
    }

    /**
     * Describes an instance field without resolving or reading it.
     *
     * <p>The supplied Lookup remains the access authority, including protected
     * receiver restrictions. Only plans with the same Lookup identity and
     * descriptor are shared. A missing, static or inaccessible field is rejected
     * when access or enabled observation first resolves the plan.</p>
     *
     * @param lookup the caller's access authority
     * @param declaringType the field's declaring class
     * @param name the field name
     * @param fieldType the exact field type
     * @return a lazy selected-field capability
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the source is not an instance of
     *         declaringType, or fieldType is void
     */
    public Field field(MethodHandles.Lookup lookup, Class<?> declaringType,
                       String name, Class<?> fieldType) {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(declaringType, "declaringType");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fieldType, "fieldType");
        if (!declaringType.isInstance(source) || fieldType == void.class) {
            throw new IllegalArgumentException("invalid instance field descriptor");
        }
        return new Field(this, M3ObjectRuntime.plan(lookup, declaringType,
                name, fieldType));
    }

    /**
     * Subscribes to publications for this owner and its explicitly followed
     * edges. Root events have no selected field and expose no other caller's
     * field-access capability.
     *
     * @param mask a nonempty subset of ObjectEvent.ALL
     * @param listener the callback
     * @return an idempotently closeable subscription; a shared closed instance
     *         when observation is disabled
     * @throws NullPointerException if listener is null
     * @throws IllegalArgumentException if mask is empty or contains unknown bits
     */
    public ObjectSubscription listen(int mask, ObjectChangeListener listener) {
        return M3ObjectRuntime.listen(this, null, mask, listener);
    }

    /**
     * Reports an already committed logical content change by a cooperating
     * producer. No event owner is created when this object is unobserved.
     */
    public void contentChanged() {
        M3ObjectRuntime.contentChanged(this);
    }

    /**
     * Returns the lazily read startup observation policy. Changes to system
     * properties after VM startup do not enable or disable this policy.
     */
    public static boolean observationEnabled() {
        return M3ObjectRuntime.enabled();
    }

    /** An authorized lazy instance-field location. */
    public static final class Field {
        final M3Object owner;
        final M3ObjectRuntime.AccessPlan plan;

        Field(M3Object owner, M3ObjectRuntime.AccessPlan plan) {
            this.owner = owner;
            this.plan = plan;
        }

        /** Returns the original field receiver. */
        public Object source() {
            return owner.source;
        }

        /** Returns the declared field owner. */
        public Class<?> declaringType() {
            return plan.declaringType;
        }

        /** Returns the field name. */
        public String name() {
            return plan.name;
        }

        /** Returns the exact field type. */
        public Class<?> type() {
            return plan.type;
        }

        VarHandle resolve() {
            VarHandle handle = plan.resolve();
            if (!handle.coordinateTypes().get(0).isInstance(owner.source)) {
                throw new ClassCastException("receiver is outside Lookup access");
            }
            return handle;
        }

        /**
         * Reads the latest value with volatile VarHandle semantics. Primitive
         * values are boxed at this explicit materialization boundary.
         *
         * @throws IllegalArgumentException if the field cannot be resolved;
         *         the cause is the lookup failure
         * @throws ClassCastException if the Lookup restricts this receiver
         */
        public Object get() {
            return resolve().getVolatile(owner.source);
        }

        /**
         * Atomically exchanges the value using VarHandle.getAndSet. Reference
         * identity and primitive raw bits determine whether a change is
         * published. Primitive inputs must have the exact wrapper type.
         *
         * @param value the replacement value
         * @throws NullPointerException if a primitive input is null
         * @throws ClassCastException if the input or receiver has the wrong type
         * @throws IllegalArgumentException if access cannot be resolved
         * @throws UnsupportedOperationException if atomic exchange is unsupported,
         *         including a final field
         */
        public void set(Object value) {
            M3ObjectRuntime.set(this, value);
        }

        /**
         * Performs a VarHandle compare-and-set. A failed comparison and a
         * successful same-identity/same-bit comparison publish nothing.
         * Primitive inputs must have the exact wrapper type.
         *
         * @param expected the expected current value
         * @param value the replacement value
         * @return whether the comparison succeeded
         * @throws NullPointerException if a primitive input is null
         * @throws ClassCastException if an input or receiver has the wrong type
         * @throws IllegalArgumentException if access cannot be resolved
         * @throws UnsupportedOperationException if compare-and-set is unsupported,
         *         including a final field
         */
        public boolean compareAndSet(Object expected, Object value) {
            return M3ObjectRuntime.compareAndSet(this, expected, value);
        }

        /**
         * Subscribes to this field using this caller's access authority.
         * Including CONTENT for a reference field follows the current child's
         * cooperating publications, and rebinds when a covered write replaces
         * that reference. Only explicitly followed edges are traversed.
         *
         * <p>Disabled registration validates only the mask and listener; it does
         * not resolve the field, dereference the child or allocate observer
         * state. Enabled registration resolves access before admission.</p>
         *
         * @param mask a nonempty subset of ObjectEvent.ALL
         * @param listener the callback
         * @return an idempotently closeable subscription
         * @throws NullPointerException if listener is null
         * @throws IllegalArgumentException if mask is invalid or access fails
         * @throws ClassCastException if the Lookup restricts this receiver
         */
        public ObjectSubscription listen(int mask, ObjectChangeListener listener) {
            return M3ObjectRuntime.listen(owner, this, mask, listener);
        }
    }
}
