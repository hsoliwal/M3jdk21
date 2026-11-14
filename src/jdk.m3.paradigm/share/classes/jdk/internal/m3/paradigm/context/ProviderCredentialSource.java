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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;
import java.util.Optional;

/**
 * External credential binding. Secrets are never carried by ContextCapabilityRef
 * or persisted as context semantics.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/capability/ContextProviderCredentialSource.java}
 * blob {@code c0a2245a68d2e6e585cc7d1c8b33d9d2e1e9b671} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface ProviderCredentialSource {
    Optional<String> bearer(ProviderDefinition definition);

    static ProviderCredentialSource none() {
        return definition -> Optional.empty();
    }

    static ProviderCredentialSource fixed(String token) {
        String value = validate(token);
        return definition -> Optional.of(value);
    }

    static String validate(String token) {
        String value = Objects.requireNonNull(token, "token");
        if (value.isBlank() || value.length() > 4096
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid bearer credential");
        }
        return value;
    }
}
