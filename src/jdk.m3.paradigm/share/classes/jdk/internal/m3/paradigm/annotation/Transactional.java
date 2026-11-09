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
 * Transactional - Marks method/class for transaction management.
 *
 * <h2>From Spring Framework</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;Service
 * 	&#64;Transactional
 * 	public class UserService {
 *
 * 		public void createUser(User user) {
 * 			// runs in transaction
 * 		}
 *
 * 		&#64;Transactional(readOnly = true)
 * 		public User findById(Long id) {
 * 			// read-only transaction
 * 		}
 *
 * 		&#64;Transactional(propagation = Propagation.REQUIRES_NEW)
 * 		public void audit(String action) {
 * 			// new transaction
 * 		}
 * 	}
 * }
 * </pre>
 *
 * @since ContextOS 1.0 - AnOP Spring
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/Transactional.java}
 * blob {@code eacaf5480f6d9db60b93cedceea2875c6c5b68bf} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE) // IOP: Compile-time only
@Documented
public @interface Transactional {

    /**
     * Transaction manager name.
     *
     * @return transaction manager
     */
    String value() default "";

    /**
     * Transaction manager name (alias).
     *
     * @return transaction manager
     */
    String transactionManager() default "";

    /**
     * Propagation behavior.
     *
     * @return propagation
     */
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Isolation level.
     *
     * @return isolation
     */
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Timeout in seconds.
     *
     * @return timeout
     */
    int timeout() default -1;

    /**
     * Read-only transaction.
     *
     * @return true if read-only
     */
    boolean readOnly() default false;

    /**
     * Exceptions that trigger rollback.
     *
     * @return rollback exceptions
     */
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exceptions that don't trigger rollback.
     *
     * @return no-rollback exceptions
     */
    Class<? extends Throwable>[] noRollbackFor() default {};

    /** Propagation behaviors. */
    enum Propagation {
        REQUIRED,
        SUPPORTS,
        MANDATORY,
        REQUIRES_NEW,
        NOT_SUPPORTED,
        NEVER,
        NESTED
    }

    /** Isolation levels. */
    enum Isolation {
        DEFAULT,
        READ_UNCOMMITTED,
        READ_COMMITTED,
        REPEATABLE_READ,
        SERIALIZABLE
    }
}
