/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 */

package jdk.internal.vm.parallel;

import java.util.Map;
import java.util.Objects;

public record BulkExecution(Map<String, Object> outputs, String provider, String receiptRoot) {
    public BulkExecution {
        outputs = Map.copyOf(Objects.requireNonNull(outputs, "outputs"));
        provider = BulkTask.identifier(provider, "provider");
        String root = Objects.requireNonNull(receiptRoot, "receiptRoot");
        if (root.length() != 64) throw new IllegalArgumentException("receiptRoot");
        for (int i = 0; i < root.length(); i++) {
            char c = root.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                throw new IllegalArgumentException("receiptRoot");
            }
        }
        receiptRoot = root;
    }
}
