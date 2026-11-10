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
 * AIErrorPolicy - AnOP annotation for error handling semantic expectation.
 *
 * <h2>AnOP Advisory: Error Semantics</h2>
 *
 * <pre>
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  Not retry logic — semantic expectation.                                  ║
 * ║  Avoids agents inventing retries.                                         ║
 * ║  Avoids silent swallowing.                                                ║
 * ║  Keeps semantics explicit.                                                ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * @ISpec(value = "Payment processor")
 * @AIErrorPolicy(ErrorPolicy.FAIL_FAST) // No silent failures
 * public interface IPaymentProcessor {
 *
 *     PaymentResult process(Payment payment);
 * }
 * }</pre>
 *
 * @since IOP 1.0 - AnOP Advisory Layer
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/AIErrorPolicy.java}
 * blob {@code 16e29b82092c196454e599d93064038aa4d211fa} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface AIErrorPolicy {

    /**
     * Error handling semantic expectation.
     *
     * @return error policy
     */
    ErrorPolicy value();

    /** Error policy types. */
    enum ErrorPolicy {
        /** Fail immediately on first error. */
        FAIL_FAST,

        /** Propagate errors to caller. */
        PROPAGATE,

        /** Continue processing, log errors. */
        BEST_EFFORT
    }
}
