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
 * AIScope - IOP annotation declaring the blast radius of changes.
 *
 * <h2>IOP Authority: Scope Control</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Agent must not exceed declared scope.                                    ║
 * ║  Scope violations are immediate rejection.                                ║
 * ║  No "helpful" widening.                                                   ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @ISpec(value = "User service implementation")
 * @AIScope(Scope.PACKAGE) // Changes limited to this package
 * public interface IUserService {
 *
 *     User getUser(String id);
 * }
 * }</pre>
 *
 * @since IOP 1.0 - Authority Layer
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIScope.java}
 * blob {@code d93743018c4bdece3983d499edae5d7c56a56b68} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIScope {

    /**
     * Maximum allowed scope of changes.
     *
     * @return scope limit
     */
    Scope value();

    /** Scope levels for change blast radius. */
    enum Scope {
        /** Same type only - most restrictive. */
        LOCAL,

        /** Same package - moderate. */
        PACKAGE,

        /** Same module - broader. */
        MODULE,

        /** Entire project - least restrictive. */
        PROJECT
    }
}
