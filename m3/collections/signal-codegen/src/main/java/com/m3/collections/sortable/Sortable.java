// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package com.m3.collections.sortable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-in source-time generation of structural signals and an optional absolute numeric signal.
 * The declared {@code signal} names an accessible instance numeric field or zero-arg method.
 * A signal is an ordering coordinate, never object identity or a promise of semantic distance.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Sortable {
    String signal() default "";
}
