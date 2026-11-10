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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Compact fixed-shape context backed by VarHandle-accessed slots.
 *
 * <p>The key shape is immutable; only slot values change. This keeps the hot context small,
 * predictable and allocation-light while still supporting acquire/release and CAS semantics.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/context/SmallVarHandleContext.java}
 * blob {@code 4c1dc97cbef5da846807687fb9bf347ea73cf103} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class SmallVarHandleContext {
    public static final int MAX_SLOTS = 64;

    private static final VarHandle VALUE_HANDLE = MethodHandles.arrayElementVarHandle(Object[].class);
    private static final VarHandle REVISION_HANDLE;

    static {
        try {
            REVISION_HANDLE = MethodHandles.lookup()
                    .findVarHandle(SmallVarHandleContext.class, "revision", long.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final List<String> keys;
    private final Map<String, Integer> index;
    private final Object[] values;
    @SuppressWarnings("unused")
    private volatile long revision;

    public SmallVarHandleContext(List<String> keys) {
        this(keys, Map.of());
    }

    public SmallVarHandleContext(List<String> keys, Map<String, ?> initialValues) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(initialValues, "initialValues");
        if (keys.isEmpty() || keys.size() > MAX_SLOTS) {
            throw new IllegalArgumentException("CONTEXT_SLOT_COUNT_INVALID:" + keys.size());
        }
        LinkedHashMap<String, Integer> indexes = new LinkedHashMap<>();
        AtomicInteger sequence = new AtomicInteger();
        keys.forEach(key -> {
            String normalized = requireKey(key);
            if (indexes.putIfAbsent(normalized, sequence.getAndIncrement()) != null) {
                throw new IllegalArgumentException("CONTEXT_DUPLICATE_KEY:" + normalized);
            }
        });
        this.keys = List.copyOf(indexes.keySet());
        this.index = Map.copyOf(indexes);
        this.values = new Object[this.keys.size()];
        initialValues.forEach((key, value) -> setRelease(key, value));
        this.revision = 0L;
    }

    public int size() {
        return values.length;
    }

    public List<String> keys() {
        return keys;
    }

    public long revision() {
        return (long) REVISION_HANDLE.getAcquire(this);
    }

    public Object getAcquire(String key) {
        return VALUE_HANDLE.getAcquire(values, slot(key));
    }

    public <T> T getAcquire(String key, Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object value = getAcquire(key);
        return value == null ? null : type.cast(value);
    }

    public void setRelease(String key, Object value) {
        VALUE_HANDLE.setRelease(values, slot(key), value);
        REVISION_HANDLE.getAndAdd(this, 1L);
    }

    public Object getAndSet(String key, Object value) {
        Object previous = VALUE_HANDLE.getAndSet(values, slot(key), value);
        REVISION_HANDLE.getAndAdd(this, 1L);
        return previous;
    }

    public boolean compareAndSet(String key, Object expected, Object replacement) {
        boolean changed = VALUE_HANDLE.compareAndSet(values, slot(key), expected, replacement);
        if (changed) {
            REVISION_HANDLE.getAndAdd(this, 1L);
        }
        return changed;
    }

    public Map<String, Object> snapshot() {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        IntStream.range(0, keys.size())
                .forEach(i -> copy.put(keys.get(i), VALUE_HANDLE.getAcquire(values, i)));
        return java.util.Collections.unmodifiableMap(copy);
    }

    public SmallVarHandleContext copy() {
        SmallVarHandleContext copy = new SmallVarHandleContext(keys);
        IntStream.range(0, values.length)
                .forEach(i -> VALUE_HANDLE.setRelease(copy.values, i, VALUE_HANDLE.getAcquire(values, i)));
        copy.revision = revision();
        return copy;
    }

    private int slot(String key) {
        Integer slot = index.get(requireKey(key));
        if (slot == null) {
            throw new IllegalArgumentException("CONTEXT_UNKNOWN_KEY:" + key);
        }
        return slot;
    }

    private static String requireKey(String key) {
        String normalized = Objects.requireNonNull(key, "key").trim();
        if (normalized.isEmpty() || normalized.length() > 96) {
            throw new IllegalArgumentException("CONTEXT_KEY_INVALID");
        }
        return normalized;
    }
}
