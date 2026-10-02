/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 * Source lineage: com.synexia.common.collections.LongLongConsumer
 * at com.synexia 3db24805d640c72ab1bd637d83561696d99561a0.
 */
package com.m3.collections;

/** Primitive two-long consumer used by M3 maps without Map.Entry materialization. */
@FunctionalInterface
public interface M3LongLongConsumer {
    void accept(long first, long second);
}
