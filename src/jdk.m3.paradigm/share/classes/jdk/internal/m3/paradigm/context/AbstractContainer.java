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

import jdk.internal.m3.collections.TokenInterner;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * AbstractContainer - Base implementation for IContainer.
 *
 * <p>Provides IoC container with template method pattern:
 * <ul>
 *   <li>Thread-safe bindings via ConcurrentHashMap</li>
 *   <li>Hierarchical scope support (parent-child containers)</li>
 *   <li>Compile-time preferred over runtime binding</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doGet(Class)} - Custom resolution logic</li>
 *   <li>{@link #doGet(Class, String)} - Custom qualified resolution</li>
 *   <li>{@link #doBind(Class, Supplier)} - Custom binding logic</li>
 *   <li>{@link #onResolved(Class, Object)} - Callback after resolution</li>
 *   <li>{@link #onBound(Class)} - Callback after binding</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/factory/AbstractContainer.java}
 * blob {@code 5b385b1121fccb481cc2189921a2159e23bdc757} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractContainer implements IContainer {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractContainer.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String ERR_NO_BINDING = INTERNER.intern("No binding for type: ");
    private static final String WITH_QUALIFIER = INTERNER.intern(" with qualifier: ");
    private static final String ERR_TYPE_SUPPLIER_NULL = INTERNER.intern("type and supplier cannot be null");
    private static final String ERR_TYPE_QUAL_SUPPLIER_NULL = INTERNER.intern("type, qualifier, and supplier cannot be null");
    private static final String PREFIX_CONTAINER = INTERNER.intern("Container[bindings=");
    private static final String QUALIFIED_EQ = INTERNER.intern(", qualified=");
    private static final String HAS_PARENT_EQ = INTERNER.intern(", hasParent=");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");

    // --- STATE ---
    private final Map<Class<?>, Supplier<?>> bindings = new ConcurrentHashMap<>();
    private final Map<String, Map<Class<?>, Supplier<?>>> qualifiedBindings = new ConcurrentHashMap<>();
    private volatile IContainer parent;

    /** Create a container. */
    protected AbstractContainer() {
        // Default constructor
    }

    /**
     * Create a container with parent.
     *
     * @param parent the parent container
     */
    protected AbstractContainer(IContainer parent) {
        this.parent = parent;
    }

    @Override
    public <T> T get(Class<T> type) {
        // Allow subclass to handle resolution
        Optional<T> custom = doGet(type);
        if (custom.isPresent()) {
            onResolved(type, custom.get());
            return custom.get();
        }

        // Check local bindings
        @SuppressWarnings("unchecked")
        Supplier<T> supplier = (Supplier<T>) bindings.get(type);
        if (supplier != null) {
            T instance = supplier.get();
            onResolved(type, instance);
            return instance;
        }

        // Delegate to parent
        if (parent != null) {
            return parent.get(type);
        }

        throw new IllegalArgumentException(ERR_NO_BINDING + type.getName());
    }

    @Override
    public <T> T get(Class<T> type, String qualifier) {
        // Allow subclass to handle resolution
        Optional<T> custom = doGet(type, qualifier);
        if (custom.isPresent()) {
            onResolved(type, custom.get());
            return custom.get();
        }

        // Check qualified bindings
        Map<Class<?>, Supplier<?>> qualified = qualifiedBindings.get(qualifier);
        if (qualified != null) {
            @SuppressWarnings("unchecked")
            Supplier<T> supplier = (Supplier<T>) qualified.get(type);
            if (supplier != null) {
                T instance = supplier.get();
                onResolved(type, instance);
                return instance;
            }
        }

        // Delegate to parent
        if (parent != null) {
            return parent.get(type, qualifier);
        }

        throw new IllegalArgumentException(
                ERR_NO_BINDING + type.getName() + WITH_QUALIFIER + qualifier);
    }

    @Override
    public <T> Optional<T> tryGet(Class<T> type) {
        try {
            return Optional.of(get(type));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public <T> void bind(Class<T> type, Supplier<T> supplier) {
        if (type == null || supplier == null) {
            throw new NullPointerException(ERR_TYPE_SUPPLIER_NULL);
        }
        doBind(type, supplier);
        bindings.put(type, supplier);
        onBound(type);
    }

    /**
     * Bind a type with qualifier.
     *
     * @param <T>       the service type
     * @param type      the service class
     * @param qualifier the qualifier name
     * @param supplier  the supplier
     */
    public <T> void bind(Class<T> type, String qualifier, Supplier<T> supplier) {
        if (type == null || qualifier == null || supplier == null) {
            throw new NullPointerException(ERR_TYPE_QUAL_SUPPLIER_NULL);
        }
        doBind(type, supplier);
        qualifiedBindings.computeIfAbsent(qualifier, k -> new ConcurrentHashMap<>())
                .put(type, supplier);
        onBound(type);
    }

    @Override
    public IContainer createChild() {
        return new ChildContainer(this);
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Custom resolution logic.
     *
     * @param <T>  the service type
     * @param type the service class
     * @return custom resolved instance, or empty to use default
     */
    protected <T> Optional<T> doGet(Class<T> type) {
        return Optional.empty(); //FLLDCIM: MRL — FM-36 return null -> Optional.empty()
    }

    /**
     * Custom qualified resolution logic.
     *
     * @param <T>       the service type
     * @param type      the service class
     * @param qualifier the qualifier
     * @return custom resolved instance, or empty to use default
     */
    protected <T> Optional<T> doGet(Class<T> type, String qualifier) {
        return Optional.empty(); //FLLDCIM: MRL — FM-36 return null -> Optional.empty()
    }

    /**
     * Custom binding logic.
     *
     * @param <T>      the service type
     * @param type     the service class
     * @param supplier the supplier
     */
    protected <T> void doBind(Class<T> type, Supplier<T> supplier) {
        // Default: no-op
    }

    /**
     * Callback after type is resolved.
     *
     * @param <T>      the service type
     * @param type     the service class
     * @param instance the resolved instance
     */
    protected <T> void onResolved(Class<T> type, T instance) {
        // Default: no-op
    }

    /**
     * Callback after type is bound.
     *
     * @param <T>  the service type
     * @param type the service class
     */
    protected <T> void onBound(Class<T> type) {
        // Default: no-op
    }

    // --- Accessors for Subclasses ---

    /**
     * Get the bindings map.
     *
     * @return unmodifiable view of bindings
     */
    protected Map<Class<?>, Supplier<?>> getBindings() {
        return Map.copyOf(bindings);
    }

    /**
     * Get the parent container.
     *
     * @return the parent container or null
     */
    protected IContainer getParent() {
        return parent;
    }

    /**
     * Get the binding count.
     *
     * @return number of registered bindings
     */
    protected int bindingCount() {
        return bindings.size();
    }

    /**
     * Check if a type is bound locally.
     *
     * @param type the service class
     * @return true if bound locally
     */
    protected boolean hasLocalBinding(Class<?> type) {
        return bindings.containsKey(type);
    }

    @Override
    public String toString() {
        return PREFIX_CONTAINER + bindings.size() +
                QUALIFIED_EQ + qualifiedBindings.size() +
                HAS_PARENT_EQ + (parent != null) + BRACKET_CLOSE;
    }

    /** Child container implementation. */
    private static class ChildContainer extends AbstractContainer {

        ChildContainer(IContainer parent) {
            super(parent);
        }
    }
}
