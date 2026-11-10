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

/**
 * Minimal progress callback that does not introduce a synexia-job dependency cycle.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/locality/LocalityProgressMonitor.java}
 * blob {@code 38c3ed2b9b9e87ba893a92e037f781a536a82965} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface LocalityProgressMonitor {
    void update(String stage, int completed, int total);

    static LocalityProgressMonitor noop() {
        return (stage, completed, total) -> {
            // deliberately empty
        };
    }

    static LocalityProgressMonitor orNoop(LocalityProgressMonitor monitor) {
        return monitor == null ? noop() : monitor;
    }
}
