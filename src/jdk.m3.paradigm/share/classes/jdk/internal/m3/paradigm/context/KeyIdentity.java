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

/**
 * Internal operations over ContextKey's virtual namespace:name; no extra resident key fields.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/context/ContextKeyIdentity.java}
 * blob {@code 8ea872e38d5e6afae663f8c6bc7129c47fa75062} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class KeyIdentity {
    private KeyIdentity() { }

    /** Same integer comparison result as (namespace + ":" + name).compareTo(otherId). */
    static int compare(String namespace, String name, String otherNamespace, String otherName) {
        if (namespace.equals(otherNamespace)) return name.compareTo(otherName);
        int length = namespace.length() + 1 + name.length();
        int otherLength = otherNamespace.length() + 1 + otherName.length();
        int common = Math.min(length, otherLength);
        for (int at = 0; at < common; at++) {
            int difference = unit(namespace, name, at) - unit(otherNamespace, otherName, at);
            if (difference != 0) return difference;
        }
        return length - otherLength;
    }

    /** Exact Objects.hash(namespace + ":" + name, type), including Java int overflow. */
    static int hash(String namespace, String name, Class<?> type) {
        int hash = 31 * namespace.hashCode() + ':';
        for (int at = 0; at < name.length(); at++) hash = 31 * hash + name.charAt(at);
        return 31 * (31 + hash) + type.hashCode();
    }

    /** Exact non-empty ASCII [A-Za-z0-9_.-]+ token contract, without regex allocation. */
    static boolean isToken(String value) {
        if (value.isEmpty()) return false;
        for (int at = 0; at < value.length(); at++) {
            char c = value.charAt(at);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '_' || c == '.' || c == '-')) return false;
        }
        return true;
    }

    private static char unit(String namespace, String name, int at) {
        if (at < namespace.length()) return namespace.charAt(at);
        return at == namespace.length() ? ':' : name.charAt(at - namespace.length() - 1);
    }
}
