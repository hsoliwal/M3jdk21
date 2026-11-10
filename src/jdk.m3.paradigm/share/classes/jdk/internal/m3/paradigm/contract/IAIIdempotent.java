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
 * AIIdempotent - IOP annotation requiring idempotent code generation.
 *
 * <h2>IOP Authority: Idempotence Contract</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Applying the same plan twice must result in the same code.               ║
 * ║  If idempotence cannot be proven → reject.                                ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 * <li>Retries without fear</li>
 * <li>Safe re-planning</li>
 * <li>Deterministic CI runs</li>
 * <li>Reproducible builds</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 * 	&#64;code
 * 	&#64;ISpec(value = "Data mapper")
 * 	&#64;AIIdempotent // Re-running generation must produce identical output
 * 	public interface IUserMapper&lt;Self extends IUserMapper&lt;Self>> extends IContext&lt;Self> {
 *
 * 		UserDTO toDto(User user);
 *
 * 		User toEntity(UserDTO dto);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - Authority Layer
 * @m3.pattern synexia:iop
 * @m3.algorithm Declares the requirement that repeated application of the same generation plan leave identical code. The annotation records metadata and does not itself perform enforcement or generation.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}, preserving the declared element and nested-enum schema.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAIIdempotent.java}
 * blob {@code 1906d465fc0794129ef2b0eec9083683c2f76e13} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAIIdempotent {}
