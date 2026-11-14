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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AIDisposes - AnOP annotation for CDI-style disposer hints.
 *
 * <h2>Jakarta CDI Inspiration</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║ CDI: @Disposes → disposer method for cleanup ║
 * ║ AnOP: @AIDisposes → compile-time disposer declaration ║
 * ║ ║
 * ║ CDI says: "Call this when the produced instance needs cleanup" ║
 * ║ AnOP says: "This parameter indicates a disposer method" ║
 * ║ ║
 * ║ CRITICAL: AnOP does NOT call disposers. It declares INTENT. ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * {@code
 *
 * // Producer
 * &#64;AIProduces
 * Connection produceConnection() {
 *     return dataSource.getConnection();
 * }
 *
 * // Disposer - called when Connection scope ends
 * void closeConnection(@AIDisposes Connection conn) {
 *     conn.close();
 * }
 *
 * // Qualified disposer
 * void closePooledConnection(
 *         @AIDisposes @AIQualifier("pooled") Connection conn) {
 *     connectionPool.release(conn);
 * }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Disposer Hints (CDI-inspired)
 * @see <a href=
 *      "https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html">Jakarta
 *      CDI</a>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIDisposes.java}
 * blob {@code c81fa5ee8a2f1f6c5ae8a5b1f207d6795aebf5c5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIDisposes {}
