/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * M3JDK21-owned RV32IM provider and retained historical compatibility types.
 * The existing internal bulk contract is accessed through a qualified java.base export.
 */
module jdk.m3.tornado {
    requires transitive tornado.api;

    exports jdk.m3.tornado;
    exports jdk.internal.m3.cpugpu;

    opens jdk.internal.m3.cpugpu to tornado.runtime;
}
