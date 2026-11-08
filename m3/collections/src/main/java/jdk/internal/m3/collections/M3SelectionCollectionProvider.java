/*******************************************************************************
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

import java.util.Objects;
import java.util.function.Function;

/**
 * Executable provider. Models must be pure; creators must return a fresh empty instance.
 * Change modelVersion whenever model constants or implementation selection changes.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionProvider.java}
 * blob {@code f2f0b96c3875a237e4de39685eca0ee20532dcaf} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections multi-package batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3SelectionCollectionProvider<T> {
    private final String id;
    private final String modelVersion;
    private final M3CollectionFamily<T> family;
    private final M3CollectionCapabilities capabilities;
    private final Function<M3SelectionCollectionRequest<?>, M3CollectionEstimate> model;
    private final Function<M3SelectionCollectionRequest<?>, ? extends T> creator;

    public M3SelectionCollectionProvider(String id, String modelVersion, M3CollectionFamily<T> family,
            M3CollectionCapabilities capabilities,
            Function<M3SelectionCollectionRequest<?>, M3CollectionEstimate> model,
            Function<M3SelectionCollectionRequest<?>, ? extends T> creator) {
        this.id = Objects.requireNonNull(id, "id");
        this.modelVersion = Objects.requireNonNull(modelVersion, "modelVersion");
        this.family = Objects.requireNonNull(family, "family");
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.model = Objects.requireNonNull(model, "model");
        this.creator = Objects.requireNonNull(creator, "creator");
        if (!id.matches("[a-z][a-z0-9.-]*") || modelVersion.isBlank()) {
            throw new IllegalArgumentException("stable provider ID and modelVersion required");
        }
    }

    public String id() { return id; }
    public String modelVersion() { return modelVersion; }
    public M3CollectionFamily<T> family() { return family; }
    public M3CollectionCapabilities capabilities() { return capabilities; }

    M3CollectionEstimate estimate(M3SelectionCollectionRequest<?> request) {
        return Objects.requireNonNull(model.apply(request), "provider returned a null estimate");
    }

    T create(M3SelectionCollectionRequest<?> request) {
        // Only an immutable, already-filtered plan calls this method.
        if (!family.equals(request.family())) {
            throw new IllegalArgumentException("request is incompatible with " + id);
        }
        return family.type().cast(Objects.requireNonNull(creator.apply(request),
                "provider returned a null collection"));
    }
}
