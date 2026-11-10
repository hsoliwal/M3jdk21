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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BulkScheduler implements BulkExecutor {
    private static final Comparator<BulkExecutorProvider> ORDER =
            Comparator.comparingInt(BulkExecutorProvider::priority)
                    .reversed()
                    .thenComparing(BulkExecutorProvider::id);

    private final List<BulkExecutorProvider> providers;

    public BulkScheduler(List<? extends BulkExecutorProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        ArrayList<BulkExecutorProvider> copy = new ArrayList<>(providers.size());
        HashSet<String> ids = new HashSet<>();
        for (BulkExecutorProvider provider : providers) {
            BulkExecutorProvider checked = Objects.requireNonNull(provider, "provider");
            String id = BulkTask.identifier(checked.id(), "provider");
            if (!ids.add(id)) throw new IllegalArgumentException("duplicate provider: " + id);
            copy.add(checked);
        }
        if (copy.isEmpty()) throw new IllegalArgumentException("providers");
        copy.sort(ORDER);
        this.providers = List.copyOf(copy);
    }

    public List<String> providerIds() {
        return providers.stream().map(BulkExecutorProvider::id).toList();
    }

    public BulkExecutorProvider select(BulkTask task) {
        Objects.requireNonNull(task, "task");
        for (BulkExecutorProvider provider : providers) {
            if (provider.supports(task)) return provider;
        }
        throw new IllegalStateException("no provider supports: " + task.operationId());
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        Objects.requireNonNull(values, "values");
        for (String input : task.inputs()) {
            if (!values.containsKey(input) || values.get(input) == null) {
                throw new IllegalArgumentException("missing input: " + input);
            }
        }
        BulkExecutorProvider provider = select(task);
        BulkExecution execution =
                Objects.requireNonNull(provider.execute(task, values), "provider result");
        if (!provider.id().equals(execution.provider())) {
            throw new IllegalStateException("provider identity mismatch");
        }
        Set<String> expected = Set.copyOf(task.outputs());
        if (!execution.outputs().keySet().equals(expected)) {
            throw new IllegalStateException("provider output shape mismatch");
        }
        return execution;
    }
}
