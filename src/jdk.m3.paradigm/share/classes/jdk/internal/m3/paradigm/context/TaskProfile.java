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
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable context value describing what a task needs, not how it will execute.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/dynamic/TaskProfile.java}
 * blob {@code 1e32fbd1c0d419cc7030fbc03b2ed7c17d31245e} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record TaskProfile(
        String text,
        String requiredCapability,
        Set<String> requiredTags,
        Set<String> preferredTags,
        long taskBytes,
        Map<String, MetricPreference> metricPreferences,
        boolean requireReadOnly,
        int maxResults) {

    public enum Goal {
        MINIMIZE,
        MAXIMIZE,
        TARGET
    }

    public record MetricPreference(
            double minInclusive,
            double maxInclusive,
            double target,
            double weight,
            boolean hard,
            Goal goal) {

        public MetricPreference {
            if (Double.isNaN(minInclusive)
                    || Double.isNaN(maxInclusive)
                    || minInclusive > maxInclusive) {
                throw new IllegalArgumentException("invalid metric range");
            }
            if (!Double.isFinite(weight) || weight <= 0.0) {
                throw new IllegalArgumentException(
                        "metric weight must be positive and finite");
            }
            goal = Objects.requireNonNull(goal, "goal");
            if (goal == Goal.TARGET && !Double.isFinite(target)) {
                throw new IllegalArgumentException(
                        "target goal requires a finite target");
            }
        }

        public static MetricPreference minimize(
                double minInclusive,
                double maxInclusive,
                double weight,
                boolean hard) {
            return new MetricPreference(
                    minInclusive,
                    maxInclusive,
                    0.0,
                    weight,
                    hard,
                    Goal.MINIMIZE);
        }

        public static MetricPreference maximize(
                double minInclusive,
                double maxInclusive,
                double weight,
                boolean hard) {
            return new MetricPreference(
                    minInclusive,
                    maxInclusive,
                    0.0,
                    weight,
                    hard,
                    Goal.MAXIMIZE);
        }

        public static MetricPreference target(
                double minInclusive,
                double maxInclusive,
                double target,
                double weight,
                boolean hard) {
            return new MetricPreference(
                    minInclusive,
                    maxInclusive,
                    target,
                    weight,
                    hard,
                    Goal.TARGET);
        }
    }

    public TaskProfile {
        text = Objects.requireNonNullElse(text, "");
        if (text.length() > 65_536) {
            throw new IllegalArgumentException("task text too long");
        }
        requiredCapability = requiredCapability == null || requiredCapability.isBlank()
                ? null
                : CapabilityProfile.requireId(
                        requiredCapability,
                        "requiredCapability");
        requiredTags = CapabilityProfile.immutableTags(requiredTags);
        preferredTags = CapabilityProfile.immutableTags(preferredTags);
        if (taskBytes < 0L) {
            throw new IllegalArgumentException("taskBytes must be non-negative");
        }
        if (maxResults < 1 || maxResults > 1024) {
            throw new IllegalArgumentException(
                    "maxResults must be between 1 and 1024");
        }
        TreeMap<String, MetricPreference> preferences = new TreeMap<>();
        Objects.requireNonNull(
                        metricPreferences,
                        "metricPreferences")
                .forEach((name, preference) -> preferences.put(
                        CapabilityProfile.requireId(
                                name,
                                "metric preference"),
                        Objects.requireNonNull(
                                preference,
                                "metric preference")));
        metricPreferences = Collections.unmodifiableMap(preferences);
    }
}
