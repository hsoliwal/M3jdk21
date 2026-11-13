/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Plan disposition of an application class. */
public enum M3ClassRole {
    AOT_CANDIDATE,
    BYTECODE_FALLBACK,
    UNREACHABLE
}
