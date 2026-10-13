// SPDX-License-Identifier: Apache-2.0
package com.m3.residency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method (or parameter) stores its transient argument beyond the call, so passing a transient handle to it makes the obtaining site RETAINS.
 *
 * <p>CLASS retention: visible to the build-time ASM pass (m3/docs/m3-item-residency-metadata.md),
 * absent at run time.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.PARAMETER})
public @interface M3Retains {
}
