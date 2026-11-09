/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Computes monotone application-class reachability and feature disposition. */
public final class M3ReachabilityAnalyzer {
    /** One feature constraint in the plan. */
    public record Feature(String ownerClass, M3FeatureKind kind, String detail, boolean resolved) {
        /** Validates one feature row. */
        public Feature {
            if (ownerClass == null || ownerClass.isBlank() || kind == null || detail == null) {
                throw new IllegalArgumentException("Invalid feature row");
            }
        }
    }

    /** Reachability result. */
    public record Result(Set<String> roots, Set<String> reachable, Map<String, M3ClassRole> roles,
                         Set<Feature> features, Set<String> missingRoots) {
        /** Normalizes ordered immutable collections. */
        public Result {
            roots = Collections.unmodifiableSet(new TreeSet<>(roots));
            reachable = Collections.unmodifiableSet(new TreeSet<>(reachable));
            roles = Collections.unmodifiableMap(new TreeMap<>(roles));
            TreeSet<Feature> orderedFeatures = new TreeSet<>(
                    java.util.Comparator.comparing(Feature::ownerClass)
                            .thenComparing(feature -> feature.kind().name())
                            .thenComparing(Feature::detail));
            orderedFeatures.addAll(features);
            features = Collections.unmodifiableSet(orderedFeatures);
            missingRoots = Collections.unmodifiableSet(new TreeSet<>(missingRoots));
        }
    }

    private M3ReachabilityAnalyzer() {
    }

    /** Computes the class-granular closure with the configured Java or explicit JNI kernel. */
    public static Result analyze(Map<String, M3ClassInfo> classes, String mainClass,
                                 Set<String> dynamicRoots, Set<M3FeatureKind> allowedFeatures,
                                 M3Profile profile, M3Mode mode) {
        return analyze(classes, mainClass, dynamicRoots, allowedFeatures, profile, mode,
                M3ReachabilityKernels.selected());
    }

    static Result analyze(Map<String, M3ClassInfo> classes, String mainClass,
                          Set<String> dynamicRoots, Set<M3FeatureKind> allowedFeatures,
                          M3Profile profile, M3Mode mode, M3ReachabilityKernel kernel) {
        if (classes == null || mainClass == null || dynamicRoots == null || allowedFeatures == null
                || profile == null || mode == null || kernel == null) {
            throw new IllegalArgumentException("Reachability inputs must not be null");
        }
        Set<String> roots = new TreeSet<>();
        roots.add(mainClass);
        roots.addAll(dynamicRoots);
        for (String observed : profile.loadedClassRanks().keySet()) {
            if (classes.containsKey(observed)) {
                roots.add(observed);
            }
        }

        Set<String> presentRoots = new TreeSet<>();
        Set<String> missingRoots = new TreeSet<>();
        for (String root : roots) {
            if (classes.containsKey(root)) {
                presentRoots.add(root);
            } else {
                missingRoots.add(root);
            }
        }
        M3ReachabilityGraph graph = M3ReachabilityGraph.from(classes, presentRoots);
        Set<String> reachable = graph.namesFor(kernel.close(graph));

        Map<String, M3ClassRole> roles = new TreeMap<>();
        for (String className : classes.keySet()) {
            if (reachable.contains(className)) {
                roles.put(className, M3ClassRole.AOT_CANDIDATE);
            } else if (mode == M3Mode.CLOSED_WORLD) {
                roles.put(className, M3ClassRole.UNREACHABLE);
            } else {
                roles.put(className, M3ClassRole.BYTECODE_FALLBACK);
            }
        }

        Set<Feature> features = new TreeSet<>(java.util.Comparator.comparing(Feature::ownerClass)
                .thenComparing(feature -> feature.kind().name()).thenComparing(Feature::detail));
        for (String className : reachable) {
            M3ClassInfo info = classes.get(className);
            for (M3FeatureKind kind : info.features()) {
                boolean resolved = mode != M3Mode.CLOSED_WORLD || allowedFeatures.contains(kind);
                String detail = kind == M3FeatureKind.JNI_NATIVE_METHOD
                        ? String.join(",", info.nativeMethods()) : "constant-pool-detection";
                features.add(new Feature(className, kind, detail, resolved));
            }
        }
        return new Result(roots, reachable, roles, features, missingRoots);
    }
}
