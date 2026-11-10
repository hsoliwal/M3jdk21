/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
/** Payload for M3ObjectLoaderLifetimeTest: its bytes, not its Class object, are defined by each child loader.
 * It stays in the unnamed package because a child loader may not define java.* classes. */
public final class M3ObjectLoaderLifetimePayload {
    private volatile int value = 41; // M3ObjectLoaderLifetimeTest.INITIAL_VALUE

    public M3ObjectLoaderLifetimePayload() { }

    public static java.lang.invoke.MethodHandles.Lookup authority() {
        return java.lang.invoke.MethodHandles.lookup();
    }
}
