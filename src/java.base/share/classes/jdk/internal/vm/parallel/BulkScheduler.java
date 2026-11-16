/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.vm.parallel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BulkScheduler implements BulkExecutor {
    /** Provider identity and priority are immutable scheduler registration facts. */
    private record Registration(BulkExecutorProvider provider, String id, int priority) {
        void requireStable() {
            if (!id.equals(provider.id()) || priority != provider.priority()) {
                throw new IllegalStateException("bulk provider registration changed: " + id);
            }
        }
    }

    private static final Comparator<Registration> ORDER =
            Comparator.comparingInt(Registration::priority)
                    .reversed()
                    .thenComparing(Registration::id);

    private final List<Registration> registrations;
    private final Registration selected;

    public BulkScheduler(List<? extends BulkExecutorProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        ArrayList<Registration> copy = new ArrayList<>(providers.size());
        HashSet<String> ids = new HashSet<>();
        for (BulkExecutorProvider provider : providers) {
            BulkExecutorProvider checked = Objects.requireNonNull(provider, "provider");
            String id = BulkTask.identifier(checked.id(), "provider");
            int priority = checked.priority();
            if (!ids.add(id)) throw new IllegalArgumentException("duplicate provider: " + id);
            copy.add(new Registration(checked, id, priority));
        }
        if (copy.isEmpty()) throw new IllegalArgumentException("providers");
        copy.sort(ORDER);
        this.registrations = List.copyOf(copy);
        this.selected = null;
    }

    private BulkScheduler(List<Registration> registrations, Registration selected) {
        this.registrations = registrations;
        this.selected = selected;
    }

    /**
     * Returns a scheduler bound to one of this scheduler's registered providers.
     *
     * <p>The returned scheduler retains the original registration identities and
     * priorities. Binding does not change this scheduler or an execution already
     * in progress. An unsupported operation or provider failure is reported to
     * the caller; another provider is never used by a bound scheduler.</p>
     *
     * @param providerId the registered provider identity
     * @return an immutable scheduler selection
     * @throws IllegalArgumentException if the identity is invalid or unregistered
     * @throws IllegalStateException if the provider registration has changed
     */
    public BulkScheduler withProvider(String providerId) {
        String checked = BulkTask.identifier(providerId, "providerId");
        for (Registration item : registrations) {
            if (item.id().equals(checked)) {
                item.requireStable();
                return item == selected ? this : new BulkScheduler(registrations, item);
            }
        }
        throw new IllegalArgumentException("unknown provider: " + checked);
    }

    public List<String> providerIds() {
        return registrations.stream().map(Registration::id).toList();
    }

    public BulkExecutorProvider select(BulkTask task) {
        return registration(task).provider();
    }

    private Registration registration(BulkTask task) {
        Objects.requireNonNull(task, "task");
        if (selected != null) {
            selected.requireStable();
            boolean supported = selected.provider().supports(task);
            selected.requireStable();
            if (supported) return selected;
            throw new IllegalStateException("provider " + selected.id()
                    + " does not support: " + task.operationId());
        }
        for (Registration item : registrations) {
            item.requireStable();
            if (item.provider().supports(task)) {
                item.requireStable();
                return item;
            }
        }
        throw new IllegalStateException("no provider supports: " + task.operationId());
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        // Freeze name-to-object bindings before provider selection. Array/data payloads
        // remain by reference: copying them here would defeat device-resident work.
        Map<String, Object> bindings = Collections.unmodifiableMap(
                new HashMap<>(Objects.requireNonNull(values, "values")));
        for (String input : task.inputs()) {
            if (!bindings.containsKey(input) || bindings.get(input) == null) {
                throw new IllegalArgumentException("missing input: " + input);
            }
        }
        Registration item = registration(task);
        BulkExecution execution =
                Objects.requireNonNull(item.provider().execute(task, bindings), "provider result");
        item.requireStable();
        if (!item.id().equals(execution.provider())) {
            throw new IllegalStateException("provider identity mismatch");
        }
        // Task outputs are already unique and immutable. Check names without copying
        // potentially multi-gigabyte result planes through BulkExecution.outputs().
        Set<String> actual = execution.outputNames();
        if (actual.size() != task.outputs().size()
                || !actual.containsAll(task.outputs())) {
            throw new IllegalStateException("provider output shape mismatch");
        }
        return execution;
    }
}
