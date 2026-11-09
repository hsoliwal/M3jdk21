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

package jdk.internal.m3.paradigm.annotation;

/**
 * AccessLevel - Access modifier for generated code.
 *
 * <h2>From Lombok Pattern</h2>
 * <p>
 * Controls visibility of generated methods and constructors.
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Getter(AccessLevel.PROTECTED)
 * 	private String name;
 *
 * 	&#64;Builder(access = AccessLevel.PACKAGE)
 * 	public class Internal {
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Lombok
  * @author Synexia
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/AccessLevel.java}
 * blob {@code 11fe0331668a839342d089259a01c374b2be1fb5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum AccessLevel {

    /** Public access. */
    PUBLIC,

    /** Protected access. */
    PROTECTED,

    /** Package-private (default) access. */
    PACKAGE,

    /** Private access. */
    PRIVATE,

    /** Do not generate the member. */
    NONE,

    /** Use the module system (Java 9+). */
    MODULE
}
