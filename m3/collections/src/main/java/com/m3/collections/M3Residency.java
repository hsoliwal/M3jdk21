// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compile-time hint for M3's opt-in object-residency recipe.
 *
 * <p>This annotation is inert on stock Java. The recipe records its values in hash-pinned
 * metadata; runtime transformation is permitted only when escape and reconstruction proofs pass.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface M3Residency {
    int flags() default M3ObjectResidency.LAZY_CREATE
            | M3ObjectResidency.LRU
            | M3ObjectResidency.PRESSURE_AWARE;
    long estimatedBytes() default -1L;
    String parent() default "";
    Strategy strategy() default Strategy.RECREATE;

    enum Strategy { RECREATE, SWAP, PIN }
}
