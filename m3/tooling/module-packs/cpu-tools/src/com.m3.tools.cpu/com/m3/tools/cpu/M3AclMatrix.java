// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package com.m3.tools.cpu;

import java.util.Objects;

/** Static role-policy bitmask projection with explicit deny precedence.
 * A policy-result helper, not authentication, sandboxing or an authorization server.
 */
public final class M3AclMatrix {
    private M3AclMatrix() { }

    /**
     * grants/denials indexed by resource*permissions+action; each long is 64 role bits.
     * Returns a flat resource/action allow mask with fail-closed explicit deny precedence.
     */
    public static long[] allowed(int resources, int permissions, long[] grants,
                                 long[] denials, long activeRoles) {
        Objects.requireNonNull(grants); Objects.requireNonNull(denials);
        if (resources < 0 || resources > 100_000 || permissions < 1 || permissions > 64)
            throw new IllegalArgumentException("ACL geometry");
        int count = Math.multiplyExact(resources, permissions);
        if (count > 4_000_000 || grants.length != count || denials.length != count)
            throw new IllegalArgumentException("ACL lane length");
        long[] results = new long[(count + 63) / 64];
        for (int slot = 0; slot < count; slot++) {
            if ((grants[slot] & activeRoles) != 0 && (denials[slot] & activeRoles) == 0)
                results[slot >>> 6] |= 1L << (slot & 63);
        }
        return results;
    }

    public static boolean at(long[] mask, int resources, int resource, int permission, int permissions) {
        Objects.requireNonNull(mask);
        if (resources < 0 || resource < 0 || resource >= resources
                || permission < 0 || permission >= permissions || permissions < 1)
            throw new IllegalArgumentException("ACL lookup");
        int slot = Math.addExact(Math.multiplyExact(resource, permissions), permission);
        if (mask.length != (Math.multiplyExact(resources, permissions) + 63) / 64)
            throw new IllegalArgumentException("ACL mask geometry");
        return (mask[slot >>> 6] & (1L << (slot & 63))) != 0;
    }
}
