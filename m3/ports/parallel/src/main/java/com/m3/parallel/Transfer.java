/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

/**
 * Host-to-device transfer policy of one bound buffer. Device-to-host copies are always explicit
 * ({@link Result#download(IntBuffer...)}).
 */
public enum Transfer {
    /** Copied host-to-device before the first launch only; then resident on the device. */
    ONCE,
    /** Copied host-to-device before every launch (small configuration buffers). */
    EACH_LAUNCH,
    /** Never copied automatically; device storage starts zero-filled and moves only on explicit upload/download. */
    ON_DEMAND
}
