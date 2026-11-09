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
 * Repository - Marks class as a data access component.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>{@code
 * &#64;Repository
 * public class UserRepository {
 *     public User findById(Long id) { ... }
 *     public void save(User user) { ... }
 * }
 * }</pre>
 *
 * <h2>Semantic Meaning</h2>
 * <p>
 * Indicates that the class provides data access. This is a specialization
 * of @Component for the persistence layer. Also enables exception translation.
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Repository.java}
 * blob {@code cffc7664fae2bde38a5c21286d479fd75534c61f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
@Component
public @interface Repository {

    /**
     * Repository name.
     *
     * @return repository name
     */
    String value() default "";
}
