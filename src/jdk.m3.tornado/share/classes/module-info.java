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
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

/**
 * Optional TornadoVM execution for the M3JDK21 parallel and internal bulk APIs.
 * Historical RV32IM types and module dependencies retain their existing names.
 */
module jdk.m3.tornado {
    requires transitive tornado.api;
    requires transitive m3.parallel;
    requires static tornado.runtime;

    exports jdk.m3.tornado;
    exports jdk.internal.m3.cpugpu;
    exports jdk.m3.parallel.bulk;

    opens jdk.internal.m3.cpugpu to tornado.runtime;
    opens jdk.internal.m3.tornado.parallel to tornado.runtime;

    provides jdk.m3.parallel.spi.BackendProvider with
            jdk.internal.m3.tornado.parallel.TornadoBackendProvider;
}
