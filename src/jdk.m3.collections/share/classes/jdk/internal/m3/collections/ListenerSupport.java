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
package jdk.internal.m3.collections;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
/**
 * Thread-safe listener management utility.
 *
 * <p>
 * Provides safe listener registration, removal, and notification with automatic
 * exception handling.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * // Create support for event listeners
 * ListenerSupport<EventListener> support = new ListenerSupport<>();
 *
 * // Add listeners
 * support.add(listener1);
 * support.add(listener2);
 *
 * // Notify all (exceptions swallowed)
 * support.notify(l -> l.onEvent(event));
 *
 * // Notify with error handling
 * support.notify(l -> l.onEvent(event), (l, ex) -> log.warn("Listener failed: {}", ex.getMessage()));
 *
 * // Remove listener
 * support.remove(listener1);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Uses CopyOnWriteArrayList for thread-safe iteration during notification.
 * Adding/removing listeners is safe during notification.
 * </p>
 *
 * @param <T>
 *            listener type
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ListenerSupport.java}
 * blob {@code 3d90dcd3911f15753cfaf43dc73aba014f86b07e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ListenerSupport<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ListenerSupport.class);


    private final List<T> listeners = new CopyOnWriteArrayList<>();

    public ListenerSupport() {
        // Default constructor
    }

    /**
     * Adds a listener.
     *
     * @param listener
     *            listener to add (null ignored)
     * @return this for chaining
     */
    public ListenerSupport<T> add(T listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
        return this;
    }

    /**
     * Adds multiple listeners.
     *
     * @param listeners
     *            listeners to add
     * @return this for chaining
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b29): source-retention
    //FLLDCIM: MRL — T2-L3 for-each → forEach; behaviour preserved
    public final ListenerSupport<T> addAll(T... listeners) {
        if (listeners != null) {
            java.util.Arrays.stream(listeners).forEach(this::add);
        }
        return this;
    }

    /**
     * Removes a listener.
     *
     * @param listener
     *            listener to remove
     * @return true if removed
     */
    public boolean remove(T listener) {
        return listener != null && listeners.remove(listener);
    }

    /**
     * Removes listeners matching a predicate.
     *
     * @param predicate
     *            predicate to match
     * @return number of listeners removed
     */
    public int removeIf(Predicate<T> predicate) {
        if (predicate == null) {
            return 0;
        }
        int sizeBefore = listeners.size();
        listeners.removeIf(predicate);
        return sizeBefore - listeners.size();
    }

    /** Removes all listeners. */
    public void clear() {
        listeners.clear();
    }

    /**
     * Checks if there are any listeners.
     *
     * @return true if no listeners
     */
    public boolean isEmpty() {
        return listeners.isEmpty();
    }

    /**
     * Returns the number of listeners.
     *
     * @return listener count
     */
    public int size() {
        return listeners.size();
    }

    /**
     * Checks if a listener is registered.
     *
     * @param listener
     *            listener to check
     * @return true if registered
     */
    public boolean contains(T listener) {
        return listener != null && listeners.contains(listener);
    }

    /**
     * Notifies all listeners, swallowing any exceptions.
     *
     * <p>
     * All listeners are notified even if some throw exceptions. Exceptions are
     * silently ignored.
     * </p>
     *
     * @param callback
     *            callback to invoke on each listener
     */
    public void notify(Consumer<T> callback) {
        if (callback == null) {
            return;
        }
        for (T listener : listeners) { // NFL — per-element exception swallowing required for listener safety
            try {
                callback.accept(listener);
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        }
    }

    /**
     * Notifies all listeners with exception handling.
     *
     * <p>
     * All listeners are notified even if some throw exceptions. The error handler
     * receives the listener and exception.
     * </p>
     *
     * @param callback
     *            callback to invoke on each listener
     * @param errorHandler
     *            handler for exceptions (receives listener and exception)
     */
    public void notify(Consumer<T> callback, BiConsumer<T, Exception> errorHandler) {
        if (callback == null) {
            return;
        }
        for (T listener : listeners) { // NFL — per-element exception handling with error handler callback
            try {
                callback.accept(listener);
            } catch (Exception e) {
                if (errorHandler != null) {
                    try {
                        errorHandler.accept(listener, e);
                    } catch (Exception ignored) {
                        log.debug("Suppressed: {}", ignored.getMessage(), ignored);
                    }
                }
            }
        }
    }

    /**
     * Notifies all listeners and collects exceptions.
     *
     * <p>
     * All listeners are notified even if some throw exceptions. Returns the count
     * of successful notifications.
     * </p>
     *
     * @param callback
     *            callback to invoke on each listener
     * @return count of successful notifications
     */
    public int notifyAndCount(Consumer<T> callback) {
        if (callback == null) {
            return 0;
        }
        int success = 0;
        for (T listener : listeners) { // NFL — mutable counter with per-element exception handling
            try {
                callback.accept(listener);
                success++;
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        }
        return success;
    }

    /**
     * Notifies listeners until one returns true.
     *
     * <p>
     * Useful for event handlers that can "consume" an event.
     * </p>
     *
     * @param callback
     *            predicate callback (return true to stop)
     * @return true if any listener returned true
     */
    public boolean notifyUntilHandled(Predicate<T> callback) {
        if (callback == null) {
            return false;
        }
        for (T listener : listeners) { // NFL — short-circuit return with per-element exception handling
            try {
                if (callback.test(listener)) {
                    return true;
                }
            } catch (Exception ignored) {
                log.debug("Suppressed: {}", ignored.getMessage(), ignored);
            }
        }
        return false;
    }

    /**
     * Gets an immutable snapshot of current listeners.
     *
     * @return list of listeners
     */
    public List<T> getListeners() {
        return List.copyOf(listeners);
    }

    /**
     * Iterates over all listeners.
     *
     * @param action
     *            action to perform on each listener
     */
    public void forEach(Consumer<T> action) {
        if (action != null) {
            listeners.forEach(action);
        }
    }

    @Override
    public String toString() {
        return "ListenerSupport[size=" + listeners.size() + "]";
    }
}
