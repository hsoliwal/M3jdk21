/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package java.lang;

/**
 * Receives coalesced object invalidations on framework background dispatch.
 * A callback failure is contained so remaining subscriptions can run.
 * Callbacks should return promptly because dispatch is shared.
 *
 * @since 21
 */
@FunctionalInterface
public interface ObjectChangeListener {
    /**
     * Receives an immutable invalidation envelope. Its current value is read
     * lazily and may already reflect later mutations.
     *
     * @param event the admitted invalidation
     */
    void onChange(ObjectEvent event);
}
