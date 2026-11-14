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
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIBindsInstance - AnOP annotation for instance binding at component creation.
 *
 * <h2>Dagger Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ Dagger: @BindsInstance → bind instance at component creation ║
 * ║ AnOP: @AIBindsInstance → compile-time instance binding declaration ║
 * ║ ║
 * ║ Dagger says: "Bind this instance when creating the component" ║
 * ║ AnOP says: "This parameter is bound at component creation" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT bind instances. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIComponent.Factory
 * 	interface Factory {
 *
 * 		AppComponent create(@AIBindsInstance Application app,
 * 				&#64;AIBindsInstance @AIQualifier("baseUrl") String baseUrl);
 * 	}
 *
 * 	&#64;AIComponent.Builder
 * 	interface Builder {
 *
 * 		&#64;AIBindsInstance
 * 		Builder application(Application app);
 *
 * 		&#64;AIBindsInstance
 * 		Builder config(Config config);
 *
 * 		AppComponent build();
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Instance Binding Hints (Dagger-inspired)
 * @see <a href="https://dagger.dev/dev-guide/">Dagger @BindsInstance</a>
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares an instance supplied to a component builder or factory; this annotation stores and injects no instance.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.PARAMETER, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIBindsInstance.java}
 * blob {@code 231a65320dac88bfad4b703c0595a8fd10bde17a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIBindsInstance {

    /**
     * Nullable instance hint.
     *
     * @return true if can be null
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Declares whether the supplied instance may be null; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures boolean annotation element; default expression false.
     */
    boolean nullable() default false;
}
