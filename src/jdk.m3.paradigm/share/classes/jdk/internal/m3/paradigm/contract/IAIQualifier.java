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
 * AIQualifier - AnOP annotation for disambiguating multiple implementations.
 *
 * <h2>Spring Inspiration, IOP Authority</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ SPRING: @Qualifier → runtime selection among candidates ║
 * ║ AnOP: @AIQualifier → compile-time disambiguation hint ║
 * ║ ║
 * ║ Spring says: "Inject the bean with this qualifier" ║
 * ║ AnOP says: "This implementation is identified by this qualifier" ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;AIQualifier("mysql")
 * 	public interface IMySqlRepository extends IRepository {
 * 	}
 *
 * 	&#64;AIQualifier("postgres")
 * 	public interface IPostgresRepository extends IRepository {
 * 	}
 *
 * 	// In dependent code:
 * 	&#64;AIDependency(qualifier = "mysql")
 * 	IRepository repository;
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Qualifier Hints (Spring-inspired)
 * @m3.pattern spring:dependency-injection
 * @m3.algorithm Declares a name that disambiguates injection candidates; this annotation performs no candidate lookup or selection.
 * @m3.dataStructures SOURCE-retained annotation targeting {ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}; its elements and nested declarations are metadata only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIQualifier.java}
 * blob {@code cf5ed34e1312cc93141f48e39511fa26b0dd04c1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIQualifier {

    /**
     * Qualifier value for disambiguation.
     *
     * @return qualifier
     * @m3.pattern spring:dependency-injection
     * @m3.algorithm Names the qualifier used to distinguish implementation candidates; this annotation element performs no corresponding runtime action.
     * @m3.dataStructures String annotation element; explicit value required, with no default.
     */
    String value();
}
