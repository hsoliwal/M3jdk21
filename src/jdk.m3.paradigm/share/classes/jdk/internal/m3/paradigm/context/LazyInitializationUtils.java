/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.lang.invoke.VarHandle;
import java.util.Optional;
import java.util.function.Supplier;
/**
 * LazyInitializationUtils - Utility for common lazy-loading patterns.
 *
 * <p>
 * Centralizes the lock-free, compare-and-swap based lazy initialization
 * patterns used throughout the m3-cop module. Provides:
 * <ul>
 * <li>Safe lazy initialization with CAS (Compare-And-Set)</li>
 * <li>Double-checked locking via VarHandle acquire/release semantics</li>
 * <li>Callback hooks for initialization events</li>
 * <li>Thread-safe eviction patterns</li>
 * </ul>
 *
 * <h2>Core Pattern: Lazy Load with CAS</h2>
 *
 * <pre>
 * // Fast path: check if loaded
 * Object loaded = handle.getAcquire(this);
 * if (loaded != null)
 * 	return loaded;
 *
 * // Slow path: create new value
 * Object newValue = factory.create();
 *
 * // Try to CAS: only first thread wins
 * if (handle.compareAndSet(this, null, newValue)) {
 * 	onInitialized(newValue);
 * 	return newValue;
 * }
 *
 * // Another thread won: return their value
 * return (Object) handle.getAcquire(this);
 * </pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 * <li>No explicit locks (lock-free)</li>
 * <li>Thread-safe initialization with clear semantics</li>
 * <li>Minimal overhead for fast path (already loaded)</li>
 * <li>Predictable behavior (no deadlocks, no fairness issues)</li>
 * </ul>
 *
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/utils/LazyInitializationUtils.java}
 * blob {@code 08fc8373134993cd4248df55496c8e5972b49383} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class LazyInitializationUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(LazyInitializationUtils.class);


    private LazyInitializationUtils() {
        // Utility class - no instantiation
    }

    /**
     * Lazy initialization pattern with CAS and callback.
     *
     * <p>
     * Thread-safe initialization of a nullable field. Only the thread that wins the
     * CAS operation will invoke the callback.
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @param factory
     *            creates new value if field is null
     * @param callback
     *            invoked only by the thread that wins the CAS
     * @param <T>
     *            the value type
     * @return the initialized value (created by winner of race)
     */
    @SuppressWarnings("unchecked")
    public static <T> T ensureInitializedWithCallback(
            VarHandle handle, Object instance, Supplier<T> factory, InitializationCallback<T> callback) {
        // Fast path: check if already initialized (acquire semantics)
        T current = (T) handle.getAcquire(instance);
        if (current != null) {
            return current;
        }

        // Slow path: create new value
        T newValue = factory.get();

        // Try CAS: only first thread wins and invokes callback
        if (handle.compareAndSet(instance, null, newValue)) {
            callback.onInitialized(newValue);
            return newValue;
        }

        // Another thread won: return their value
        return (T) handle.getAcquire(instance);
    }

    /**
     * Lazy initialization pattern without callback.
     *
     * <p>
     * Simpler variant when you don't need initialization callback.
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @param factory
     *            creates new value if field is null
     * @param <T>
     *            the value type
     * @return the initialized value
     */
    @SuppressWarnings("unchecked")
    public static <T> T ensureInitialized(VarHandle handle, Object instance, Supplier<T> factory) {
        T current = (T) handle.getAcquire(instance);
        if (current != null) {
            return current;
        }

        T newValue = factory.get();
        if (handle.compareAndSet(instance, null, newValue)) {
            return newValue;
        }

        return (T) handle.getAcquire(instance);
    }

    /**
     * Eviction pattern with CAS loop.
     *
     * <p>
     * Atomically evict (clear) a lazy-initialized field. Pattern:
     *
     * <pre>
     * Object current;
     * do {
     * 	current = handle.getAcquire(instance);
     * 	if (current == null)
     * 		return; // Already evicted
     * } while (!handle.compareAndSet(instance, current, null));
     * callback.onEvicted(current);
     * </pre>
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @param callback
     *            invoked with the evicted value
     * @param <T>
     *            the value type
     */
    @SuppressWarnings("unchecked")
    // NFL: CAS retry loop — atomic compare-and-set requires spin — loop preserved per M3 §3
    public static <T> void evictWithCallback(VarHandle handle, Object instance, EvictionCallback<T> callback) {
        Object current;
        do {
            current = handle.getAcquire(instance);
            if (current == null) {
                return; // Already evicted or never loaded
            }
        } while (!handle.compareAndSet(instance, current, null));

        // Successfully evicted: invoke callback
        callback.onEvicted((T) current);
    }

    /**
     * Simple eviction pattern without callback.
     *
     * <p>
     * Atomically clears the field, returning the old value if available.
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @param <T>
     *            the value type
     * @return optional containing the evicted value if it was loaded
     */
    @SuppressWarnings("unchecked")
    // NFL: CAS retry loop — atomic compare-and-set requires spin — loop preserved per M3 §3
    public static <T> Optional<T> evict(VarHandle handle, Object instance) {
        Object current;
        do {
            current = handle.getAcquire(instance);
            if (current == null) {
                return Optional.empty();
            }
        } while (!handle.compareAndSet(instance, current, null));

        return Optional.of((T) current);
    }

    /**
     * Check if a field is initialized (non-null).
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @return true if field is non-null
     */
    public static boolean isInitialized(VarHandle handle, Object instance) {
        return handle.getAcquire(instance) != null;
    }

    /**
     * Get the current value with acquire semantics (or empty if null).
     *
     * @param handle
     *            the VarHandle for the field
     * @param instance
     *            the instance containing the field
     * @param <T>
     *            the value type
     * @return optional containing the value if non-null
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> getIfInitialized(VarHandle handle, Object instance) {
        T value = (T) handle.getAcquire(instance);
        return Optional.ofNullable(value);
    }

    /**
     * Callback for initialization events.
     *
     * @param <T>
     *            the value type
     */
    @FunctionalInterface
    public interface InitializationCallback<T> {

        /**
         * Called after successful initialization (won the CAS race).
         *
         * @param value
         *            the newly initialized value
         */
        void onInitialized(T value);
    }

    /**
     * Callback for eviction events.
     *
     * @param <T>
     *            the value type
     */
    @FunctionalInterface
    public interface EvictionCallback<T> {

        /**
         * Called after successful eviction.
         *
         * @param value
         *            the evicted value
         */
        void onEvicted(T value);
    }
}
