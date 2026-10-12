/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable name of a kernel. The CPU reference backend resolves it to a registered {@link JavaKernel}; native backends
 * resolve it to a precompiled kernel in their bundle.
 *
 * @param name identifier matching {@code [A-Za-z0-9._-]{1,128}}
 */
public record KernelId(String name) {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9._-]{1,128}");

    /**
     * Validates the name.
     *
     * @param name identifier matching {@code [A-Za-z0-9._-]{1,128}}
     */
    public KernelId {
        Objects.requireNonNull(name, "name");
        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("kernel id must match [A-Za-z0-9._-]{1,128}: " + name);
        }
    }

    /**
     * Creates a kernel id.
     *
     * @param name identifier matching {@code [A-Za-z0-9._-]{1,128}}
     * @return the id
     */
    public static KernelId of(String name) {
        return new KernelId(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
