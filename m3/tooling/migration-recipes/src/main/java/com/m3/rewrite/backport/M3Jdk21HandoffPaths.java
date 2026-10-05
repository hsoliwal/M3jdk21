// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.backport;

/** Shared, dependency-free path contract for typed Synexia -> M3JDK21 Java handoffs. */
final class M3Jdk21HandoffPaths {
    private M3Jdk21HandoffPaths() {}

    static boolean javaSource(String value) {
        if (value == null
                || !(value.startsWith("src/")
                        || value.startsWith("test/")
                        || value.startsWith("m3/ports/"))
                || !value.endsWith(".java")
                || value.indexOf('\\') >= 0
                || value.length() > 4096) {
            return false;
        }
        for (String part : value.split("/", -1)) {
            if (!part.matches("[A-Za-z0-9_$.-]+")
                    || ".".equals(part)
                    || "..".equals(part)) {
                return false;
            }
        }
        return value.chars().noneMatch(Character::isISOControl);
    }
}
