/*******************************************************************************
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
/** Independently built Apache-2.0 M3 collection primitives. */
module jdk.m3.collections {
    requires transitive jdk.m3.arrays;
    exports jdk.m3.collections;
    exports jdk.m3.collections.bits;
    exports jdk.m3.collections.concurrent;
    exports jdk.m3.collections.graph;
    exports jdk.m3.collections.packed;
    exports jdk.m3.collections.range;
    exports jdk.m3.collections.sketch;
    exports jdk.m3.collections.text;
}
