// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package org.apache.maven.model.building;

/** Finite API-shape double; not the Maven library or a model-builder proof. */
public interface ModelBuildingResult {
    Object getEffectiveModel();
}
