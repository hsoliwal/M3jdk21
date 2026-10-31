// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package org.gradle.tooling.model;

/** Finite API-shape double; does not start or qualify an actual Gradle daemon. */
public interface GradleProject {
    String getName();
    Object getTasks();
    Object getChildren();
}
