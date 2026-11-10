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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
/**
 * VarHandleUtils - Utility for common VarHandle patterns.
 *
 * <p>
 * Centralizes VarHandle creation and atomic access patterns used throughout the
 * m3-cop module. Provides common operations:
 * <ul>
 * <li>VarHandle lookup creation with exception handling</li>
 * <li>Common atomic access patterns (getAcquire, setRelease, CAS)</li>
 * <li>Lock-free lazy initialization patterns</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>
 *
 * VarHandle myField = VarHandleUtils.createVarHandle(MyClass.class, "myField", Object.class);
 * Object value = (Object) myField.getAcquire(instance);
 * </pre>
 *
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/utils/VarHandleUtils.java}
 * blob {@code 9dc70f6a059d9f4944bf68dcfec30d1346ee04a5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class VarHandleUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(VarHandleUtils.class);


    private VarHandleUtils() {
        // Utility class - no instantiation
    }

    /**
     * Create a VarHandle for a field with exception handling.
     *
     * <p>
     * Wraps the low-level MethodHandles.Lookup API and handles exceptions. Throws
     * ExceptionInInitializerError if lookup fails (should only happen at class load
     * time).
     *
     * @param targetClass
     *            the class containing the field
     * @param fieldName
     *            the name of the field
     * @param fieldType
     *            the type of the field
     * @return the VarHandle for atomic access
     * @throws ExceptionInInitializerError
     *             if lookup fails
     */
    public static VarHandle createVarHandle(Class<?> targetClass, String fieldName, Class<?> fieldType) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.findVarHandle(targetClass, fieldName, fieldType);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Perform a compare-and-set operation with exactly-once semantics.
     *
     * <p>
     * Common pattern: try CAS in a loop for retry logic.
     *
     * @param handle
     *            the VarHandle
     * @param instance
     *            the instance
     * @param expected
     *            the expected value
     * @param newValue
     *            the new value
     * @return true if successful, false if expected didn't match
     */
    public static boolean compareAndSet(VarHandle handle, Object instance, Object expected, Object newValue) {
        return handle.compareAndSet(instance, expected, newValue);
    }

    /**
     * Get value with acquire semantics (visibility guarantee).
     *
     * <p>
     * Use for reading shared state that may have been written by another thread.
     * Provides acquire semantics: subsequent operations cannot be reordered before
     * this read.
     *
     * @param handle
     *            the VarHandle
     * @param instance
     *            the instance
     * @return the current value
     */
    public static Object getAcquire(VarHandle handle, Object instance) {
        return handle.getAcquire(instance);
    }

    /**
     * Set value with release semantics (visibility guarantee).
     *
     * <p>
     * Use for writing shared state that will be read by another thread. Provides
     * release semantics: all previous operations will be visible to subsequent
     * acquires.
     *
     * @param handle
     *            the VarHandle
     * @param instance
     *            the instance
     * @param value
     *            the new value
     */
    public static void setRelease(VarHandle handle, Object instance, Object value) {
        handle.setRelease(instance, value);
    }

    /**
     * Lazy initialization pattern with CAS retry.
     *
     * <p>
     * Attempts to initialize a field exactly once using compare-and-set. Pattern:
     *
     * <pre>
     * Object current = getAcquire(handle, instance);
     * if (current == null) {
     * 	Object newValue = createValue();
     * 	if (compareAndSet(handle, instance, null, newValue)) {
     * 		return newValue;
     * 	}
     * 	current = getAcquire(handle, instance);
     * }
     * return current;
     * </pre>
     *
     * @param handle
     *            the VarHandle for the lazy field
     * @param instance
     *            the instance
     * @param factory
     *            function to create the value if needed
     * @return the initialized value (either created by this thread or winner of
     *         race)
     */
    public static Object ensureInitialized(VarHandle handle, Object instance, ValueFactory factory) {
        Object current = getAcquire(handle, instance);
        if (current != null) {
            return current;
        }

        Object newValue = factory.create();
        if (compareAndSet(handle, instance, null, newValue)) {
            return newValue;
        }

        return getAcquire(handle, instance);
    }

    /** Factory for creating lazy-initialized values. */
    @FunctionalInterface
    public interface ValueFactory {

        /**
         * Create a new value.
         *
         * @return the created value
         */
        Object create();
    }
}
