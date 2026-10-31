/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.text;

import java.util.Objects;
import java.util.UUID;

/** Stable ownership identity, never a reusable cache-slot address. */
public record StorageIdentity(UUID owner, long generation, long record) {
    public StorageIdentity {
        Objects.requireNonNull(owner);
        if (generation <= 0 || record <= 0) throw new IllegalArgumentException("nonpositive identity");
    }
}
