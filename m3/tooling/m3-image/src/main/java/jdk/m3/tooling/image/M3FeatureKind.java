/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Conservatively detected dynamic boundary. */
public enum M3FeatureKind {
    REFLECTION,
    CLASS_LOADING,
    METHOD_HANDLES,
    INVOKEDYNAMIC,
    DYNAMIC_CONSTANT,
    DYNAMIC_PROXY,
    SERVICE_LOADER,
    RESOURCE_LOOKUP,
    JNI_NATIVE_METHOD,
    NATIVE_LIBRARY_LOAD
}
