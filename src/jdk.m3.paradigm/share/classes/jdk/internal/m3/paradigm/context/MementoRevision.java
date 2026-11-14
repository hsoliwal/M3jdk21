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

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable revision metadata; an origin is a reference, not inherited authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MementoRevision.java}
 * blob {@code 0fd99839c55702903ee759bdae451188bc3c37bd} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MementoRevision(UUID uid, long revision, UUID operationId, String eventType,
        Instant recordedAt, String digest, UUID originUid, long originRevision) {
    public MementoRevision {
        Objects.requireNonNull(uid, "uid");
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        Objects.requireNonNull(digest, "digest");
        Objects.requireNonNull(eventType, "eventType");
        if (revision < 0 || eventType.isBlank() || eventType.length() > 256
                || !digest.matches("[0-9a-f]{64}")
                || (originUid == null ? originRevision != -1 : originRevision < 0)) {
            throw new IllegalArgumentException("Invalid memento metadata");
        }
    }
}
