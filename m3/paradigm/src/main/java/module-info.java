/* *****************************************************************************
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

/**
 * M3 programming paradigms absorbed from Synexia: annotation-oriented (AnOP), interface-oriented (IOP) and
 * context-oriented (COP) programming. User decision 2026-11-09 ("New jdk.m3.paradigm module"): the implementation
 * lives in the internal packages {@code jdk.internal.m3.paradigm.annotation}, {@code .contract} and {@code .context};
 * nothing is exported until the user admits public API.
 */
module jdk.m3.paradigm {
    requires jdk.m3.collections;
}
