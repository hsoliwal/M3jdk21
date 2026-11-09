/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Immutable executable image plan. */
public record M3ImagePlan(
        Path directory,
        String planSha256,
        String mainClass,
        M3Mode mode,
        String javaRuntimeIdentity,
        List<M3ClassPathScanner.Input> inputs,
        Map<String, M3ClassInfo> classes,
        Map<String, M3ClassRole> roles,
        Set<M3ReachabilityAnalyzer.Feature> features,
        Set<String> roots,
        Set<String> modules,
        M3Profile profile,
        boolean moduleFallback) {

    /** Normalizes immutable state. */
    public M3ImagePlan {
        directory = directory.toAbsolutePath().normalize();
        planSha256 = M3Hashing.requireSha256(planSha256, "planSha256");
        if (mainClass == null || mainClass.isBlank() || mode == null || profile == null) {
            throw new IllegalArgumentException("Invalid plan metadata");
        }
        javaRuntimeIdentity = M3Hashing.requireSha256(javaRuntimeIdentity, "javaRuntimeIdentity");
        inputs = List.copyOf(inputs);
        classes = Collections.unmodifiableMap(new TreeMap<>(classes));
        roles = Collections.unmodifiableMap(new TreeMap<>(roles));
        if (!classes.keySet().equals(roles.keySet())) {
            throw new IllegalArgumentException("Plan roles must cover exactly the class inventory");
        }
        TreeSet<M3ReachabilityAnalyzer.Feature> orderedFeatures = new TreeSet<>(
                java.util.Comparator.comparing(M3ReachabilityAnalyzer.Feature::ownerClass)
                        .thenComparing(feature -> feature.kind().name())
                        .thenComparing(M3ReachabilityAnalyzer.Feature::detail));
        orderedFeatures.addAll(features);
        features = Collections.unmodifiableSet(orderedFeatures);
        roots = Collections.unmodifiableSet(new TreeSet<>(roots));
        modules = Collections.unmodifiableSet(new TreeSet<>(modules));
        if (!classes.containsKey(mainClass) || !roots.contains(mainClass) || modules.isEmpty()) {
            throw new IllegalArgumentException("Plan must contain its main class, root, and at least one module");
        }
        if (roles.get(mainClass) == M3ClassRole.UNREACHABLE) {
            throw new IllegalArgumentException("Main class cannot be unreachable");
        }
        for (String root : roots) {
            if (classes.containsKey(root) && roles.get(root) == M3ClassRole.UNREACHABLE) {
                throw new IllegalArgumentException("Root class cannot be unreachable: " + root);
            }
        }
        for (M3ReachabilityAnalyzer.Feature feature : features) {
            if (!classes.containsKey(feature.ownerClass())
                    || roles.get(feature.ownerClass()) == M3ClassRole.UNREACHABLE) {
                throw new IllegalArgumentException("Dynamic feature has invalid owner: " + feature);
            }
            if (mode == M3Mode.CLOSED_WORLD && !feature.resolved()) {
                throw new IllegalArgumentException("Closed-world plan contains unresolved feature: " + feature);
            }
        }
    }

    /** Returns local input bindings by id. */
    public Map<String, Path> bindings() {
        Map<String, Path> bindings = new TreeMap<>();
        inputs.forEach(input -> bindings.put(input.id(), input.path()));
        return Collections.unmodifiableMap(bindings);
    }
}
