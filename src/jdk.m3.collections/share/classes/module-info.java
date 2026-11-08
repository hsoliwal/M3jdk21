// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * jdk.m3.collections: the M3 collections lane (arrays, collections, algorithms) as one JDK module, moved area by area
 * from the m3/ side tree by m3/tooling/jdk-naming/move_module_area.py (approved layout 2026-11-08). Package
 * names are unchanged (they are API); each area's Maven module becomes a bridge pom over these sources until
 * every consumer has moved.
 */
module jdk.m3.collections {
    exports jdk.m3.arrays;
    exports jdk.m3.arrays.packed;
}
