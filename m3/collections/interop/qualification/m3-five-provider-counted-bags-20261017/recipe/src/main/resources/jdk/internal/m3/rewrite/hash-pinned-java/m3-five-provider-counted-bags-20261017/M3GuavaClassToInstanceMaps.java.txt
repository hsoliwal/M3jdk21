/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections.interop;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.m3.collections.M3LinkedHashMap;

/** Guava's checked ClassToInstanceMap API over original M3 indexed map storage. */
public final class M3GuavaClassToInstanceMaps {
    private M3GuavaClassToInstanceMaps() { }
    /** Independent factory; Guava's own checked, live Entry implementation is retained. */
    public static <B> ClassToInstanceMap<B> create() {
        return MutableClassToInstanceMap.create(new M3LinkedHashMap<Class<? extends B>, B>());
    }
}
