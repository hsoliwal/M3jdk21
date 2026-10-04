// SPDX-License-Identifier: Apache-2.0
package com.m3.rewrite.pass;

/** Execution character of one bounded M3 refinement pass. */
public enum M3PassMode {
    INVENTORY,
    TRANSFORM,
    RELATION,
    FAN_IN,
    ADMISSION,
    VERIFY
}
