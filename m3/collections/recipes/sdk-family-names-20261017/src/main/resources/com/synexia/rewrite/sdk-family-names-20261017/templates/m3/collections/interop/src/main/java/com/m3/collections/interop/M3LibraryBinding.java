/* SPDX-License-Identifier: Apache-2.0; Copyright 2026 Hitesh Soliwal and contributors. */
package com.m3.collections.interop;

import java.util.List;
import java.util.Objects;

/** Exact API-family identity and accompanying M3 storage, not a claim of entire-library equivalence. */
public record M3LibraryBinding(Family family, String factoryName, List<String> parameterTypes,
        String contractType, String adapterType, List<String> m3Owners, Ownership ownership,
        String version) {
    public enum Family {
        JDK("Java Development Kit", "jdk", "java.util."),
        GUAVA("Guava", "guava", "com.google.common."),
        ECLIPSE_COLLECTIONS("Eclipse Collections", "eclipse", "org.eclipse.collections."),
        SPRING_FRAMEWORK("Spring Framework", "spring", "org.springframework."),
        APACHE_COMMONS_COLLECTIONS("Apache Commons Collections", "apache", "org.apache.commons.collections4.");
        private final String displayName, prefix, namespace;
        Family(String displayName, String prefix, String namespace) {
            this.displayName = displayName; this.prefix = prefix; this.namespace = namespace;
        }
        public String displayName() { return displayName; }
    }
    public enum Ownership {
        M3_CREATED, M3_CREATED_WITH_CALLER_LIST_ALIASING, M3_SCALAR_ADAPTER, BORROWED_M3
    }
    public M3LibraryBinding {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(ownership, "ownership");
        parameterTypes = List.copyOf(parameterTypes);
        m3Owners = List.copyOf(m3Owners);
        if (factoryName == null || !factoryName.matches("[A-Za-z_$][A-Za-z0-9_$]*")
                || !factoryName.startsWith(family.prefix)) throw new IllegalArgumentException("factory family/name mismatch");
        typeName(contractType); typeName(adapterType);
        for (String parameter : parameterTypes) typeName(parameter);
        if (!contractType.startsWith(family.namespace) && !contractType.startsWith("java.util."))
            throw new IllegalArgumentException("contract belongs to another family");
        if (m3Owners.isEmpty()) throw new IllegalArgumentException("M3 owner evidence required");
        for (String owner : m3Owners) {
            typeName(owner);
            if (!owner.startsWith("com.m3.collections.M3")) throw new IllegalArgumentException("not an M3 collection owner");
        }
        if (version == null || version.isBlank() || version.chars().anyMatch(Character::isWhitespace))
            throw new IllegalArgumentException("pinned compatibility version required");
    }
    /** Erased binary parameter identity; permits real overloads without name collisions. */
    public String descriptor() { return factoryName + "(" + String.join(",", parameterTypes) + ")"; }

    private static void typeName(String value) {
        if (value == null || !value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+"))
            throw new IllegalArgumentException("invalid binary type name: " + value);
    }
}
