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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Application - High-level application intent specification.
 *
 * <h2>From Grand Java Patterns Proof</h2>
 * <p>
 * The ultimate AnOP annotation - describes entire application behavior in
 * natural language. This is the golden test of AnOP perfection.
 * </p>
 *
 * <h2>The Golden Test</h2> <blockquote> If a developer can
 * write @Application("...") and never ask "How does this work internally?" -
 * then AnOP is perfect. </blockquote>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @Application("""
 * 		Ingest text,
 * 		extract meaning,
 * 		store memory,
 * 		allow cancellation,
 * 		observe progress.
 * 		""")
 * public interface MemoryIngestion {
 *
 * 	void ingest(TextInput input);
 * }
 * }</pre>
 *
 * <h2>What This Reads As</h2>
 * <p>
 * This reads like English, not systems code. Users declare what they want; the
 * framework makes it happen.
 * </p>
 *
 * <h2>AnOP Mapping</h2>
 *
 * <pre>
 * | Intent              | AnOP Equivalent    |
 * |---------------------|--------------------|
 * | "extract meaning"   | @LLM               |
 * | "store memory"      | @Contextual        |
 * | "allow cancellation"| @Cancellable       |
 * | "observe progress"  | @Observable        |
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Application
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Application.java}
 * blob {@code edbfbd6400b769fd79296881dda8adba0f8a7977} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Application {

    /**
     * Natural language description of application behavior.
     *
     * @return behavior description
     */
    String value();

    /**
     * Application name.
     *
     * @return name for identification
     */
    String name() default "";

    /**
     * Version identifier.
     *
     * @return semantic version
     */
    String version() default "";
}
