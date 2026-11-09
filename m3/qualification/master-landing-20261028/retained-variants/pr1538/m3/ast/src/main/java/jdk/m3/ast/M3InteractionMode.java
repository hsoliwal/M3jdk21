// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors.
package jdk.m3.ast;

/** Operation relating two immutable indexed values. */
public enum M3InteractionMode {
    ASSERT,
    SUPPORT,
    OPPOSE,
    COMPARE,
    DERIVE,
    EXPLORE,
    APPLY,
    CONNECT
}
