/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Immutable description of one executable or advisory capability.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dynamic/CapabilityProfile.java}
 * blob {@code 5250e9ba7b404e57f28db2ac32771b1e3f68e356} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record CapabilityProfile(
        String bindingName,
        String capability,
        Set<String> tags,
        Map<String, Double> metrics,
        boolean readOnly,
        Map<String, String> attributes) {

    public static final String COST = "cost";
    public static final String LATENCY_MICROS = "latencyMicros";
    public static final String EFFICIENCY = "efficiency";
    public static final String RELIABILITY = "reliability";
    public static final String CPU_UNITS = "cpuUnits";
    public static final String MEMORY_BYTES = "memoryBytes";
    public static final String CONTEXT_BYTES = "contextBytes";
    public static final String MIN_TASK_BYTES = "minTaskBytes";
    public static final String MAX_TASK_BYTES = "maxTaskBytes";

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");

    public CapabilityProfile {
        bindingName = requireId(bindingName, "bindingName");
        capability = requireId(capability, "capability");
        tags = immutableTags(tags);
        metrics = immutableMetrics(metrics);
        attributes = immutableAttributes(attributes);
        double minTaskBytes = metrics.getOrDefault(MIN_TASK_BYTES, 0.0);
        double maxTaskBytes = metrics.getOrDefault(MAX_TASK_BYTES, Double.MAX_VALUE);
        if (minTaskBytes < 0.0 || maxTaskBytes < minTaskBytes) {
            throw new IllegalArgumentException("invalid task-size envelope");
        }
    }

    public OptionalDouble metric(String name) {
        Double value = metrics.get(name);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public double metricOrDefault(String name, double defaultValue) {
        return metrics.getOrDefault(name, defaultValue);
    }

    static String requireId(String value, String field) {
        if (value == null || !ID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is not a valid identifier");
        }
        return value;
    }

    static Set<String> immutableTags(Set<String> values) {
        Objects.requireNonNull(values, "tags");
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) {
            String tag = Objects.requireNonNull(value, "tag")
                    .trim()
                    .toLowerCase(java.util.Locale.ROOT);
            if (!tag.isEmpty()) {
                sorted.add(tag);
            }
        }
        return Collections.unmodifiableSet(sorted);
    }

    private static Map<String, Double> immutableMetrics(Map<String, Double> values) {
        Objects.requireNonNull(values, "metrics");
        TreeMap<String, Double> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            String name = requireId(key, "metric");
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("metric must be finite: " + name);
            }
            sorted.put(name, value);
        });
        return Collections.unmodifiableMap(sorted);
    }

    private static Map<String, String> immutableAttributes(Map<String, String> values) {
        Objects.requireNonNull(values, "attributes");
        TreeMap<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            String name = requireId(key, "attribute");
            String text = Objects.requireNonNull(value, "attribute value");
            if (text.length() > 4096) {
                throw new IllegalArgumentException("attribute too long: " + name);
            }
            sorted.put(name, text);
        });
        return Collections.unmodifiableMap(sorted);
    }
}
