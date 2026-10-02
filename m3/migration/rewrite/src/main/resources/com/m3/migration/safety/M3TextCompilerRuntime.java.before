/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;

/**
 * Explicit compiler-lowering target for operations whose Java evaluation has already produced
 * operands in left-to-right order. This class is a contract surface, not blanket authorization to
 * rewrite arbitrary String expressions.
 */
public final class M3TextCompilerRuntime {
    private M3TextCompilerRuntime() {}

    public static M3Text index(String value) {
        return M3Text.fromString(Objects.requireNonNull(value, "value"));
    }

    /**
     * Converts operands with String.valueOf(Object) in array order and composes the resulting text.
     * A compiler recipe must preserve original operand evaluation, overload resolution and exception
     * timing before calling this helper.
     */
    public static M3Text concatObjects(Object... values) {
        Objects.requireNonNull(values, "values");
        M3Text result = M3Text.empty();
        for (Object value : values) {
            result = result.concat(M3Text.fromString(String.valueOf(value)));
        }
        return result;
    }

    public static String concatObjectsToString(Object... values) {
        return concatObjects(values).asString();
    }
}
