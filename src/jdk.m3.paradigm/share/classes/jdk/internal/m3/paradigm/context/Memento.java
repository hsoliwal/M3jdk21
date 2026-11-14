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

import java.util.Map;
import java.util.Objects;

/**
 * Immutable, detached data. Holding a memento never grants executable authority.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/Memento.java}
 * blob {@code 9d840a39c7a74c5195d84df470bf83df9d09c5ff} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record Memento(MementoRevision metadata, Map<String, String> cells) {
    public Memento {
        Objects.requireNonNull(metadata, "metadata");
        cells = MementoCodec.copy(cells);
        if (!MementoCodec.digest(cells).equals(metadata.digest())) {
            throw new IllegalArgumentException("Memento checksum mismatch");
        }
    }
}
