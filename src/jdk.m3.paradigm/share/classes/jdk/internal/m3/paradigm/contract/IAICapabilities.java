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
 * IAICapabilities - AnOP annotation for capability hints.
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
 * 	&#64;code
 * 	&#64;ISpec(value = "Event store with append-only semantics")
 * 	&#64;IAICapabilities({IAICapability.IMMUTABLE_COLLECTIONS, IAICapability.JSON_SERIALIZATION})
 * 	public interface EventStore {
 *
 * 		void append(Event e);
 *
 * 		List&lt;Event> replay(String streamId);
 * 	}
 * }
 * </pre>
 *
 * @since IOP 1.0 - AnOP Capabilities
 * @see IAICapability
 * @m3.pattern synexia:aiop
 * @m3.algorithm Declares required or preferred implementation-capability tokens at a type or method; it resolves no library and establishes no implementation capability.
 * @m3.dataStructures SOURCE-retained annotation targeting {@code {ElementType.TYPE, ElementType.METHOD}}; existing element and enum schemas are preserved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/IAICapabilities.java}
 * blob {@code 7184303bf2799394c661d7f38d0adb4ae3edce3b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface IAICapabilities {

    /**
     * Capabilities this implementation requires or prefers.
     *
     * @return array of capabilities
     * @m3.pattern synexia:aiop
     * @m3.algorithm Declares IAICapabilities#value intent: Capabilities this implementation requires or prefers. Interpretation belongs to consuming configuration or verification tooling; the element has no executable body.
     * @m3.dataStructures Annotation element of type {@code IAICapability[]}; an explicit value is required.
     */
    IAICapability[] value();
}
