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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record BulkTask(String operationId, long workItems, long localWork,
        List<String> inputs, List<String> outputs) {
    public BulkTask {
        operationId = identifier(operationId, "operationId");
        if (workItems < 1 || workItems > Integer.MAX_VALUE) throw new IllegalArgumentException("workItems");
        if (localWork < 0 || localWork > workItems) throw new IllegalArgumentException("localWork");
        inputs = names(inputs, "inputs");
        outputs = names(outputs, "outputs");
        if (inputs.isEmpty()) throw new IllegalArgumentException("inputs");
        if (outputs.isEmpty()) throw new IllegalArgumentException("outputs");
    }
    static String identifier(String value, String field) {
        String checked = Objects.requireNonNull(value, field);
        if (checked.isEmpty() || checked.length() > 128) throw new IllegalArgumentException(field);
        for (int i = 0; i < checked.length(); i++) {
            char c = checked.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9' || c == '.' || c == '_' || c == '-';
            if (!ok) throw new IllegalArgumentException(field);
        }
        return checked;
    }
    private static List<String> names(List<String> values, String field) {
        Objects.requireNonNull(values, field);
        HashSet<String> unique = new HashSet<>();
        for (String value : values) {
            String name = identifier(value, field);
            if (!unique.add(name)) throw new IllegalArgumentException(field);
        }
        return List.copyOf(values);
    }
}
