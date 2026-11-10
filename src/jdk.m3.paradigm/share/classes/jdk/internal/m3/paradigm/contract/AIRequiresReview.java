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
/*
 * Copyright 2026 Synexia <hsoliwal@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdk.internal.m3.paradigm.contract;

import java.lang.annotation.*;

/**
 * AIRequiresReview - IOP annotation requiring manual review before applying
 * changes.
 *
 * <h2>IOP Authority: Review Gate</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Agent may generate a plan.                                               ║
 * ║  Agent may generate a JDT Change.                                         ║
 * ║  Agent may NOT apply it automatically.                                    ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 * <li>Public APIs - external contract changes need review</li>
 * <li>Security-sensitive code - auth, crypto, permissions</li>
 * <li>Core infrastructure - foundational code</li>
 * <li>Database schemas - migration impact</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Authentication service")
 *     &#64;AIRequiresReview // No auto-apply, human must approve
 *     public interface IAuthService {
 *
 *         AuthResult authenticate(String user, String password);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - Authority Layer
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIRequiresReview.java}
 * blob {@code 9946e9d8eaedbdd4f9abc60d3c67cc8b6d0af65a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIRequiresReview {

    /**
     * Reason for requiring review (documentation).
     *
     * @return reason string
     */
    String reason() default "";
}
