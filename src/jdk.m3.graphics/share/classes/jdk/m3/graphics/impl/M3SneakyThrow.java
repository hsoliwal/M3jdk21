// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
package jdk.m3.graphics.impl;

/** Rethrows any Throwable without declaring it (the semantics of Lombok's sneakyThrow, which delombok leaves behind). */
public final class M3SneakyThrow {
    private M3SneakyThrow() {
    }

    public static RuntimeException sneakyThrow(Throwable t) {
        throw M3SneakyThrow.<RuntimeException>rethrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T rethrow(Throwable t) throws T {
        throw (T) t;
    }
}
