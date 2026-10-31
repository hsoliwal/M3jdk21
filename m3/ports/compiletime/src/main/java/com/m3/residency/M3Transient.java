// SPDX-License-Identifier: Apache-2.0
package com.m3.residency;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instances of the annotated type are parent-owned transient handles (parent + index + generation). Every call site that obtains one is classified by the M3 reference-metadata pass; behaviour-only sites may receive recyclable handles, every other site keeps a pinned handle.
 *
 * <p>CLASS retention: visible to the build-time ASM pass (m3/docs/m3-item-residency-metadata.md),
 * absent at run time.</p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface M3Transient {
}
