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
 * AICapabilities - AnOP annotation for capability hints.
 *
 * <h2>AnOP Principle: Hints Are Advisory</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  IOP decides legality.                                                    ║
 * ║  AnOP suggests strategy.                                                  ║
 * ║  JDT decides reality.                                                     ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 * <li>Advisory only - agent may ignore</li>
 * <li>JDT legality still applies</li>
 * <li>Build must still pass without hints</li>
 * <li>If hint cannot be satisfied → no failure, just fallback</li>
 * <li>Hints never add new dependencies unless build already declares them</li>
 * </ul>
 *
 * <h2>What Agent May Do With Hints</h2>
 * <ul>
 * <li>✅ Choose between equivalent implementations</li>
 * <li>✅ Select stdlib vs third-party</li>
 * <li>✅ Optimize for availability</li>
 * <li>❌ Add new dependencies without permission</li>
 * <li>❌ Bypass style or policy</li>
 * <li>❌ Change operation choice</li>
 * <li>❌ Escalate scope</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 *
 * {
 *     &#64;code
 *     &#64;ISpec(value = "Event store with append-only semantics")
 *     &#64;AICapabilities({AICapability.IMMUTABLE_COLLECTIONS,
 *             AICapability.JSON_SERIALIZATION})
 *     public interface EventStore {
 *
 *         void append(Event e);
 *
 *         List&lt;Event> replay(String streamId);
 *     }
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Capabilities
 * @see AICapability
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AICapabilities.java}
 * blob {@code 9c69d52bfac7d67c73460e584b8b719bb6dae748} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AICapabilities {

    /**
     * Capabilities this implementation requires or prefers.
     *
     * @return array of capabilities
     */
    AICapability[] value();
}
